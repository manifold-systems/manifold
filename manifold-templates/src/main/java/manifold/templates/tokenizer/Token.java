package manifold.templates.tokenizer;


public class Token {

    public enum TokenType {
        STRING_CONTENT,
        EXPRESSION,
        STATEMENT,
        DIRECTIVE,
        COMMENT
    }

    private TokenType _type;
    private String _content;
    private int _offset;
    private int _line;
    private int _position;
    private int _endPos;

    public Token(TokenType type, String content, int line, int column, int position, int endPos) {
        _type = type;
        _content = content;
        _offset = column;
        _line = line;
        _position = position;
        _endPos = endPos;
    }

    public TokenType getType() {
        return _type;
    }

    public String getContent() {
        return _content;
    }

    public int getOffset() {
        return _offset;
    }

    public int getLine() {
        return _line;
    }

    public int getPosition() {
        return _position;
    }

    public int getEndPosition() {
        return _endPos;
    }
}
