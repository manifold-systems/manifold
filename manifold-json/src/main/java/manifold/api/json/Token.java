package manifold.api.json;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

final class Token
{
  private final TokenType type;
  private final String string;
  private final int line;
  private final int column;
  static final Map<String, TokenType> constants = initializeConstants();

  public Token( TokenType type, String string, int line, int column )
  {
    this.type = type;
    this.string = string;
    this.line = line;
    this.column = column;
  }

  private static Map<String, TokenType> initializeConstants()
  {
    HashMap<String, TokenType> map = new HashMap<String, TokenType>();
    map.put( "true", TokenType.TRUE );
    map.put( "false", TokenType.FALSE );
    map.put( "null", TokenType.NULL );
    return Collections.unmodifiableMap( map );
  }

  public String getString()
  {
    return string;
  }

  public TokenType getType()
  {
    return type;
  }

  public int getLineNumber()
  {
    return line;
  }

  public int getColumn()
  {
    return column;
  }

  @Override
  public String toString()
  {
    return string + " : " + type;
  }

  public boolean isValueType()
  {
    return type == TokenType.LCURLY || type == TokenType.LSQUARE ||
           type == TokenType.INTEGER || type == TokenType.DOUBLE ||
           type == TokenType.STRING || type == TokenType.TRUE ||
           type == TokenType.FALSE || type == TokenType.NULL;
  }
}
