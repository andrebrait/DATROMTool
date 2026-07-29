package io.github.datromtool.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Evaluates regular expressions that came from a file the user merely pointed the tool at — a
 * Retool clone list, a patterns file, an {@code --exclude-regex}, a profile. {@code
 * java.util.regex} has no match timeout and no depth limit, so such a pattern can otherwise
 * monopolize or end the run (CWE-1333).
 *
 * <p>Two distinct failure modes, both handled here, because bounding one does not bound the other:
 *
 * <ul>
 *   <li><b>Backtracking</b> burns time while re-reading the input, so a read budget bounds it.
 *   <li><b>Depth</b> costs no reads at all: the engine recurses once per pattern node, so a long
 *       flat pattern exhausts the stack having read almost nothing — measured at ~6,000 reads of
 *       a 100,000 budget. Only catching the resulting {@link StackOverflowError} bounds that.
 * </ul>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UntrustedPatterns {

    /**
     * How much of the input one pattern may read before it is given up on. Legitimate patterns
     * read a small multiple of the input's length; this leaves three orders of magnitude of room.
     */
    public static final int MAX_CHARACTER_READS = 100_000;

    private static final int PATTERN_EXCERPT_LENGTH = 120;

    /** A pattern that could not be evaluated within its budget, or at all. */
    public static final class RejectedPatternException extends RuntimeException {

        RejectedPatternException(String message) {
            super(message);
        }
    }

    /** Compiles an untrusted pattern, rejecting one too deep for the engine to build. */
    @Nonnull
    public static Pattern compile(@Nonnull String regex) {
        try {
            return Pattern.compile(regex);
        } catch (StackOverflowError e) {
            throw rejected(regex, "is too deeply nested for the regular expression engine");
        }
    }

    /** As {@link Matcher#matches()}, bounded. */
    public static boolean matches(@Nonnull Pattern pattern, @Nonnull String input) {
        return evaluate(pattern, input, true);
    }

    /** As {@link Matcher#find()}, bounded. */
    public static boolean find(@Nonnull Pattern pattern, @Nonnull String input) {
        return evaluate(pattern, input, false);
    }

    private static boolean evaluate(Pattern pattern, String input, boolean wholeInput) {
        Matcher matcher = pattern.matcher(new BudgetedCharSequence(input, MAX_CHARACTER_READS));
        try {
            return wholeInput ? matcher.matches() : matcher.find();
        } catch (BudgetExceeded e) {
            throw rejected(
                    pattern.pattern(),
                    format(
                            "read more than %d characters of '%s' without deciding: it backtracks "
                                    + "catastrophically",
                            MAX_CHARACTER_READS,
                            input));
        } catch (StackOverflowError e) {
            // The stack unwinds fully before this runs: the engine holds no state of its own
            // across a match, so giving up on this one pattern leaves the run usable.
            throw rejected(pattern.pattern(), "exhausted the stack of the regular expression engine");
        }
    }

    private static RejectedPatternException rejected(String regex, String reason) {
        String excerpt = regex.length() > PATTERN_EXCERPT_LENGTH
                ? regex.substring(0, PATTERN_EXCERPT_LENGTH) + "… (" + regex.length() + " characters)"
                : regex;
        return new RejectedPatternException(
                format("Pattern '%s' %s, and was given up on", excerpt, reason));
    }

    /** Signals the read budget's exhaustion; never leaves this class. */
    private static final class BudgetExceeded extends RuntimeException {

        BudgetExceeded() {
            super(null, null, false, false);
        }
    }

    private static final class BudgetedCharSequence implements CharSequence {

        private final CharSequence delegate;
        private int readsLeft;

        BudgetedCharSequence(CharSequence delegate, int readsLeft) {
            this.delegate = delegate;
            this.readsLeft = readsLeft;
        }

        @Override
        public char charAt(int index) {
            if (--readsLeft < 0) {
                throw new BudgetExceeded();
            }
            return delegate.charAt(index);
        }

        @Override
        public int length() {
            return delegate.length();
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return delegate.subSequence(start, end);
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
