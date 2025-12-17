package org.drools.rewrite.drl.ast;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.misc.Interval;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Token-based replacement of &&/|| with and/or in LHS pattern composition, skipping RHS and constraint expressions.
 */
public class AstLhsLogicalOperatorRecipe extends BaseAstDrlRecipe {

    @Override
    public String getDisplayName() {
        return "AST: Replace &&/|| in LHS pattern composition";
    }

    @Override
    public String getDescription() {
        return "Rewrites infix logical operators between patterns to textual and/or, avoiding RHS and constraint contexts.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofSeconds(10);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return visitor(source -> rewriteWithTokens(source, this::process));
    }

    private void process(CommonTokenStream tokens, TokenStreamRewriter rewriter) {
        for (Interval span : whenThenSpans(tokens)) {
            Deque<Integer> argStack = new ArrayDeque<>();
            int parenDepth = 0;
            for (int i = span.a; i <= span.b; i++) {
                Token t = tokens.get(i);
                String text = t.getText();
                if ("(".equals(text)) {
                    parenDepth++;
                    Token prev = previousDefault(tokens, i, span.a);
                    if (prev != null && Character.isLetterOrDigit(prev.getText().charAt(prev.getText().length() - 1))) {
                        argStack.push(parenDepth);
                    }
                } else if (")".equals(text)) {
                    if (!argStack.isEmpty() && argStack.peek() == parenDepth) {
                        argStack.pop();
                    }
                    parenDepth = Math.max(0, parenDepth - 1);
                } else if ("&&".equals(text) && argStack.isEmpty()) {
                    rewriter.replace(t, "and");
                } else if ("||".equals(text) && argStack.isEmpty()) {
                    rewriter.replace(t, "or");
                }
            }
        }
    }

    private static Token previousDefault(CommonTokenStream tokens, int idx, int lowerBound) {
        for (int i = idx - 1; i >= lowerBound; i--) {
            Token t = tokens.get(i);
            if (t.getChannel() == Token.DEFAULT_CHANNEL) {
                return t;
            }
        }
        return null;
    }

    private static List<Interval> whenThenSpans(CommonTokenStream tokens) {
        List<Interval> spans = new ArrayList<>();
        List<? extends Token> all = tokens.getTokens();
        int i = 0;
        while (i < all.size()) {
            Token t = all.get(i);
            if ("when".equalsIgnoreCase(t.getText())) {
                int start = i;
                int j = i + 1;
                while (j < all.size() && !"then".equalsIgnoreCase(all.get(j).getText())) {
                    j++;
                }
                if (j < all.size()) {
                    spans.add(new Interval(start + 1, j - 1));
                    i = j;
                    continue;
                } else {
                    break;
                }
            }
            i++;
        }
        return spans;
    }
}
