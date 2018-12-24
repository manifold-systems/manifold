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

package manifold.templates.tokenizer;


public class Token
{
  public enum TokenType
  {
    CONTENT,
    COMMENT,
    EXPR,
    STMT,
    DIRECTIVE,
    EXPR_BRACE_BEGIN( "${" ),
    EXPR_BRACE_END( "}" ),
    EXPR_ANGLE_BEGIN( "<%=" ),
    STMT_ANGLE_BEGIN( "<%" ),
    DIR_ANGLE_BEGIN( "<%@" ),
    ANGLE_END( "%>" ),
    COMMENT_BEGIN( "<%--" ),
    COMMENT_END( "--%>" );

    private String _staticToken;

    TokenType()
    {
    }

    TokenType( String staticToken )
    {
      _staticToken = staticToken;
    }

    public String getToken()
    {
      return _staticToken;
    }

  }

  private TokenType _type;
  private int _offset;
  private String _value;
  private int _line;
  private int _column;

  Token( TokenType type, int offset, String value, int line, int column )
  {
    _type = type;
    _offset = offset;
    _value = value;
    _line = line;
    _column = column;
  }

  public TokenType getType()
  {
    return _type;
  }

  public String getText()
  {
    return _value;
  }

  public int getOffset()
  {
    return _offset;
  }

  public int getLine()
  {
    return _line;
  }

  public int getColumn()
  {
    return _column;
  }
}
