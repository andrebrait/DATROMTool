package io.github.datromtool.domain.detector.exception;

import io.github.datromtool.domain.detector.Rule;

public final class RuleWrappedException extends RuntimeException {

    protected final Rule rule;

    public RuleWrappedException(Rule rule, Throwable cause) {
        super(cause);
        this.rule = rule;
    }

    @Override
    public String getMessage() {
        return "Rule '" + rule + "' failed";
    }
}
