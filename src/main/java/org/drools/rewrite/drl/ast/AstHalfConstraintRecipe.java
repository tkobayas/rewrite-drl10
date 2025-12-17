package org.drools.rewrite.drl.ast;

import java.time.Duration;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.drools.rewrite.drl.antlr.DRLParser;
import org.drools.rewrite.drl.antlr.DRLParserBaseListener;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Token-based half-constraint normalization that scopes changes to LHS (between when/then).
 */
public class AstHalfConstraintRecipe extends BaseAstDrlRecipe {

    private static final Logger LOG = LoggerFactory.getLogger(AstHalfConstraintRecipe.class);

    private static final Set<String> RELATIONAL = new HashSet<>(List.of("==", "!=", "<=", ">=", "<", ">"));

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
        java.util.Set<ParserRuleContext> processed = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        ParseTreeWalker.DEFAULT.walk(new DRLParserBaseListener() {
            @Override
            public void enterOrRestriction(DRLParser.OrRestrictionContext ctx) {
                rewriteOrRestriction(ctx, tokens, rewriter, processed);
            }

            @Override
            public void enterAndRestriction(DRLParser.AndRestrictionContext ctx) {
                rewriteAndRestriction(ctx, tokens, rewriter, processed);
            }
        }, cu);
    }

    private void rewriteOrRestriction(DRLParser.OrRestrictionContext ctx, CommonTokenStream tokens, TokenStreamRewriter rewriter, java.util.Set<ParserRuleContext> processed) {
        List<DRLParser.AndRestrictionContext> parts = ctx.andRestriction();
        if (parts == null || parts.size() < 2) {
            return; // half-constraint exists in the multiple children case only
        }
        DRLParser.AndRestrictionContext anchor = parts.get(0); // the first part is a full constraint
        String leftOperand = getLeftOperandFromParent(anchor, tokens);
        if (leftOperand == null) {
            return;
        }

        for (int i = 1; i < parts.size(); i++) { // skip the first part which is not half-constraint
            DRLParser.AndRestrictionContext target = parts.get(i);
            if (target.singleRestriction().size() > 1) {
                LOG.warn("Warning: half constraint has multiple singleRestriction elements at line {}, column {}  Text: {}",
                         target.getStart().getLine(), target.getStart().getCharPositionInLine(), textOf(target, tokens));
                continue;
            }
            rewriteHalfConstraints(target.singleRestriction(0), leftOperand, tokens, rewriter, processed);
        }
    }

    private void rewriteAndRestriction(DRLParser.AndRestrictionContext ctx, CommonTokenStream tokens, TokenStreamRewriter rewriter, java.util.Set<ParserRuleContext> processed) {
        List<DRLParser.SingleRestrictionContext> parts = ctx.singleRestriction();
        if (parts == null || parts.size() < 2) {
            return; // half-constraint exists in the multiple children case only
        }
        DRLParser.SingleRestrictionContext anchor = parts.get(0); // the first part is a full constraint
        String leftOperand = getLeftOperandFromParent(anchor, tokens);
        if (leftOperand == null) {
            return;
        }
        for (int i = 1; i < parts.size(); i++) { // skip the first part which is not half-constraint
            DRLParser.SingleRestrictionContext target = parts.get(i);
            rewriteHalfConstraints(target, leftOperand, tokens, rewriter, processed);
        }
    }

    private void rewriteHalfConstraints(DRLParser.SingleRestrictionContext halfConstraint, String leftOperand, CommonTokenStream tokens, TokenStreamRewriter rewriter, java.util.Set<ParserRuleContext> processed) {
        if (processed.contains(halfConstraint)) {
            return;
        }
        Token first = firstDefault(halfConstraint, tokens);
        if (first != null && isRelational(first.getText())) {
            rewriter.insertBefore(first, leftOperand + " ");
            processed.add(halfConstraint);
        }
    }

    private static String textOf(ParserRuleContext ctx, CommonTokenStream tokens) {
        return tokens.getTokenSource().getInputStream()
                .getText(Interval.of(ctx.getStart().getStartIndex(), ctx.getStop().getStopIndex()));
    }

    private static String getLeftOperandFromParent(ParserRuleContext ctx, CommonTokenStream tokens) {
        // Walk up to the relationalExpression holding this restriction and take its left shiftExpression text
        ParserRuleContext cursor = ctx;
        while (cursor != null) {
            if (cursor instanceof DRLParser.RelationalExpressionContext) {
                DRLParser.RelationalExpressionContext rel = (DRLParser.RelationalExpressionContext) cursor;
                if (rel.left != null) {
                    return textOf(rel.left, tokens).trim();
                }
                break;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private static Token firstDefault(ParserRuleContext ctx, CommonTokenStream tokens) {
        int start = ctx.getStart().getTokenIndex();
        int end = ctx.getStop().getTokenIndex();
        for (int i = start; i <= end; i++) {
            Token t = tokens.get(i);
            if (t.getChannel() == Token.DEFAULT_CHANNEL) {
                return t;
            }
        }
        return null;
    }

    private static boolean isRelational(String text) {
        return RELATIONAL.contains(text);
    }
}
