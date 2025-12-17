package org.drools.rewrite.drl;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.time.Duration;

/**
 * Replace infix logical operators used to compose LHS patterns with textual {@code and}/{@code or}.
 */
public class LhsLogicalOperatorRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "Replace &&/|| in LHS pattern composition";
    }

    @Override
    public String getDescription() {
        return "DRL10 no longer accepts infix &&/|| between patterns. This rewrites likely pattern connectors to textual and/or.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofSeconds(10);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                String original = text.getText();
                String rewritten = rewriteLhsLogical(original);
                if (original.equals(rewritten)) {
                    return text;
                }
                return text.withText(rewritten);
            }
        };
    }

    private static String rewriteLhsLogical(String source) {
        // Operate within each when/then block to limit scope.
        StringBuilder result = new StringBuilder();
        int idx = 0;
        while (true) {
            int whenIdx = DrlTextUtils.indexOfWord(source, "when", idx);
            if (whenIdx < 0) {
                result.append(source.substring(idx));
                break;
            }
            int thenIdx = DrlTextUtils.indexOfWord(source, "then", whenIdx);
            if (thenIdx < 0) {
                result.append(source.substring(idx));
                break;
            }
            // Copy text before when
            result.append(source, idx, whenIdx);
            String lhs = source.substring(whenIdx, thenIdx);
            result.append(rewriteWithinLhs(lhs));
            idx = thenIdx;
        }
        return result.toString();
    }

    private static String rewriteWithinLhs(String lhsSection) {
        StringBuilder rewritten = new StringBuilder();
        boolean inString = false;
        char stringDelimiter = 0;
        int parenDepth = 0;
        java.util.Deque<Integer> argStack = new java.util.ArrayDeque<>();
        for (int i = 0; i < lhsSection.length(); i++) {
            char c = lhsSection.charAt(i);
            if (!inString && (c == '"' || c == '\'')) {
                inString = true;
                stringDelimiter = c;
                rewritten.append(c);
                continue;
            } else if (inString) {
                rewritten.append(c);
                if (c == stringDelimiter && lhsSection.charAt(Math.max(0, i - 1)) != '\\') {
                    inString = false;
                }
                continue;
            }
            if (c == '&' && i + 1 < lhsSection.length() && lhsSection.charAt(i + 1) == '&') {
                if (looksLikePatternConnector(lhsSection, i, parenDepth, argStack.size())) {
                    rewritten.append("and");
                    i++; // skip next &
                    continue;
                }
            }
            if (c == '|' && i + 1 < lhsSection.length() && lhsSection.charAt(i + 1) == '|') {
                if (looksLikePatternConnector(lhsSection, i, parenDepth, argStack.size())) {
                    rewritten.append("or");
                    i++; // skip next |
                    continue;
                }
            }
            rewritten.append(c);
            if (c == '(') {
                parenDepth++;
                int prev = previousNonWhitespaceSameLine(lhsSection, i - 1);
                if (prev >= 0 && Character.isLetterOrDigit(lhsSection.charAt(prev))) {
                    argStack.push(parenDepth);
                }
            } else if (c == ')') {
                if (!argStack.isEmpty() && argStack.peek() == parenDepth) {
                    argStack.pop();
                }
                parenDepth = Math.max(0, parenDepth - 1);
            }
        }
        return rewritten.toString();
    }

    private static boolean looksLikePatternConnector(String text, int opIndex, int parenDepth, int argDepth) {
        // Avoid rewriting connectors deep inside constraint expressions (e.g., inside pattern argument lists).
        if (parenDepth > 1 || argDepth > 0) {
            return false;
        }
        int prev = previousNonWhitespace(text, opIndex - 1);
        int next = nextNonWhitespace(text, opIndex + 2);
        if (prev < 0 || next < 0) {
            return false;
        }
        char nextChar = text.charAt(next);
        // Pattern connectors usually precede a type name (capitalized) or an opening paren wrapping patterns.
        boolean nextLooksLikePattern = Character.isUpperCase(nextChar) ||
                nextChar == '(' || // allow grouped patterns regardless of case inside
                (nextChar == '(' && (next + 1 < text.length() && Character.isUpperCase(text.charAt(next + 1)))) ||
                nextChar == '?'; // inline query ?Foo()
        return nextLooksLikePattern;
    }

    private static int previousNonWhitespace(String text, int from) {
        for (int i = from; i >= 0; i--) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int nextNonWhitespace(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int previousNonWhitespaceSameLine(String text, int from) {
        for (int i = from; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                return -1;
            }
            if (!Character.isWhitespace(c)) {
                return i;
            }
        }
        return -1;
    }
}
