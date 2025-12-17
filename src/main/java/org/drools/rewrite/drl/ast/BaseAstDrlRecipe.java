package org.drools.rewrite.drl.ast;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.drools.rewrite.drl.antlr.DRLLexer;
import org.drools.rewrite.drl.antlr.DRLParser;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.text.PlainText;
import org.openrewrite.text.PlainTextVisitor;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;

/**
 * Base for token-stream–driven DRL rewrites. Uses the generated DRL6 lexer to
 * operate on tokens and apply edits via TokenStreamRewriter.
 */
abstract class BaseAstDrlRecipe extends Recipe {

    protected PlainTextVisitor<ExecutionContext> visitor(Function<String, String> rewriter) {
        return new PlainTextVisitor<ExecutionContext>() {
            @Override
            public PlainText visitText(PlainText text, ExecutionContext executionContext) {
                String original = text.getText();
                String rewritten = rewriter.apply(original);
                if (original.equals(rewritten)) {
                    return text;
                }
                return text.withText(rewritten);
            }
        };
    }

    protected String rewriteWithTokens(String source, TokenProcessor processor) {
        DRLLexer lexer = new DRLLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
        processor.process(tokens, rewriter);
        return rewriter.getText();
    }

    protected String rewriteWithParser(String source, ParserProcessor processor) {
        DRLLexer lexer = new DRLLexer(CharStreams.fromString(source));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        DRLParser parser = new DRLParser(tokens);
        DRLParser.CompilationUnitContext cu = parser.compilationUnit();
        TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
        processor.process(parser, cu, tokens, rewriter);
        return rewriter.getText();
    }

    protected interface TokenProcessor {
        void process(CommonTokenStream tokens, TokenStreamRewriter rewriter);
    }

    protected interface ParserProcessor {
        void process(DRLParser parser, DRLParser.CompilationUnitContext cu, CommonTokenStream tokens, TokenStreamRewriter rewriter);
    }
}
