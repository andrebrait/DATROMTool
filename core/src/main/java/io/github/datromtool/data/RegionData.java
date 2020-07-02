package io.github.datromtool.data;

import lombok.Value;

import java.util.List;
import java.util.regex.Pattern;

@Value
public class RegionData {

    String code;
    Pattern pattern;
    List<String> languages;

}
