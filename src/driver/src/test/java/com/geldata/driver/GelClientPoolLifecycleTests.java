package com.geldata.driver;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.geldata.driver.clients.BaseGelClient;
import com.geldata.driver.datatypes.Json;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GelClientPoolLifecycleTests {
    @Test
    public void closeDisposesCheckedOutClientsAndDoesNotRecacheThem() throws Exception {
        var createdClient = new AtomicReference<FakeGelClient>();
        var pool = new GelClientPool(
                GelConnection.builder().withHost("localhost").build(),
                GelClientConfig.builder()
                        .withPoolSize(1)
                        .withClientAvailability(1)
                        .build(),
                (connection, config, poolHandle) -> {
                    var client = new FakeGelClient(connection, config, poolHandle);
                    createdClient.set(client);
                    return client;
                }
        );

        var result = pool.querySingle(String.class, "select 'value'");
        var client = createdClient.get();

        assertNotNull(client);

        pool.close();

        assertEquals(1, client.disconnectCalls.get());
        assertEquals(1, client.disposeCalls.get());
        assertEquals(0, pool.getClientCount());

        client.completeQuery("value");

        assertEquals("value", result.toCompletableFuture().join());
        assertEquals(1, client.closeCalls.get());
        assertEquals(1, client.disconnectCalls.get());
        assertEquals(1, client.disposeCalls.get());
        assertEquals(0, pool.getClientCount());
    }

    private static final class FakeGelClient extends BaseGelClient {
        private final CompletableFuture<String> querySingleResult;
        private final AtomicInteger closeCalls;
        private final AtomicInteger disconnectCalls;
        private final AtomicInteger disposeCalls;

        private FakeGelClient(
                @NotNull GelConnection connection,
                @NotNull GelClientConfig config,
                @NotNull AutoCloseable poolHandle
        ) {
            super(connection, config, poolHandle);
            this.querySingleResult = new CompletableFuture<>();
            this.closeCalls = new AtomicInteger();
            this.disconnectCalls = new AtomicInteger();
            this.disposeCalls = new AtomicInteger();
        }

        void completeQuery(String value) {
            this.querySingleResult.complete(value);
        }

        @Override
        public Optional<Long> getSuggestedPoolConcurrency() {
            return Optional.empty();
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public CompletionStage<Void> connect() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletionStage<Void> disconnect() {
            this.disconnectCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        protected CompletionStage<Void> onDispose() {
            this.disposeCalls.incrementAndGet();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void close() throws Exception {
            this.closeCalls.incrementAndGet();
            super.close();
        }

        @Override
        public CompletionStage<Void> execute(
                @NotNull String query,
                @Nullable Map<String, Object> args,
                EnumSet<Capabilities> capabilities
        ) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override
        public <T> CompletionStage<List<T>> query(
                @NotNull Class<T> cls,
                @NotNull String query,
                @Nullable Map<String, Object> args,
                @NotNull EnumSet<Capabilities> capabilities
        ) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        @Override
        public <T> CompletionStage<T> querySingle(
                @NotNull Class<T> cls,
                @NotNull String query,
                @Nullable Map<String, Object> args,
                @NotNull EnumSet<Capabilities> capabilities
        ) {
            return this.querySingleResult.thenApply(cls::cast);
        }

        @Override
        public <T> CompletionStage<T> queryRequiredSingle(
                @NotNull Class<T> cls,
                @NotNull String query,
                @Nullable Map<String, Object> args,
                @NotNull EnumSet<Capabilities> capabilities
        ) {
            return this.querySingleResult.thenApply(cls::cast);
        }

        @Override
        public CompletionStage<Json> queryJson(
                @NotNull String query,
                @Nullable Map<String, Object> args,
                @NotNull EnumSet<Capabilities> capabilities
        ) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override
        public CompletionStage<List<Json>> queryJsonElements(
                @NotNull String query,
                @Nullable Map<String, Object> args,
                @NotNull EnumSet<Capabilities> capabilities
        ) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
    }
}
