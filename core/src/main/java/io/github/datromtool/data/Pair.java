package io.github.datromtool.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Value;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Value
@AllArgsConstructor(staticName = "of")
@JsonInclude(NON_NULL)
public class Pair<D, T> {

    D left;
    T right;
}
