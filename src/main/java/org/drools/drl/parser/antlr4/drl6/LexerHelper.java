package org.drools.drl.parser.antlr4.drl6;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.misc.Interval;

/**
 * Minimal stub to satisfy generated DRL6 lexer dependency.
 * For our token-rewrite use cases we only need a placeholder implementation.
 */
public class LexerHelper {
    private final CharStream input;

    public LexerHelper(CharStream input) {
        this.input = input;
    }

    public boolean isRhsDrlEnd() {
        int idx = input.index();
        if (idx < 0 || idx >= input.size()) {
            return false;
        }

        // Require only whitespace between start of line and "end"
        int scan = idx - 1;
        while (scan >= 0) {
            char ch = input.getText(new Interval(scan, scan)).charAt(0);
            if (ch == '\n' || ch == '\r') {
                break;
            }
            if (!Character.isWhitespace(ch)) {
                return false;
            }
            scan--;
        }

        int end = Math.min(input.size() - 1, idx + 6);
        String ahead = input.getText(new Interval(idx, end)).toLowerCase();
        if (!ahead.startsWith("end")) {
            return false;
        }
        if (ahead.length() == 3) {
            return true;
        }
        char next = ahead.charAt(3);
        return Character.isWhitespace(next) || next == ';';
    }
}
