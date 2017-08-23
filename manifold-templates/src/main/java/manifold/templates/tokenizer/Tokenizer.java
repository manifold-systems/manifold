package manifold.templates.tokenizer;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import manifold.templates.manifold.TemplateIssue;
import manifold.internal.javac.IIssue;
import manifold.templates.tokenizer.Token.TokenType;

import static manifold.templates.tokenizer.Token.TokenType.*;

public class Tokenizer {

    private List<TemplateIssue> _issues = new ArrayList<>();

    class TokenBuilder implements Iterator<Token> {
        String tokenString;
        int line, col;
        int index;
        int toJump = 0;

        TokenBuilder(String str) {
            this.tokenString = str;
            line = 1;
            col = 1;
            index = 0;
        }

        private Character peekBehind() {
            return peekBehind(1);
        }

        private Character peekBehind(int distance) {
            if (index - distance < 0) {
                return null;
            }
            return tokenString.charAt(index - distance);
        }

        private Character peekForward() {
            return peekForward(1);
        }

        private Character peekForward(int distance) {
            if (index + distance < tokenString.length()) {
                return tokenString.charAt(index + distance);
            }
            return null;
        }

        public boolean hasNext() {
            if (tokenString == null) {
                return false;
            }
            return index < tokenString.length();
        }

        public Token next() {
            if (index >= tokenString.length()) {
                throw new NoSuchElementException();
            }
            TokenType nextType = getNextTokenType();
            int pos = this.index;
            int col = this.col;
            int line = this.line;
            Token toReturn;
            if (nextType == STATEMENT) {
                advancePosition(2);
                toJump = 2;
                toReturn = next(nextType, true, line, col, pos,"%>");
                advancePosition(2);
            } else if (nextType == EXPRESSION) {
                if (isModernExpressionSyntax()) {
                    advancePosition(2);
                    toJump = 1;
                    toReturn = next(nextType, true, line, col, pos, "}");
                    advancePosition();
                } else {
                    advancePosition(3);
                    toJump = 2;
                    toReturn = next(nextType, true, line, col, pos, "%>");
                    advancePosition(2);
                }

            } else if (nextType == DIRECTIVE) {
                advancePosition(3);
                toJump = 2;
                toReturn = next(nextType, true, line, col, pos,"%>");
                advancePosition(2);
            } else if (nextType == COMMENT) {
                advancePosition(4);
                toJump = 4;
                toReturn = next(nextType, false, line, col, pos,"--%>");
                advancePosition(4);
            } else { //String Content
                toJump = 0;
                toReturn = next(nextType, false, line, col, pos,"<%", "${");
            }
            return toReturn;
        }

        public void remove() {
            throw new UnsupportedOperationException("remove");
        }


        private Token next(TokenType type, boolean quoteSensitive, int line, int col, int pos, String... terminateConditions) {
            int contentStartPos = index;
            int length = tokenString.length();
            List<Character> termStart = new ArrayList<Character>();
            for (String s: terminateConditions) {
                termStart.add(s.charAt(0));
            }
            int quoteState = 0;
            while (index < length) {
                char current = tokenString.charAt(index);
                if (current == '"' && quoteSensitive) {
                    if (quoteState == 1 && peekBehind() != '\\') {
                        quoteState = 0;
                    } else if (quoteState == 0) {
                        quoteState = 1;
                    }
                } else if (current == '\'' && quoteSensitive) {
                    if (quoteState == 2 && peekBehind() != '\\') {
                        quoteState = 0;
                    } else if (quoteState == 0) {
                        quoteState = 2;
                    }
                } else if (quoteState == 0) {
                    if (termStart.contains(current)) {
                        if (checkIfTerminates(terminateConditions)) {
                            String currentTokenString = tokenString.substring(contentStartPos, index);
                            if (type != STRING_CONTENT) {
                                currentTokenString = currentTokenString.trim();
                            }
                            return new Token(type, currentTokenString, line, col, pos, index + toJump);
                        }
                    }
                    if (type != COMMENT) {
                        checkIllegalOpenings(type);
                    }
                }
                advancePosition();
            }
            if (type == STRING_CONTENT) {
                return new Token(type, tokenString.substring(contentStartPos), line, col, pos, index);
            }
            addError("Tokenization Error: " + type + " is not closed", line);
            return new Token(type, tokenString.substring(contentStartPos), line, col, pos, index);
        }

        private boolean isModernExpressionSyntax() {
            if (tokenString.charAt(index) == '$') {
                return true;
            }
            return false;
        }

        private boolean checkIfTerminates(String[] terminateConditions) {
            for (String cond: terminateConditions) {
                boolean terminates = true;
                for (int i = 0; i < cond.length(); i += 1) {
                    Character c = peekForward(i);
                    if (c == null || cond.charAt(i) != c) {
                        terminates = false;
                    }
                }
                if (terminates) {
                    return true;
                }
            }
            return false;
        }

        private void checkIllegalOpenings(TokenType type) {
            if (tokenString.charAt(index) == '<' && peekForward() == '%') {
                if (peekForward(2) == '@') {
                    addError("Attempted to open new directive within " + type, line);
                } else if (peekForward(2) == '=') {
                    addError("Attempted to open new expression within " + type, line);
                } else {
                    addError("Attempted to open new statement within " + type, line);
                }
                next();
            }
            if (tokenString.charAt(index) == '$' && peekForward() == '{') {
                addError("Attempted to open new expression within " + type, line);
                next();
            }
        }

        /** Returns the correct token type to be parsed. */
        private TokenType getNextTokenType() {
            Character next = peekForward();
            if (tokenString.charAt(index) == '<' && next == '%') {
                if (peekForward(2) != null && peekForward(2) == '@') {
                    return DIRECTIVE;
                }
                if (peekForward(2) != null && peekForward(2) == '=') {
                    return EXPRESSION;
                }
                if (peekForward(2) != null && peekForward(2) == '-' && peekForward(3) != null && peekForward(3) == '-') {
                    return COMMENT;
                }
                return STATEMENT;
            } else if (tokenString.charAt(index) == '$' && next == '{') {
                return EXPRESSION;
            } else {
                return STRING_CONTENT;
            }
        }

        private void advancePosition() {
            if (index < this.tokenString.length()) {
                char current = tokenString.charAt(index);
                if (current == 10) {
                    this.line += 1;
                    this.col = 0;
                }
                this.col += 1;
                index += 1;
            }
        }

        private void advancePosition(int i) {
            for (int x = 0; x < i; x += 1) {
                advancePosition();
            }
        }

    }

    public List<Token> tokenize(String str) {
        ArrayList<Token> tokens = new ArrayList<Token>();
        TokenBuilder builder = new TokenBuilder(str);
        while (builder.hasNext()) {
            tokens.add(builder.next());
        }
        return tokens;
    }

    private void addError(String message, int line) {
        TemplateIssue error = new TemplateIssue(IIssue.Kind.Error, 0, line, 0, message);
        _issues.add( error );
    }

    public List<TemplateIssue> getIssues() {
        return  _issues;
    }
}
