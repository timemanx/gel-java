package com.geldata.driver.clients;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

import com.geldata.driver.async.ChannelCompletableFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

final class SharedNettyEventLoopGroups {
    private static final Object LOCK = new Object();

    private static SharedHandle currentHandle;
    private static int referenceCount;

    static SharedHandle acquire() {
        synchronized (LOCK) {
            if(currentHandle == null) {
                currentHandle = new SharedHandle(
                        new NioEventLoopGroup(),
                        new DefaultEventExecutorGroup(8)
                );
                referenceCount = 0;
            }

            referenceCount++;
            return currentHandle;
        }
    }

    static CompletionStage<Void> release(SharedHandle handle) {
        synchronized (LOCK) {
            if(currentHandle != handle) {
                return CompletableFuture.completedFuture(null);
            }

            referenceCount--;

            if(referenceCount > 0) {
                return CompletableFuture.completedFuture(null);
            }

            currentHandle = null;

            return CompletableFuture.allOf(
                    ChannelCompletableFuture.completeFrom(handle.nettyTcpGroup.shutdownGracefully()).toCompletableFuture(),
                    ChannelCompletableFuture.completeFrom(handle.duplexerGroup.shutdownGracefully()).toCompletableFuture()
            );
        }
    }

    static final class SharedHandle {
        final NioEventLoopGroup nettyTcpGroup;
        final EventExecutorGroup duplexerGroup;

        private SharedHandle(NioEventLoopGroup nettyTcpGroup, EventExecutorGroup duplexerGroup) {
            this.nettyTcpGroup = nettyTcpGroup;
            this.duplexerGroup = duplexerGroup;
        }
    }

    private SharedNettyEventLoopGroups() {
    }
}
