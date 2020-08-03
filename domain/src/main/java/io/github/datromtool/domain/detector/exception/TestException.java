package io.github.datromtool.domain.detector.exception;

import io.github.datromtool.domain.detector.Test;

public final class TestException extends RuntimeException {

    private final Test test;

    public TestException(Test test, String message) {
        super(message);
        this.test = test;
    }

    @Override
    public String getMessage() {
        return "Test '" + test + "' failed. Reason: " + super.getMessage();
    }
}
