package com.geldata.driver.clients;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SharedNettyEventLoopGroupsTests {
    @Test
    public void lastReleaseShutsDownSharedGroups() {
        var first = SharedNettyEventLoopGroups.acquire();
        var second = SharedNettyEventLoopGroups.acquire();
        SharedNettyEventLoopGroups.SharedHandle third = null;

        try {
            assertSame(first, second);
            assertFalse(first.nettyTcpGroup.isShuttingDown());
            assertFalse(first.duplexerGroup.isShuttingDown());

            SharedNettyEventLoopGroups.release(first).toCompletableFuture().join();

            assertFalse(second.nettyTcpGroup.isShuttingDown());
            assertFalse(second.duplexerGroup.isShuttingDown());

            SharedNettyEventLoopGroups.release(second).toCompletableFuture().join();

            assertTrue(first.nettyTcpGroup.isTerminated());
            assertTrue(first.duplexerGroup.isTerminated());

            third = SharedNettyEventLoopGroups.acquire();
            assertNotSame(first, third);
            assertFalse(third.nettyTcpGroup.isShuttingDown());
            assertFalse(third.duplexerGroup.isShuttingDown());
        } finally {
            if(third != null) {
                SharedNettyEventLoopGroups.release(third).toCompletableFuture().join();
            }

            SharedNettyEventLoopGroups.release(second).toCompletableFuture().join();
            SharedNettyEventLoopGroups.release(first).toCompletableFuture().join();
        }
    }
}
