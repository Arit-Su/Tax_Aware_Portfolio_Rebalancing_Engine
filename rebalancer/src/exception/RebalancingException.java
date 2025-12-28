package exception;

//package com.rebalancing.engine.exception;

/**
 * Custom exception for errors that occur during the portfolio rebalancing process.
 * Using a specific exception type is better than a generic one for targeted error handling.
 */
public class RebalancingException extends RuntimeException {
    public RebalancingException(String message) {
        super(message);
    }

    public RebalancingException(String message, Throwable cause) {
        super(message, cause);
    }
}