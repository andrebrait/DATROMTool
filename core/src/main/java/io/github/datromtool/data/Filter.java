package io.github.datromtool.data;

import com.google.common.collect.ImmutableList;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Filter {

    ImmutableList<String> regions;
    ImmutableList<String> languages;


}
