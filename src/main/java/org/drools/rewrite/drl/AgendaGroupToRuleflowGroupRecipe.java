package org.drools.rewrite.drl;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Rewrite legacy agenda-group attribute to ruleflow-group.
 */
public class AgendaGroupToRuleflowGroupRecipe extends Recipe {
    private static final Pattern AGENDA_GROUP = Pattern.compile("\\bagenda-group\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public String getDisplayName() {
        return "Rewrite agenda-group to ruleflow-group";
    }

    @Override
    public String getDescription() {
        return "DRL10 uses ruleflow-group attribute; replace occurrences of agenda-group in rule attributes.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofSeconds(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                String original = text.getText();
                String rewritten = rewriteRuleAttributes(original);
                if (original.equals(rewritten)) {
                    return text;
                }
                return text.withText(rewritten);
            }
        };
    }

    private static String rewriteRuleAttributes(String source) {
        StringBuilder out = new StringBuilder();
        int idx = 0;
        while (true) {
            int ruleIdx = indexOfWord(source, "rule", idx);
            if (ruleIdx < 0) {
                out.append(source.substring(idx));
                break;
            }
            int whenIdx = indexOfWord(source, "when", ruleIdx);
            if (whenIdx < 0) {
                out.append(source.substring(idx));
                break;
            }
            // copy up to rule keyword
            out.append(source, idx, ruleIdx);
            String header = source.substring(ruleIdx, whenIdx);
            out.append(AGENDA_GROUP.matcher(header).replaceAll("ruleflow-group"));
            idx = whenIdx;
        }
        return out.toString();
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
