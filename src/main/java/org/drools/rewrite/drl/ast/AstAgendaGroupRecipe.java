package org.drools.rewrite.drl.ast;

import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.drools.rewrite.drl.antlr.DRLParser;
import org.drools.rewrite.drl.antlr.DRLParserBaseListener;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;

import java.time.Duration;

/**
 * Token-based agenda-group to ruleflow-group rewrite between rule header and when.
 */
public class AstAgendaGroupRecipe extends BaseAstDrlRecipe {
    @Override
    public String getDisplayName() {
        return "AST: Rewrite agenda-group to ruleflow-group";
    }

    @Override
    public String getDescription() {
        return "Replaces agenda-group attributes with ruleflow-group in rule headers.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofSeconds(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return visitor(source -> rewriteWithParser(source, this::process));
    }

    private void process(DRLParser parser, DRLParser.CompilationUnitContext cu, CommonTokenStream tokens, TokenStreamRewriter rewriter) {
        ParseTreeWalker.DEFAULT.walk(new DRLParserBaseListener() {
            @Override
            public void enterAttributes(DRLParser.AttributesContext ctx) {
                int start = ctx.getStart().getTokenIndex();
                int end = ctx.getStop().getTokenIndex();
                for (int i = start; i <= end; i++) {
                    Token token = tokens.get(i);
                    if (token.getType() == DRLParser.DRL_AGENDA_GROUP) {
                        rewriter.replace(token, "ruleflow-group");
                    }
                }
            }
        }, cu);
    }
}
