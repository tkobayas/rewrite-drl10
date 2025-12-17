package org.drools.rewrite.drl;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Attempts to normalize chained constraints that omit the left-hand side in subsequent operands.
 * Example: {@code Person(name == "Mark" || == "Mario")} -> {@code Person(name == "Mark" || name == "Mario")}
 *
 * This is a heuristic token-level rewrite intended for DRL LHS constraints.
 */
public class HalfConstraintRecipe extends Recipe {
    // Matches "<lhs> <op> <rhs> <logical> <op> <rhs2>" with missing lhs in the second comparison.
    private static final Pattern HALF_CONSTRAINT = Pattern.compile(
            "(?<lhs>[A-Za-z_][\\w\\.]*)\\s*(?<op>==|!=|<=|>=|<|>)\\s*(?<rhs1>[^|&;\\n,]+?)\\s*(?<logical>\\|\\||\\bor\\b)\\s*(?<halfop>==|!=|<=|>=|<|>)\\s*(?<rhs2>[^|&;\\n,]+)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public String getDisplayName() {
        return "Rewrite half-constraints";
    }

    @Override
    public String getDescription() {
        return "Fills in missing left-hand operands in chained constraints to comply with DRL10 parsing.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofSeconds(30);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                String original = text.getText();
                String rewritten = rewriteHalfConstraints(original);
                if (original.equals(rewritten)) {
                    return text;
                }
                return text.withText(rewritten);
            }
        };
    }

    private static String rewriteHalfConstraints(String source) {
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
            out.append(source, idx, whenIdx + 4);
            String lhsBlock = source.substring(whenIdx + 4, thenIdx);
            out.append(rewriteWithinWhen(lhsBlock));
            idx = thenIdx;
        }
        return out.toString();
    }

    private static String rewriteWithinWhen(String block) {
        String current = block;
        while (true) {
            Matcher matcher = HALF_CONSTRAINT.matcher(current);
            StringBuffer sb = new StringBuffer();
            boolean changed = false;
            while (matcher.find()) {
                changed = true;
                String lhs = matcher.group("lhs");
                String logical = matcher.group("logical");
                String halfop = matcher.group("halfop");
                String replacement = matcher.group("lhs") + " " + matcher.group("op") + " " + matcher.group("rhs1") +
                        " " + logical + " " + lhs + " " + halfop + " " + matcher.group("rhs2");
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            if (!changed) {
                return current;
            }
            current = sb.toString();
        }
    }
}
