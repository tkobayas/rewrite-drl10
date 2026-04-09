package org.drools.rewrite.drl.ast;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.ParseTree;
import org.drools.rewrite.drl.antlr.DRLParser;
import org.drools.rewrite.drl.antlr.DRLParserBaseListener;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Removes unsupported annotations that appear between LHS logical operators and
 * the following pattern/expression, e.g. {@code or @Annot String()}.
 */
public class AstLhsAnnotationRecipe extends BaseAstDrlRecipe {

    private static final Logger LOG = LoggerFactory.getLogger(AstLhsAnnotationRecipe.class);

    @Override
    public String getDisplayName() {
        return "AST: Drop annotations in LHS";
    }

    @Override
    public String getDescription() {
        return "Removes annotations between LHS logical operators and the following expression because DRL10 no longer accepts them there.";
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
                deleteDirectAnnotations(ctx, tokens, rewriter);
            }

            @Override
            public void enterLhsAnd(DRLParser.LhsAndContext ctx) {
                deleteDirectAnnotations(ctx, tokens, rewriter);
            }
        }, cu);
    }

    private void deleteDirectAnnotations(ParseTree ctx, CommonTokenStream tokens, TokenStreamRewriter rewriter) {
        for (int i = 0; i < ctx.getChildCount(); i++) {
            ParseTree child = ctx.getChild(i);
            if (child instanceof DRLParser.DrlAnnotationContext annotation) {
                int startIdx = annotation.getStart().getTokenIndex();
                int endIdx = extendThroughTrailingInlineWhitespace(tokens, annotation.getStop().getTokenIndex());
                LOG.warn("Dropping unsupported LHS operator annotation at line {}, column {}: {}",
                        annotation.getStart().getLine(),
                        annotation.getStart().getCharPositionInLine(),
                        annotation.getText());
                rewriter.delete(tokens.get(startIdx), tokens.get(endIdx));
            }
        }
    }

    private static int extendThroughTrailingInlineWhitespace(CommonTokenStream tokens, int endIdx) {
        int idx = endIdx + 1;
        while (idx < tokens.size()) {
            Token token = tokens.get(idx);
            if (token.getChannel() == Token.DEFAULT_CHANNEL) {
                break;
            }
            String text = token.getText();
            if (text == null || text.contains("\n") || text.contains("\r")) {
                break;
            }
            idx++;
            endIdx = token.getTokenIndex();
        }
        return endIdx;
    }
}
