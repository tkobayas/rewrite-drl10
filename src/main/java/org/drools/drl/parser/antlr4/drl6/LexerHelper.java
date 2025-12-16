package org.drools.drl.parser.antlr4.drl6;

import org.antlr.v4.runtime.CharStream;

/**
 * Minimal stub to satisfy generated DRL6 lexer dependency.
 * For our token-rewrite use cases we only need a placeholder implementation.
 */
public class LexerHelper {
    public LexerHelper(CharStream input) {
        // no-op
    }

    public boolean isRhsDrlEnd() {
        // Conservative default; adjust if RHS parsing is needed.
        return false;
    }
}
