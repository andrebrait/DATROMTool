package io.github.datromtool.exception;

public final class WrappedExecutionException extends RuntimeException {

    public WrappedExecutionException(ExecutionException cause) {
        super(cause);
    }

    @Override
    public ExecutionException getCause() {
        return (ExecutionException) super.getCause();
    }
}
