package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record Pair<D, T>(D left, T right) {

    public static <D, T> Pair<D, T> of(D left, T right) {
        return new Pair<>(left, right);
    }
}
