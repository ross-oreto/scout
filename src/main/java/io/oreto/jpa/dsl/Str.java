package io.oreto.jpa.dsl;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for strings
 */
class Str implements CharSequence {
    static final String EMPTY = "";
    protected static final List<CharSequence> emptyList = new ArrayList<>();

    /**
     * Static variables for characters
     */
    static class Chars {
        static final char NEGATIVE = '-';
        static final char POSITIVE = '+';
        static final char DECIMAL = '.';
        static final char SPACE = ' ';
        static final char ZERO = '0';
    }

    /**
     * Determine if the string is blank
     * @param s The string to test
     * @return True if the string is blank (something other than whitespace)
     */
    static boolean isBlank(CharSequence s) {
        return isEmpty(s) || s.chars().allMatch(Character::isWhitespace);
    }

    /**
     * Determine if a given string is numeric.
     * This is better than relying on something like Integer.parseInt
     * to throw an Exception which has to be part of the normal method behavior.
     * Also better than a regular expression which is more difficult to maintain.
     * @param s A string.
     * @param type the type of number, natural, whole, integer, rational.
     * @return Return true if the value is numeric, false otherwise.
     */
    static boolean isNumber(CharSequence s, Num.Type type) {
        // do some initial sanity checking to catch easy issues quickly and with very little processing
        // also this makes the state tracking easier below
        if (s == null)
            return false;

        int length = s.length();
        if (isBlank(s)
                || (length == 1 && !Character.isDigit(s.charAt(0)))
                ||  s.charAt(length - 1) == Chars.DECIMAL)
            return false;

        boolean dotted = false;
        int[] arr = s.chars().toArray();

        int len = arr.length;
        for (int i = 0; i < len; i++) {
            char c = (char) arr[i];

            switch (c) {
                // +/- can only be in the start position
                case Chars.NEGATIVE:
                    if (i > 0) return false;
                    if (type == Num.Type.natural || type == Num.Type.whole) return false;
                    break;
                case Chars.POSITIVE:
                    if (i > 0) return false;
                    break;
                case Chars.DECIMAL:
                    // only one decimal place allowed
                    if (dotted || type != Num.Type.rational) return false;
                    dotted = true;
                    break;
                default:
                    // this better be a digit
                    if (!Character.isDigit(c))
                        return false;
                    break;
            }
        }
        // make sure natural number type isn't assigned a 0
        return type != Num.Type.natural || !s.chars().allMatch(it -> (char) it == Chars.ZERO);
    }

    /**
     * Determine if the string is a number
     * @param s The string to test
     * @return True if the string is a valid number, false otherwise
     */
    static boolean isNumber(CharSequence s) {
        return isNumber(s, Num.Type.rational);
    }

    /**
     * Determine if the string is an integer
     * @param s The string to test
     * @return True if the string is a valid integer, false otherwise
     */
    static boolean isInteger(CharSequence s) {
        return isNumber(s, Num.Type.integer);
    }

    /**
     * Determine if the string is a byte
     * @param s The string to test
     * @return True if the string is a valid byte, false otherwise
     */
    static boolean isByte(CharSequence s) {
        if (isInteger(s)) {
            int i = Integer.parseInt(s.toString());
            return i >= Byte.MIN_VALUE && i <= Byte.MAX_VALUE;
        } else {
            return false;
        }
    }

