package org.drools.rewrite.drl;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.time.Duration;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prefix custom operator usages with the DRL10-required {@code ##}.
 */
public class PrefixCustomOperatorRecipe extends Recipe {
    private static final Set<String> BUILT_INS = Set.of(
            "contains", "excludes", "matches", "memberof", "soundslike", "str",
            "after", "before", "coincides", "during", "finishedby", "finishes",
            "includes", "meets", "metby", "overlappedby", "overlaps", "startedby", "starts",
            "not", "and", "or"
    );

    @Override
    public String getDisplayName() {
        return "Prefix custom operators with ##";
    }

    @Override
    public String getDescription() {
        return "Ensures custom operators are prefixed with ## as required by DRL10.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofSeconds(15);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                String original = text.getText();
                String rewritten = rewriteOperators(original);
                if (original.equals(rewritten)) {
                    return text;
                }
                return text.withText(rewritten);
            }
        };
    }

    private static String rewriteOperators(String source) {
        StringBuilder out = new StringBuilder();
        int idx = 0;
        while (true) {
            int whenIdx = DrlTextUtils.indexOfWord(source, "when", idx);
            if (whenIdx < 0) {
                out.append(source.substring(idx));
                break;
            }
            int thenIdx = DrlTextUtils.indexOfWord(source, "then", whenIdx);
            if (thenIdx < 0) {
                out.append(source.substring(idx));
                break;
            }
            // Copy up to and including "when"
            out.append(source, idx, whenIdx + 4);
            String lhsBlock = source.substring(whenIdx + 4, thenIdx);
            out.append(rewriteWithinWhen(lhsBlock));
            idx = thenIdx;
        }
        return out.toString();
    }

    private static String rewriteWithinWhen(String block) {
        Pattern ident = Pattern.compile("\\b([A-Za-z_][\\w]*)\\b");
        Matcher m = ident.matcher(block);
        boolean[] inString = markStringRegions(block);
        StringBuilder sb = new StringBuilder(block);
        int offset = 0;
        while (m.find()) {
            String op = m.group(1);
            String lower = op.toLowerCase();
            if (lower.startsWith("##") || BUILT_INS.contains(lower)) {
                continue;
            }
            int start = m.start(1) + offset;
            int end = m.end(1) + offset;
            if (m.start(1) < inString.length && inString[m.start(1)]) {
                continue;
            }
            int prev = previousNonWhitespace(sb, start - 1);
            int next = nextNonWhitespace(sb, end);
            if (prev < 0 || next >= sb.length()) {
                continue;
            }
            char prevCh = sb.charAt(prev);
            char nextCh = sb.charAt(next);
            if (!isOperandBoundary(prevCh) || !isOperandBoundaryStart(nextCh) || nextCh == '(') {
                continue;
            }
            sb.insert(start, "##");
            offset += 2;
        }
        return sb.toString();
    }

    private static boolean[] markStringRegions(String text) {
        boolean[] inString = new boolean[text.length()];
        boolean inside = false;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inside = !inside;
            }
            inString[i] = inside;
        }
        return inString;
    }

    private static int previousNonWhitespace(CharSequence s, int idx) {
        for (int i = idx; i >= 0; i--) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int nextNonWhitespace(CharSequence s, int idx) {
        for (int i = idx; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return s.length();
    }

    private static boolean isOperandBoundary(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '"' || ch == ')' || ch == ']' || ch == '$' || ch == '_';
    }

    private static boolean isOperandBoundaryStart(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '"' || ch == '(' || ch == '$' || ch == '_';
    }
}
