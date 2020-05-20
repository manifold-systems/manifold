/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.js.rt.parser;

public class Token
{
  private int _lineNumber;
  private int _col;
  private int _offset;
  private TokenType _type;
  private String _val;
  private String _errorMsg;

  public Token( TokenType type, String val )
  {
    _type = type;
    _val = val;
  }

  public Token( TokenType type, String val, String errorMsg )
  {
    _type = type;
    _val = val;
    _errorMsg = errorMsg;
  }

  public Token( TokenType type, String val, int lineNumber, int col, int offset )
  {
    _type = type;
    _val = val;
    _lineNumber = lineNumber;
    _col = col;
    _offset = offset;
  }

  public TokenType getType()
  {
    return _type;
  }

  public String getValue()
  {
    return _val;
  }

  public String getErrorMsg()
  {
    return _errorMsg;
  }

  @Override
  public boolean equals( Object obj )
  {
    if( !(obj instanceof Token) )
    {
      return false;
    }
    Token token = (Token)obj;
    return _type == token.getType() && _val.equals( token.getValue() );
  }

  @Override
  public String toString()
  {
    return String.format( "type: %s val: %s pos: %d:%d:%d\n", _type, _val, _lineNumber, _col, _offset );
  }

  public int getLineNumber()
  {
    return _lineNumber;
  }

  public int getOffset()
  {
    return _offset;
  }

  public int getCol()
  {
    return _col;
  }
}
