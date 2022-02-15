package io.github.datromtool.sorting;

import com.google.common.collect.ImmutableList;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class GameNameComparator implements Comparator<String> {

    private static final List<Character> specialChars = ImmutableList.of('[', '{', '(', '#');

    public static final GameNameComparator INSTANCE = new GameNameComparator(Locale.getDefault());

    private final Collator collator;

    public GameNameComparator(Locale locale) {
        this.collator = Collator.getInstance(locale);
        this.collator.setStrength(Collator.SECONDARY);
    }

    @Override
    public int compare(String o1, String o2) {
        if (o1.length() > 0 && o2.length() > 0) {
            char c1 = o1.charAt(0);
            char c2 = o2.charAt(0);
            if (specialChars.contains(c1)) {
                if (specialChars.contains(c2)) {
                    return Character.compare(c1, c2);
                } else {
                    return -1;
                }
            } else if (specialChars.contains(c2)) {
                return 1;
            }
        }
        return collator.compare(o1, o2);
    }
}