    /**
     * Convert string to an optional integer
     * @param s The string to convert
     * @return Optional integer if the string is a valid integer, Optional.empty otherwise
     */
    static Optional<Integer> toInteger(CharSequence s) {
        try {
            return isInteger(s) ? Optional.of(Integer.parseInt(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }


    /**
     * Convert string to an optional double
     * @param s The string to convert
     * @return Optional double if the string is a valid double, Optional.empty otherwise
     */
    static Optional<Double> toDouble(CharSequence s) {
        try {
            return isNumber(s) ? Optional.of(Double.parseDouble(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    /**
     * Convert string to an optional long
     * @param s The string to convert
     * @return Optional long if the string is a valid long, Optional.empty otherwise
     */
    static Optional<Long> toLong(CharSequence s) {
        try {
            return isNumber(s) ? Optional.of(Long.parseLong(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    /**
     * Convert string to an optional float
     * @param s The string to convert
     * @return Optional float if the string is a valid float, Optional.empty otherwise
     */
    static Optional<Float> toFloat(CharSequence s) {
        try {
            return isNumber(s) ? Optional.of(Float.parseFloat(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    /**
     * Convert string to an optional short
     * @param s The string to convert
     * @return Optional short if the string is a valid short, Optional.empty otherwise
     */
    static Optional<Short> toShort(CharSequence s) {
        try {
            return isInteger(s) ? Optional.of(Short.parseShort(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    /**
     * Convert string to an optional byte
     * @param s The string to convert
     * @return Optional byte if the string is a valid byte, Optional.empty otherwise
     */
    static Optional<Byte> toByte(CharSequence s) {
        try {
            return isByte(s) ? Optional.of(Byte.parseByte(s.toString())) : Optional.empty();
        } catch (NumberFormatException ignored) { }
        return Optional.empty();
    }

    /**
     * Create a new empty Str object
     * @return The new Str object
     */
    static Str empty() {
        return new Str(EMPTY);
    }

    /**
     * Determine if the string is null or empty
     * @param s The string to test
     * @return True if the string is null or empty, false otherwise
     */
    static boolean isEmpty(final CharSequence s) {
        return s == null || s.length() == 0;
    }

    /**
     * Determine if the string is not empty. Negation of <code>Str.isEmpty(s)</code>
     * @param s The string to test
     * @return True if the string is not empty, false otherwise
     */
    static boolean isNotEmpty(final CharSequence s) {
        return !isEmpty(s);
    }

    /**
     * Determine if this Str object starts with the specified string
     * @param s The string to search for
     * @return True if the Str object starts with the specified string
     */
    boolean startsWith(CharSequence s) {
        return subSequence(0, Math.min(s.length(), length())).equals(s);
    }


    /**
     * Determine if this Str object starts with the specified string
     * @param s The string to search for
     * @return True if the Str object starts with the specified string
     */
    boolean endsWith(CharSequence s) {
        return subSequence(Math.max(length() - s.length(), 0), length()).equals(s);
    }

    static private Map<Integer, List<CharSequence>> groupBySizes(CharSequence[] search) {
        Map<Integer, List<CharSequence>> strings = new WeakHashMap<>();
        for (CharSequence s : search) {
            if (strings.containsKey(s.length()))
                strings.get(s.length()).add(s);
            else
                strings.put(s.length(), new ArrayList<CharSequence>() {{
                    add(s);
                }});
        }
        return strings;
    }

    private final StringBuilder sb;

    private Str(CharSequence... charSequences) {
        this.sb = new StringBuilder();
        add(charSequences);
    }

    /**
     * Determine if the string is a number
     * @return True if the string is a valid number, false otherwise
     */
    boolean isNum() {
        return isNumber(this);
    }

    /**
     * Determine if the string is an integer
     * @return True if the string is a valid integer, false otherwise
     */
    boolean isInt() {
        return isInteger(this);
    }

    /**
     * Add all the specified strings to this Str object
     * @param charSequences The strings to add
     * @return The Str object
     */
    Str add(CharSequence... charSequences) {
        for(CharSequence cs : charSequences)
            sb.append(cs);
        return this;
    }

    /**
     * Add all the specified characters to this Str object
     * @param chars The characters to add
     * @return The Str object
     */
    Str add(char... chars) {
        for(char c : chars)
            sb.append(c);
        return this;
    }

    /**
     * Convert string to an optional integer
     * @return Optional integer if the string is a valid integer, Optional.empty otherwise
     */
    Optional<Integer> toInteger() {
        return toInteger(this);
    }

    /**
     * Convert string to an optional double
     * @return Optional double if the string is a valid double, Optional.empty otherwise
     */
    Optional<Double> toDouble() {
        return toDouble(this);
    }

    /**
     * Delete all characters from the Str object
     * @return This Str object
     */
    Str delete() {
        sb.setLength(0);
        return this;
    }

    /**
     * Determine if the string is null or empty
     * @return True if the string is null or empty, false otherwise
     */
    boolean isEmpty() {
        return isEmpty(this);
    }

    /**
     * Determine if the string is not empty. Negation of <code>Str.isEmpty(s)</code>
     * @return True if the string is not empty, false otherwise
     */
    boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * Trim all whitespace from the beginning and end of a string
     * @return The Str object
     */
    Str trim() {
        return ltrim().rtrim();
    }

    /**
     * Trim characters at the beginning and end of the string
     * @param s The string to trim
     * @return The Str object
     */
    Str trim(CharSequence s) {
        return lrtrim(0, s);
    }

    /**
     * Trim all whitespace from the end of a string
     * @return The Str object
     */
    Str rtrim() {
        int length = length();
        int last = length - 1;
        if (length > 0 && Character.isWhitespace(charAt(last))) {
            int i = last - 1;
            for (; i >= 0; i--) {
                if (!Character.isWhitespace(charAt(i))) {
                    break;
                }
            }
            sb.delete(i + 1, length);
        }
        return this;
    }

    /**
     * Trim all whitespace from the beginning of a string
     * @return The Str object
     */
    Str ltrim() {
        int length = length();
        if (length > 0 && Character.isWhitespace(charAt(0))) {
            int i = 1;
            for (; i < length; i++) {
                if (!Character.isWhitespace(charAt(i))) {
                    break;
                }
            }
            sb.delete(0, i);
        }
        return this;
    }

    /**
     * Trim s [-1 == left, 1 == right, 0 == left and right]
     * @param lr Indicates which direction to trim -1 for left, 0 for left and right, and 1 for right.
     * @param s The string to trim.
     * @return A self referencing Str to support a fluent api.
     */
    protected Str lrtrim(int lr, CharSequence... s) {
        Map<Integer, List<CharSequence>> strings = groupBySizes(s);

        List<Integer> sizes = strings.keySet().stream()
                .sorted(Comparator.reverseOrder())
                .filter(it -> it > 0 && it <= length())
                .collect(Collectors.toList());

        int i = 0;
        boolean trim;

        // left trim or both
        if (lr == 0 || lr == -1) {
            int len = length();
            do {
                trim = false;
                for (int size : sizes) {
                    int to = i + size;
                    if (to < len && strings.getOrDefault(size, emptyList).contains(subSequence(i, to)) ) {
                        i = to;
                        trim = true;
                        break;
                    }
                }
            } while(trim);

            if (i > 0) sb.delete(0, i);
        }

        // right trim or both
        if (lr == 0 || lr == 1) {
            int len = length();
            i = len;
            do {
                trim = false;
                for (int size : sizes) {
                    int from = i - size;
                    if (from >= 0 && strings.getOrDefault(size, emptyList).contains(subSequence(from, i))) {
                        i = from;
                        trim = true;
                        break;
                    }
                }
            } while(trim);

            if (i < len) sb.delete(i, len);
        }
        return this;
    }

    /**
     * Repeat the specified string len number of times
     * @param s The string to repeat
     * @param len How many times to repeat the string. If len is negative, the string will be prepended
     * @return The Str object
     */
    Str repeat(CharSequence s, int len) {
        if (len > 0)
            sb.append(String.join(EMPTY, Collections.nCopies(len, s)));
        else if (len < 0)
            sb.insert(0, String.join(EMPTY, Collections.nCopies(Math.abs(len), s)));

        return this;
    }

    /**
     * Repeat the specified string len number of times
     * @param c The character to repeat
     * @param len How many times to repeat the string. If len is negative, the string will be prepended
     * @return The Str object
     */
    Str repeat(char c, int len) {
        return repeat(String.valueOf(c), len);
    }

    /**
     * Add any number of spaces to the string
     * @param spaces The number of spaces to add
     * @return The Str object
     */
    Str space(int spaces) {
        return repeat(Chars.SPACE, spaces);
    }

    /**
     * Add a space to the string
     * @return The Str object
     */
    Str space() {
        return space(1);
    }

    /**
     * Determine if the string object contains s
     * @param s The sequence to search for
     * @return true if this Str contains s, false otherwise
     */
    public boolean contains(CharSequence s) {
        return sb.indexOf(s.toString()) > -1;
    }

    /**
     * Returns the length (character count).
     *
     * @return  the length of the sequence of characters currently
     *          represented by this object
     */
    @Override
    public int length() {
        return sb.length();
    }

    /**
     * Returns the {@code char} value in this sequence at the specified index.
     * The first {@code char} value is at index {@code 0}, the next at index
     * {@code 1}, and so on, as in array indexing.
     * <p>
     * The index argument must be greater than or equal to
     * {@code 0}, and less than the length of this sequence.
     *
     * <p>If the {@code char} value specified by the index is a
     * <a href="Character.html#unicode">surrogate</a>, the surrogate
     * value is returned.
     *
     * @param      index   the index of the desired {@code char} value.
     * @return     the {@code char} value at the specified index.
     * @throws     IndexOutOfBoundsException  if {@code index} is
     *             negative or greater than or equal to {@code length()}.
     */
    @Override
    public char charAt(int index) {
        return sb.charAt(index);
    }

    /**
     * Returns a new character sequence that is a subsequence of this sequence.
     *
     * <p> An invocation of this method of the form
     *
     * <pre>{@code
     * sb.subSequence(begin,&nbsp;end)}</pre>
     *
     * behaves in exactly the same way as the invocation
     *
     * <pre>{@code
     * sb.substring(begin,&nbsp;end)}</pre>
     *
     * This method is provided so that this class can
     * implement the {@link CharSequence} interface.
     *
     * @param      start   the start index, inclusive.
     * @param      end     the end index, exclusive.
     * @return     the specified subsequence.
     *
     * @throws  IndexOutOfBoundsException
     *          if {@code start} or {@code end} are negative,
     *          if {@code end} is greater than {@code length()},
     *          or if {@code start} is greater than {@code end}
     */
    @Override
    public CharSequence subSequence(int start, int end) {
        return sb.subSequence(start, end);
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
