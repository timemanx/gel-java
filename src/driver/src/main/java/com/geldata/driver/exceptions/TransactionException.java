package com.geldata.driver.exceptions;

/**
 * Represents an exception that was cased within a transaction.
 * @see com.geldata.driver.Transaction
 */
public final class TransactionException extends GelException {
    /**
     * Constructs a new {@linkplain TransactionException}
     * @param message The error message describing why this exception was thrown.
     */
    public TransactionException(String message) {
        super(message, false, false);
    }

    /**
     * Constructs a new {@linkplain TransactionException}.
     * @param message The error message describing why this exception was thrown.
     * @param inner The inner cause of this exception.
     */
    public TransactionException(String message, Throwable inner) {
        super(message, inner);
    }
}
