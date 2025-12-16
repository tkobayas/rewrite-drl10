package org.drools.rewrite.drl;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
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
            "contains", "excludes", "matches", "memberOf", "soundslike", "str",
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
                if (StringUtils.equals(original, rewritten)) {
                    return text;
                }
                return text.withText(rewritten);
            }
        };
    }

    private static String rewriteOperators(String source) {
        Matcher matcher = CUSTOM_OP.matcher(source);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String op = matcher.group("op");
            String lowerOp = op.toLowerCase();
            if (lowerOp.startsWith("##") || BUILT_INS.contains(lowerOp)) {
                matcher.appendReplacement(sb, matcher.group(0));
                continue;
            }
            String lhs = matcher.group("lhs");
            String not = matcher.group("not") == null ? "" : matcher.group("not");
            String rhs = matcher.group("rhs");
            String replacement = lhs + " " + not + "##" + op + " " + rhs;
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
