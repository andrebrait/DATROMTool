package io.github.datromtool.domain.detector.exception;

import io.github.datromtool.domain.detector.Test;

public final class TestWrappedException extends RuntimeException {

    protected final Test test;

    public TestWrappedException(Test test, Throwable cause) {
        super(cause);
        this.test = test;
    }

    @Override
    public String getMessage() {
        return "Test '" + test + "' failed";
    }
}
