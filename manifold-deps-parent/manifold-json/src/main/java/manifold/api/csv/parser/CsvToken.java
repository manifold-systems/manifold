/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.csv.parser;

public class CsvToken
{
  enum Type {Quoted, NotQuoted}

  private final Type _type;
  private final String _value;
  private final int _offset;
  private final int _line;
  private final int _tokenLength;
  private final int _separatorPos;
  private final char _separatorChar;

  public CsvToken( Type type, String value, int line, int offset, int length, int separatorPos, char separatorChar )
  {
    _type = type;
    _value = value;
    _line = line;
    _offset = offset;
    _tokenLength = length;
    _separatorPos = separatorPos;
    _separatorChar = separatorChar;
    verifyQuotes();
    verifyLength();
  }

  private void verifyLength()
  {
    if( getTokenLength() < _value.length() )
    {
      throw new IllegalStateException( "Token length < value length" );
    }
  }

  private void verifyQuotes()
  {
    if( getType() == Type.Quoted )
    {
      if( _value.charAt( 0 ) != '"' || _value.charAt( _value.length()-1 ) != '"' )
      {
        throw new IllegalStateException( "Quoted value missing quote: " + _value );
      }
    }
  }

  public Type getType()
  {
    return _type;
  }

  public String getValue()
  {
    return _value;
  }

  public String getData()
  {
    return getType() == Type.Quoted
           ? getValue().substring( 1, getValue().length() - 1 )
           : getValue();
  }

  public int getOffset()
  {
    return _offset;
  }

  public int getTokenLength()
  {
    return _tokenLength;
  }

  public int getLine()
  {
    return _line;
  }

  public int getSeparatorPos()
  {
    return _separatorPos;
  }

  public char getSeparatorChar()
  {
    return _separatorChar;
  }

  public boolean isEmpty()
  {
    return getData().isEmpty();
  }

  public boolean isLastInRecord()
  {
    return _separatorChar == '\n' || _separatorChar == '\0';
  }

  public boolean isEof()
  {
    return _separatorChar == '\0';
  }
}
