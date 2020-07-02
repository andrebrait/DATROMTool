package io.github.datromtool.data;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class Filter {

    List<String> regions;
    List<String> languages;


}
