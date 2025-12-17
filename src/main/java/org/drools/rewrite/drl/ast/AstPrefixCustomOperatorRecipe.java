package org.drools.rewrite.drl.ast;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.ParserRuleContext;
import org.drools.rewrite.drl.antlr.DRLParser;
import org.drools.rewrite.drl.antlr.DRLParserBaseListener;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Token-based custom operator prefixing (`##`) scoped to LHS.
 */
public class AstPrefixCustomOperatorRecipe extends BaseAstDrlRecipe {
    private static final Set<String> BUILT_INS = new HashSet<>(List.of(
            "contains", "excludes", "matches", "memberof", "soundslike", "str",
            "after", "before", "coincides", "during", "finishedby", "finishes",
            "includes", "meets", "metby", "overlappedby", "overlaps", "startedby", "starts",
            "and", "or", "&&", "||"
    ));

    @Override
    public String getDisplayName() {
        return "AST: Prefix custom operators with ##";
    }

    @Override
    public String getDescription() {
        return "Prefixes identifier-based custom operators with ## within LHS constraints.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofSeconds(15);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return visitor(source -> rewriteWithParser(source, this::process));
    }

    private void process(DRLParser parser, DRLParser.CompilationUnitContext cu, CommonTokenStream tokens, TokenStreamRewriter rewriter) {
        ParseTreeWalker.DEFAULT.walk(new DRLParserBaseListener() {
            @Override
            public void enterOperator_key(DRLParser.Operator_keyContext ctx) {
                Token id = ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getSymbol() : null;
                if (id == null) {
                    return;
                }
                if (ctx.prefix != null) { // already has ## prefix
                    return;
                }
                String text = id.getText();
                if (BUILT_INS.contains(text.toLowerCase())) {
                    return;
                }
                rewriter.insertBefore(id, "##");
            }

            @Override
            public void enterNeg_operator_key(DRLParser.Neg_operator_keyContext ctx) {
                Token id = ctx.IDENTIFIER() != null ? ctx.IDENTIFIER().getSymbol() : null;
                if (id == null) {
                    return;
                }
                if (ctx.prefix != null) { // already has ## prefix
                    return;
                }
                String text = id.getText();
                if (BUILT_INS.contains(text.toLowerCase())) {
                    return;
                }
                rewriter.insertBefore(id, "##");
            }
        }, cu);
    }
}
