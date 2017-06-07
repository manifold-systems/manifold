package manifold.api.json;

import java.io.IOException;
import java.io.Reader;

final class Tokenizer {
  private Reader source;
  private char ch;
  int line;
  int column;
  int offset;

  public Tokenizer(Reader source) {
    this.source = source;
    line = 1;
    column = 0;
    nextChar();
  }

  public Token next() {
    Token T;
    eatWhiteSpace();
    switch(ch) {
      case '"':
      case '\'':
        T = consumeString(ch);
        break;
      case '-':
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        T = consumeNumber();
        break;
      case '{':
        T = new Token(TokenType.LCURLY, "{", line, column);
        nextChar();
        break;
      case '}':
        T = new Token(TokenType.RCURLY, "}", line, column);
        nextChar();
        break;
      case '[':
        T = new Token(TokenType.LSQUARE, "[", line, column);
        nextChar();
        break;
      case ']':
        T = new Token(TokenType.RSQUARE, "]", line, column);
        nextChar();
        break;
      case ',':
        T = new Token(TokenType.COMMA, ",", line, column);
        nextChar();
        break;
      case ':':
        T = new Token(TokenType.COLON, ":", line, column);
        nextChar();
        break;
      case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g':
      case 'h': case 'i': case 'j': case 'k': case 'l': case 'm': case 'n':
      case 'o': case 'p': case 'q': case 'r': case 's': case 't': case 'u':
      case 'v': case 'w': case 'x': case 'y': case 'z': case 'A': case 'B':
      case 'C': case 'D': case 'E': case 'F': case 'G': case 'H': case 'I':
      case 'J': case 'K': case 'L': case 'M': case 'N': case 'O': case 'P':
      case 'Q': case 'R': case 'S': case 'T': case 'U': case 'V': case 'W':
      case 'X': case 'Y': case 'Z':
        T = consumeConstant();
        break;
      case '\0':
        T = new Token(TokenType.EOF, "EOF", line, column);
        break;
      default:
        T = new Token(TokenType.ERROR, String.valueOf(ch), line, column);
        nextChar();
    }
    return T;
  }

  /*
    string = '"' {char} '"' | "'" {char} "'".
    char = unescaped | "\" ('"' | "\" | "/" | "b" | "f" | "n" | "r" | "t" | "u" hex hex hex hex).
    unescaped = any printable Unicode character except '"', "'" or "\".
  */
  private Token consumeString(char quote) {
    StringBuilder sb = new StringBuilder();
    int l = line;
    int c = column;
    Token T;
    nextChar();
    while(moreChars() && ch != quote) {
      if(ch == '\\') {
        nextChar();
        switch(ch) {
          case '"':
          case '\\':
          case '/':
            sb.append(ch);
            nextChar();
            break;
          case 'b':
            sb.append('\b');
            nextChar();
            break;
          case 'f':
            sb.append('\f');
            nextChar();
            break;
          case 'n':
            sb.append('\n');
            nextChar();
            break;
          case 'r':
            sb.append('\r');
            nextChar();
            break;
          case 't':
            sb.append('\t');
            nextChar();
            break;
          case 'u':
            nextChar();
            int u = 0;
            for(int i = 0; i < 4; i++) {
              if(isHexDigit(ch)) {
                u = u * 16 + ch - '0';
                if(ch >= 'A') { // handle hex numbers: 'A' = 65, '0' = 48. 'A'-'0' = 17, 17 - 7 = 10
                  u = u - 7;
                }
              } else {
                T = new Token(TokenType.ERROR, sb.toString(), line, column);
                nextChar();
                return T;
              }
              nextChar();
            }
            sb.append((char) u);
            break;
          default:
            T = new Token(TokenType.ERROR, sb.toString(), line, column);
            nextChar();
            return T;
        }
      } else {
        sb.append(ch);
        nextChar();
      }
    }
    if(ch == quote) {
      T = new Token(TokenType.STRING, sb.toString(), l, c);
    } else {
      T = new Token(TokenType.ERROR, sb.toString(), line, column);
    }
    nextChar();
    return T;
  }

  /*
    number = [ "-" ] int [ frac ] [ exp ].
    exp = ("e" | "E") [ "-" | "+" ] digit {digit}.
    frac = "." digit {digit}.
    int = "0" |  digit19 {digit}.
    digit = "0" | "1" | ... | "9".
    digit19 = "1" | ... | "9".
  */
  private Token consumeNumber() {
    StringBuilder sb = new StringBuilder();
    int l = line;
    int c = column;
    Token T;
    boolean err;
    boolean isDouble = false;
    if(ch == '-') {
      sb.append(ch);
      nextChar();
    }
    if(ch != '0') {
      err = consumeDigits(sb);
      if(err) {
        return new Token(TokenType.ERROR, sb.toString(), line, column);
      }
    } else {
      sb.append(ch);
      nextChar();
    }
    if(ch == '.') {
      isDouble = true;
      sb.append(ch);
      nextChar();
      err = consumeDigits(sb);
      if(err) {
        return new Token(TokenType.ERROR, sb.toString(), line, column);
      }
    }
    if(ch == 'E' || ch == 'e') {
      isDouble = true;
      sb.append(ch);
      nextChar();
      if(ch == '-') {
        sb.append(ch);
        nextChar();
      } else if(ch == '+') {
        sb.append(ch);
        nextChar();
      }
      err = consumeDigits(sb);
      if(err) {
        return new Token(TokenType.ERROR, sb.toString(), line, column);
      }
    }
    if(isDouble) {
      T = new Token(TokenType.DOUBLE, sb.toString(), l, c);
    } else {
      T = new Token(TokenType.INTEGER, sb.toString(), l, c);
    }
    return T;
  }

  private boolean consumeDigits(StringBuilder sb) {
    boolean err = false;
    if(isDigit(ch)) {
      while(moreChars() && isDigit(ch)) {
        sb.append(ch);
        nextChar();
      }
    } else {
      err = true;
    }
    return err;
  }

  private boolean isDigit(char ch) {
    return ch >= '0' && ch <= '9';
  }

  private boolean isHexDigit(char ch) {
    return ch >= '0' && ch <= '9' || ch >= 'A' && ch <= 'F' || ch >= 'a' && ch <= 'f';
  }

  private Token consumeConstant() {
    StringBuilder sb = new StringBuilder();
    Token T;
    int l = line;
    int c = column;
    do {
      sb.append(ch);
      nextChar();
    } while(moreChars() && (ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z'));
    String str = sb.toString();
    TokenType type = Token.constants.get(str);
    if(type == null) {
      T = new Token(TokenType.ERROR, str, l, c);
    } else {
      T = new Token(type, str, l, c);
    }
    return T;
  }

  private void eatWhiteSpace() {
    while(moreChars() && (ch == '\t' || ch == '\n' || ch == '\r' || ch == ' ')) {
      nextChar();
    }
  }

  private void nextChar() {
    int c;

    try {
      c = source.read();
      offset++;
    } catch (IOException e) {
      c = -1;
    }
    if(c == '\n') {
      column = 0;
      line++;
    }
    else if(c != -1) {
      column++;
    } else {
      c = '\0';
    }
    ch = (char)c;
  }

  private boolean moreChars() {
    return ch != '\0';
  }
}
