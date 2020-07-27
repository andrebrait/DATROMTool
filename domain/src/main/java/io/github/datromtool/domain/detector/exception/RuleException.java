package io.github.datromtool.domain.detector.exception;

import io.github.datromtool.domain.detector.Rule;

public final class RuleException extends RuntimeException {

    private final Rule rule;

    public RuleException(Rule rule, String message) {
        super(message);
        this.rule = rule;
    }

    @Override
    public String getMessage() {
        return "Could not apply rule '" + rule + "'. Cause: " + super.getMessage();
    }
}
