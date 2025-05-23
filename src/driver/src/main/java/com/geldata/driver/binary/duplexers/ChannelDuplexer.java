package com.geldata.driver.binary.duplexers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.geldata.driver.ErrorCode;
import com.geldata.driver.async.ChannelCompletableFuture;
import com.geldata.driver.binary.protocol.ProtocolProvider;
import com.geldata.driver.binary.protocol.Receivable;
import com.geldata.driver.binary.protocol.Sendable;
import com.geldata.driver.binary.protocol.common.ProtocolError;
import com.geldata.driver.clients.GelBinaryClient;
import com.geldata.driver.exceptions.ConnectionFailedException;
import com.geldata.driver.exceptions.ConnectionFailedTemporarilyException;
import com.geldata.driver.exceptions.GelException;

import javax.naming.OperationNotSupportedException;

import static com.geldata.driver.util.ComposableUtil.composeWith;
import static com.geldata.driver.util.ComposableUtil.exceptionallyCompose;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ChannelDuplexer extends Duplexer {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDuplexer.class);

    public final ChannelHandler channelHandler = new ChannelHandler();

    private final @NotNull Queue<Receivable> messageQueue;
    private final @NotNull Queue<CompletableFuture<Receivable>> readPromises;

    private final ReentrantLock messageEnqueueLock = new ReentrantLock();

    private final GelBinaryClient client;

    private boolean isConnected;

    private @Nullable Channel channel;


    @io.netty.channel.ChannelHandler.Sharable
    public class ChannelHandler extends ChannelInboundHandlerAdapter {
        private CompletableFuture<Void> channelActivePromise;

        public ChannelHandler() {
            channelActivePromise = new CompletableFuture<>();
        }

        public void reset() {
            logger.debug("Resetting channel handler");
            this.channelActivePromise = new CompletableFuture<Void>();
        }

        @Override
        public void channelActive(@NotNull ChannelHandlerContext ctx) {
            logger.debug("Channel active");
            isConnected = true;
            channelActivePromise.complete(null);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, @NotNull Object evt) {
            logger.debug("event fired {}", evt);
            if (evt.equals("TIMEOUT")) {
                var exc = new TimeoutException("A message read process passed the configured message timeout");
                for(var promise : readPromises) {
                    promise.completeExceptionally(exc);
                }
            } else if (evt.equals("DISCONNECT")) {
                disconnect();
                var exc = new ConnectionFailedException("Client requested a disconnect");
                for(var promise : readPromises) {
                    promise.completeExceptionally(exc);
                }
            }
        }

        @Override
        public void channelInactive(@NotNull ChannelHandlerContext ctx) {
            isConnected = false;
            logger.debug("Channel inactive");
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext ctx, @NotNull Object msg) {
            var protocolMessage = (Receivable)msg;

            if(
                    protocolMessage instanceof ProtocolError && (
                            ((ProtocolError)protocolMessage).getErrorCode() == ErrorCode.IDLE_SESSION_TIMEOUT_ERROR ||
                            ((ProtocolError)protocolMessage).getErrorCode() == ErrorCode.IDLE_TRANSACTION_TIMEOUT_ERROR
                    )
            ) {
                logger.debug("Got idle disconnect message, marking as closed");
                isConnected = false;
                return;
            }

            int completeCount = 0;

            try {
                logger.debug("Read fired, entering message lock, message type {}", protocolMessage.getMessageType());
                if(!messageEnqueueLock.tryLock(client.getConfig().getMessageTimeoutValue(), client.getConfig().getMessageTimeoutUnit())) {
                    ctx.fireUserEventTriggered("TIMEOUT");
                    return;
                }
            } catch (InterruptedException e) {
                ctx.fireExceptionCaught(e);
                return;
            }

            try {
                logger.debug("Dependant promises empty?: {}", readPromises.isEmpty());

                if(readPromises.isEmpty()) {
                    logger.debug("Enqueuing message into message queue");
                    messageQueue.add(protocolMessage);
                }
                else {
                    logger.debug("Completing {} message promise(s)", readPromises.size());

                    // we don't want to iterate and complete within the lock, since the complete method *can* enqueue
                    // more promises.
                    completeCount = readPromises.size();
                }
            } finally {
                messageEnqueueLock.unlock();
            }

            for(int i = 0; i != completeCount; i++) {
                var promise = readPromises.poll();

                if(promise == null) {
                    break;
                }

                logger.debug(
                        "Completing promise {} with message type {}; already complete?: {}",
                        promise.hashCode(), protocolMessage.getMessageType(), promise.isDone()
                );

                promise.complete(protocolMessage);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Channel failed", cause);
        }

        public synchronized CompletableFuture<Void> whenReady() {
            if(this.channelActivePromise.isDone()) {
                return CompletableFuture.completedFuture(null);
            } else {
                return this.channelActivePromise;
            }
        }
    }

    public ChannelDuplexer(GelBinaryClient client) {
        this.client = client;
        this.messageQueue = new ArrayDeque<>();
        this.readPromises = new ArrayDeque<>();
    }

    @Override
    public ProtocolProvider getProtocolProvider() {
        return client.getProtocolProvider();
    }

    @Override
    public @NotNull CompletionStage<Receivable> readNext() {
        logger.debug("Entering message queue lock");

        try {
            if(!messageEnqueueLock.tryLock(client.getConfig().getMessageTimeoutValue(), client.getConfig().getMessageTimeoutUnit())) {
                return CompletableFuture.failedFuture(new TimeoutException("A message processor passed the configured message timeout"));
            }
        } catch (InterruptedException e) {
            return CompletableFuture.failedFuture(e);
        }

        try {
            logger.debug("Queue empty?: {}", this.messageQueue.isEmpty());

            if(this.messageQueue.isEmpty()) {
                logger.debug("Creating message wait promise");

                var promise = new CompletableFuture<Receivable>()
                        .orTimeout(
                                client.getConfig().getMessageTimeoutValue(),
                                client.getConfig().getMessageTimeoutUnit()
                        );

                final var readPromiseId = promise.hashCode();

                promise.whenComplete((v,e) -> logger.debug("Read promise completed, ID: {}, is success?: {}", promise.hashCode(), e == null && !promise.isCancelled()));

                logger.debug("Enqueueing read promise: ID: {}", readPromiseId);
                readPromises.add(promise);
                return promise;
            } else {
                var message = this.messageQueue.poll();
                logger.debug("Returning polled message {}", message.getMessageType());
                return CompletableFuture.completedFuture(message);
            }
        }
        finally {
            messageEnqueueLock.unlock();
        }
    }

    private CompletionStage<Void> send1(Sendable packet, @Nullable Sendable @Nullable ... packets) {
        logger.debug("Starting to send packets to {}, is connected? {}", channel, isConnected);

        if(channel == null || !isConnected) {
            return CompletableFuture.failedFuture(
                    new ConnectionFailedTemporarilyException("Cannot send message to a closed connection")
            );
        }

        logger.debug("Beginning packet encoding and writing...");
        var result = ChannelCompletableFuture.completeFrom(channel.write(packet));

        if(packets != null) {
            for (var p : packets) {
                result.thenCompose(channel.write(p));
            }
        }

        logger.debug("Flushing data...");
        channel.flush();
        logger.debug("Flush complete, returning write proxy task");
        return result;
    }

    private CompletionStage<Void> send0(AtomicInteger attempts, Sendable packet, @Nullable Sendable... packets) {
        return exceptionallyCompose(this.channelHandler.whenReady().thenCompose(v -> {
            if(!isConnected) {
                logger.debug(
                        "Connection isn't open with a ready signal, reconnecting: {}/{}",
                        attempts.get(), client.getConfig().getMaxConnectionRetries()
                );

                if(attempts.get() >= client.getConfig().getMaxConnectionRetries()) {
                    return CompletableFuture.failedFuture(
                            new ConnectionFailedException("Failed to connect after " + attempts.get() + "attempts")
                    );
                }

                attempts.incrementAndGet();

                return client.reconnect().thenCompose(n -> {
                    logger.debug("Reconnect complete, retrying send");
                    return send0(attempts, packet, packets);
                });
            }

            return send1(packet, packets);
        }), e -> {
            logger.debug("Caught failed send attempt");

            if(e instanceof GelException && ((GelException)e).shouldRetry && !((GelException)e).shouldReconnect) {
                logger.debug(
                        "Retrying send attempt based off of exception {}. attempt: {}/{}",
                        e.getClass().getSimpleName(), attempts.get() + 1, client.getConfig().getMaxConnectionRetries()
                );

                if(attempts.incrementAndGet() > client.getConfig().getMaxConnectionRetries()) {
                    return CompletableFuture.failedFuture(e);
                }

                return send0(attempts, packet, packets);
            }

            logger.debug("Returning exception to callee");

            return CompletableFuture.failedFuture(e);
        });
    }

    @Override
    public CompletionStage<Void> send(Sendable packet, @Nullable Sendable... packets) {
        AtomicInteger attempts = new AtomicInteger(0);
        logger.debug("Initializing send attempts to 0");
        return send0(attempts, packet, packets);
    }

    @Override
    public CompletionStage<Void> duplex(@NotNull DuplexCallback func, @NotNull Sendable packet, @Nullable Sendable... packets) {
        var firstHashcode = packet.hashCode();
        final var duplexId = 31 * firstHashcode + Arrays.hashCode(packets);
        logger.debug("Starting duplex step, ID: {}", duplexId);
        final var duplexPromise = new CompletableFuture<Void>();

        duplexPromise.whenComplete((v,e) -> logger.debug("Duplex step complete, ID: {}, isCancelled?: {}, isExceptional?: {}", duplexId, duplexPromise.isCancelled(), e != null));

        return this.send(packet, packets)
                .thenCompose((v) -> processDuplexStep(func, duplexPromise, duplexId))
                .thenCompose((v) -> duplexPromise);
    }

    private CompletionStage<Void> processDuplexStep(@NotNull DuplexCallback func, @NotNull CompletableFuture<Void> promise, int id) {
        logger.debug("Handling duplex step, ID: {}", id);

        return composeWith(readNext(), (packet) -> {
            logger.debug("Invoking duplex consumer, ID: {}, Message: {}", id, packet.getMessageType());
            try {
                return func.process(new DuplexResult(packet, promise));
            } catch (GelException | OperationNotSupportedException e) {
                return CompletableFuture.failedFuture(e);
            }
        }).thenCompose(v -> {
            logger.debug(
                    "Post-invoke duplex step ID: {}, isDone?: {}, isCancelled?: {}, isExceptional?: {}, ",
                    id, promise.isDone(), promise.isCancelled(), promise.isCompletedExceptionally()
            );

            if(promise.isDone()) {
                if(promise.isCompletedExceptionally() || promise.isCancelled()) {
                    logger.debug("Returning failed-state promise to callee, ID: {}", id);
                    return promise;
                }

                logger.debug("Returning completed state promise, ID: {}", id);
                return CompletableFuture.completedFuture(null);
            }

            logger.debug("Continuing duplex step for ID: {}", id);
            return processDuplexStep(func, promise, id);
        });
    }

    public void init(Channel channel) {
        this.channel = channel;
    }

    @Override
    public void reset() {
        if(this.channel != null) {
            this.channelHandler.reset();
        }
    }

    @Override
    public boolean isConnected() {
        return isConnected;
    }

    @Override
    public CompletionStage<Void> disconnect() {
        if(this.channel == null) {
            return CompletableFuture.completedFuture(null);
        }

        if(this.isConnected) {
            logger.debug("Sending terminate for disconnect");
            return send(getProtocolProvider().terminate())
                    .thenCompose(v -> ChannelCompletableFuture.completeFrom(this.channel.disconnect()));
        }

        logger.debug("Closing channel without terminating");

        return ChannelCompletableFuture.completeFrom(this.channel.disconnect());
    }
}
