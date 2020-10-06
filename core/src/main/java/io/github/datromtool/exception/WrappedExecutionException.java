package io.github.datromtool.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class WrappedExecutionException extends RuntimeException {

    private final ExecutionException wrapped;

}
