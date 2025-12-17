package org.drools.rewrite.drl;

final class DrlTextUtils {
    private DrlTextUtils() {
    }

    /**
     * Finds the next occurrence of {@code word} in {@code text} starting at {@code fromIndex},
     * ensuring it is delimited by non-alphanumeric characters (word boundary).
     *
     * @return the index of the word or -1 if not found.
     */
    static int indexOfWord(String text, String word, int fromIndex) {
        int idx = text.toLowerCase().indexOf(word.toLowerCase(), fromIndex);
        if (idx < 0) {
            return -1;
        }
        boolean startOk = idx == 0 || !Character.isLetterOrDigit(text.charAt(idx - 1));
        int endIdx = idx + word.length();
        boolean endOk = endIdx >= text.length() || !Character.isLetterOrDigit(text.charAt(endIdx));
        return (startOk && endOk) ? idx : -1;
    }
}
