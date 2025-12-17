package org.drools.rewrite.drl.ast;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.drools.rewrite.drl.antlr.DRLParser;
import org.drools.rewrite.drl.antlr.DRLParserBaseListener;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;

import java.time.Duration;

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
        return visitor(source -> rewriteWithParser(source, this::process));
    }

    private void process(DRLParser parser, DRLParser.CompilationUnitContext cu, CommonTokenStream tokens, TokenStreamRewriter rewriter) {
        ParseTreeWalker.DEFAULT.walk(new DRLParserBaseListener() {
            @Override
            public void enterLhsOr(DRLParser.LhsOrContext ctx) {
                for (TerminalNode or : ctx.OR()) {
                    if ("||".equals(or.getText())) {
                        rewriter.replace(or.getSymbol(), "or");
                    }
                }
            }

            @Override
            public void enterLhsAnd(DRLParser.LhsAndContext ctx) {
                for (TerminalNode and : ctx.AND()) {
                    if ("&&".equals(and.getText())) {
                        rewriter.replace(and.getSymbol(), "and");
                    }
                }
            }
        }, cu);
    }
}
