package io.github.datromtool.util;

import com.google.common.collect.ImmutableSet;
import io.github.datromtool.SerializationHelper;
import io.github.datromtool.data.RegionData;
import io.github.datromtool.domain.datafile.Game;
import lombok.NoArgsConstructor;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class TestUtils {

    public static RegionData loadRegionData() throws Exception {
        return SerializationHelper.getInstance()
                .loadRegionData(Paths.get(ClassLoader.getSystemResource("region-data.yaml")
                        .toURI()));
    }

    public static Game createGame(String s) {
        return Game.builder()
                .name(s)
                .description(s)
                .build();
    }

    public static RegionData getRegionByCode(RegionData regionData, String code, String... codes) {
        List<String> codeList = Arrays.asList(codes);
        return RegionData.builder().regions(regionData.getRegions()
                .stream()
                .filter(r -> code.equals(r.getCode()) || codeList.contains(r.getCode()))
                .collect(ImmutableSet.toImmutableSet()))
                .build();
    }

}
