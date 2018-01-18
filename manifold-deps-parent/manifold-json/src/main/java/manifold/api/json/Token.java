package manifold.api.json;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Token
{
  static final Map<String, TokenType> Constants = initializeConstants();

  private final TokenType _type;
  private final String _string;
  private final int _offset;
  private final int _line;
  private final int _column;

  Token( TokenType type, String string, int offset, int line, int column )
  {
    _type = type;
    _string = string;
    _offset = offset;
    _line = line;
    _column = column;
  }

  private static Map<String, TokenType> initializeConstants()
  {
    HashMap<String, TokenType> map = new HashMap<>();
    map.put( "true", TokenType.TRUE );
    map.put( "false", TokenType.FALSE );
    map.put( "null", TokenType.NULL );
    return Collections.unmodifiableMap( map );
  }

  public String getString()
  {
    return _string;
  }

  public TokenType getType()
  {
    return _type;
  }

  public int getOffset()
  {
    return _offset;
  }

  int getLineNumber()
  {
    return _line;
  }

  int getColumn()
  {
    return _column;
  }

  @Override
  public String toString()
  {
    return _string + " : " + _type;
  }

  boolean isValueType()
  {
    return _type == TokenType.LCURLY || _type == TokenType.LSQUARE ||
           _type == TokenType.INTEGER || _type == TokenType.DOUBLE ||
           _type == TokenType.STRING || _type == TokenType.TRUE ||
           _type == TokenType.FALSE || _type == TokenType.NULL;
  }
}
