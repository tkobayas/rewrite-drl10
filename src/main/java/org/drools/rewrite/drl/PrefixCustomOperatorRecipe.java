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
            "includes", "meets", "metby", "overlappedby", "overlaps", "startedby", "starts"
    );

    // Roughly matches "<lhs> [not] <op> <rhs>" where op is an identifier.
    private static final Pattern CUSTOM_OP = Pattern.compile(
            "(?<lhs>[A-Za-z_][\\w\\.]*)\\s+(?<not>not\\s+)?(?<op>[A-Za-z_][\\w]*)\\s+(?<rhs>[^;\\n]+)");

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
            int whenIdx = indexOfWord(source, "when", idx);
            if (whenIdx < 0) {
                out.append(source.substring(idx));
                break;
            }
            int thenIdx = indexOfWord(source, "then", whenIdx);
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
        Matcher matcher = CUSTOM_OP.matcher(block);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String op = matcher.group("op");
            String lowerOp = op.toLowerCase();
            if (lowerOp.startsWith("##") || BUILT_INS.contains(lowerOp)) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String lhs = matcher.group("lhs");
            String not = matcher.group("not") == null ? "" : matcher.group("not");
            String rhs = matcher.group("rhs");
            String middle = not.isEmpty() ? " " + not : " ";
            String replacement = lhs + middle + "##" + op + " " + rhs;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static int indexOfWord(String text, String word, int fromIndex) {
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
