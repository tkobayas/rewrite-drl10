package org.drools.rewrite.drl;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.Markers;
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
    // Matches "<lhs> <op> <rhs> <logical> == <rhs2>" with missing lhs in the second comparison.
    private static final Pattern HALF_CONSTRAINT = Pattern.compile(
            "(?<lhs>[A-Za-z_][\\w\\.]*?)\\s*(?<op>==|!=|<=|>=|<|>)\\s*(?<rhs1>[^|&;\\n]+?)\\s*(?<logical>\\|\\||\\bor\\b)\\s*==\\s*(?<rhs2>[^|&;\\n]+)",
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
        Matcher matcher = HALF_CONSTRAINT.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String lhs = matcher.group("lhs");
            String logical = matcher.group("logical");
            String replacement = matcher.group(0).replaceFirst("\\|\\|\\s*==", logical + " " + lhs + " ==");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
