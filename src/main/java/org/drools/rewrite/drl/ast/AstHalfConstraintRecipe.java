package org.drools.rewrite.drl.ast;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.ParserRuleContext;
import org.drools.rewrite.drl.antlr.DRLParser;
import org.drools.rewrite.drl.antlr.DRLParserBaseListener;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Token-based half-constraint normalization that scopes changes to LHS (between when/then).
 */
public class AstHalfConstraintRecipe extends BaseAstDrlRecipe {
    private static final Set<String> RELATIONAL = new HashSet<>(List.of("==", "!=", "<=", ">=", "<", ">"));
    private static final Set<String> LOGICAL = new HashSet<>(List.of("||", "or", "OR"));

    @Override
    public String getDisplayName() {
        return "AST: Rewrite half-constraints";
    }

    @Override
    public String getDescription() {
        return "Fills in missing left-hand operands in chained constraints using token-level rewrites (LHS only).";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofSeconds(30);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return visitor(source -> rewriteWithParser(source, this::process));
    }

    private void process(DRLParser parser, DRLParser.CompilationUnitContext cu, CommonTokenStream tokens, TokenStreamRewriter rewriter) {
        ParseTreeWalker.DEFAULT.walk(new DRLParserBaseListener() {
            @Override
            public void enterConstraints(DRLParser.ConstraintsContext ctx) {
                rewriteInterval(ctx, tokens, rewriter);
            }

            @Override
            public void enterPositionalConstraints(DRLParser.PositionalConstraintsContext ctx) {
                rewriteInterval(ctx, tokens, rewriter);
            }
        }, cu);
    }

    private void rewriteInterval(ParserRuleContext ctx, CommonTokenStream tokens, TokenStreamRewriter rewriter) {
        int startIdx = ctx.getStart().getTokenIndex();
        int endIdx = ctx.getStop().getTokenIndex();
        List<Integer> defaults = defaultTokenIndices(tokens, new Interval(startIdx, endIdx));
        String lastLhs = null;
        for (int di = 0; di < defaults.size(); di++) {
            int ti = defaults.get(di);
            Token t = tokens.get(ti);
            String text = t.getText();
            if (",".equals(text)) {
                lastLhs = null;
                continue;
            }
            if (isRelational(text)) {
                String lhs = extractLhs(tokens, defaults, di);
                if (lhs != null && !lhs.isBlank()) {
                    lastLhs = lhs.trim();
                }
            } else if (isIdentifier(text)) {
                Token prev = previousDefault(tokens, defaults, di);
                Token next = nextDefault(tokens, defaults, di);
                if (prev != null && next != null && isCandidateOperatorContext(prev.getText(), next.getText())) {
                    String lhs = extractLhs(tokens, defaults, di);
                    if (lhs != null && !lhs.isBlank()) {
                        lastLhs = lhs.trim();
                    }
                }
            }
            if (isLogical(text) && di + 1 < defaults.size()) {
                int nextIdx = defaults.get(di + 1);
                Token next = tokens.get(nextIdx);
                if (isRelational(next.getText())) {
                    String lhsToUse = lastLhs != null ? lastLhs : lastIdentifierBefore(defaults, tokens, di);
                    if (lhsToUse != null) {
                        rewriter.insertBefore(next, lhsToUse + " ");
                    }
                }
            }
        }
    }

    private static boolean isRelational(String text) {
        return RELATIONAL.contains(text);
    }

    private static boolean isLogical(String text) {
        return LOGICAL.contains(text);
    }

    private static boolean isIdentifier(String text) {
        return !text.isEmpty() && Character.isLetter(text.charAt(0));
    }

    private static boolean isCandidateOperatorContext(String prevText, String nextText) {
        boolean prevOk = Character.isLetterOrDigit(prevText.charAt(prevText.length() - 1)) ||
                ")".equals(prevText) || "]".equals(prevText) || "\"".equals(prevText) || "$".equals(prevText);
        boolean nextOk = Character.isLetterOrDigit(nextText.charAt(0)) || "$".equals(nextText) || "\"".equals(nextText);
        return prevOk && nextOk;
    }

    private static Token previousDefault(CommonTokenStream tokens, List<Integer> defaults, int pos) {
        for (int i = pos - 1; i >= 0; i--) {
            Token t = tokens.get(defaults.get(i));
            if (t.getChannel() == Token.DEFAULT_CHANNEL) {
                return t;
            }
        }
        return null;
    }

    private static String lastIdentifierBefore(List<Integer> defaults, CommonTokenStream tokens, int pos) {
        for (int i = pos - 1; i >= 0; i--) {
            Token t = tokens.get(defaults.get(i));
            String txt = t.getText();
            if (isIdentifier(txt)) {
                return txt;
            }
            if (isLogical(txt) || ",".equals(txt) || "(".equals(txt)) {
                break;
            }
        }
        return null;
    }

    private static Token nextDefault(CommonTokenStream tokens, List<Integer> defaults, int pos) {
        for (int i = pos + 1; i < defaults.size(); i++) {
            Token t = tokens.get(defaults.get(i));
            if (t.getChannel() == Token.DEFAULT_CHANNEL) {
                return t;
            }
        }
        return null;
    }

    private static String extractLhs(CommonTokenStream tokens, List<Integer> defaults, int relPos) {
        int startPos = relPos - 1;
        while (startPos >= 0) {
            String txt = tokens.get(defaults.get(startPos)).getText();
            if (isLogical(txt) || ",".equals(txt) || "(".equals(txt)) {
                startPos++;
                break;
            }
            startPos--;
        }
        startPos = Math.max(startPos, 0);
        if (startPos >= relPos) {
            return null;
        }
        int startTokenIdx = defaults.get(startPos);
        int endTokenIdx = defaults.get(relPos - 1);
        return tokens.getTokenSource().getInputStream()
                .getText(Interval.of(tokens.get(startTokenIdx).getStartIndex(), tokens.get(endTokenIdx).getStopIndex()));
    }

    private static List<Integer> defaultTokenIndices(CommonTokenStream tokens, Interval span) {
        List<Integer> idx = new ArrayList<>();
        for (int i = span.a; i <= span.b; i++) {
            if (tokens.get(i).getChannel() == Token.DEFAULT_CHANNEL) {
                idx.add(i);
            }
        }
        return idx;
    }

}
