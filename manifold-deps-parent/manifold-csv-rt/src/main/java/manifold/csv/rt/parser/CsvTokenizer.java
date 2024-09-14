/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.csv.rt.parser;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import manifold.rt.api.util.ManDateTimeUtil;
import manifold.rt.api.util.ManStringUtil;

import static manifold.csv.rt.parser.CsvToken.Type.NotQuoted;
import static manifold.csv.rt.parser.CsvToken.Type.Quoted;


public class CsvTokenizer
{
  private static final char[] SEPARATORS = {',', ';', ':', '|', '\t'};

  private CharSequence _content;
  private Boolean _hasHeader;
  private char _separator;
  private boolean _indented;
  private boolean _whitespace; // leading/trailing whitespace significant?
  private List<Class> _types;
  private boolean _sampling;

  private int _length;
  private int _pos;
  private int _line;
  private CsvToken _prevToken;

  public CsvTokenizer( CharSequence content )
  {
    this( content, null );
  }

  public CsvTokenizer( CharSequence content, Boolean header )
  {
    _content = content;
    _length = content.length();
    _hasHeader = header;
    resetPos();
    sample();
  }

  private void resetPos()
  {
    _pos = -1;
    _line = 1;
    _prevToken = null;
  }

  public boolean hasHeader()
  {
    return _hasHeader;
  }

  public CsvToken nextToken()
  {
    char c = nextChar();

    // skip whitespace
    if( !_whitespace )
    {
      c = skipSpaces( c );
    }

    int offset = _pos;
    int line = _line;
    StringBuilder value = new StringBuilder();

    // is quoted?
    boolean quoted = false;

    while( true )
    {
      switch( c )
      {
        case '"':
          value.append( c );

          if( value.length() == 1 )
          {
            quoted = true;
          }
          else if( quoted )
          {
            c = nextChar();
            if( c != '"' )
            {
              int end = _pos;
              c = skipSpaces( c );
              if( c != _separator && c != '\n' && c != '\0' )
              {
                if( _sampling && isPossibleSeparator( c ) )
                {
                  // when sampling favor the probability that one of these chars is the actual separator
                  if( isEol() ) _line++;
                  return _prevToken = new CsvToken( Quoted, value.toString(), line, offset, end - offset, _pos, c );
                }

                // Assume NOT quoted because more data follows what would be a closing quote; let's be lenient and let
                // this value be non-quoted (as opposed to making this an error).
                //todo: add a warning here

                _pos = end - 1; // backtrack to quote
                quoted = false;
              }
              else
              {
                if( isEol() ) _line++;
                return _prevToken = new CsvToken( Quoted, value.toString(), line, offset, end - offset, _pos, c );
              }
            }
          }
          break;

        case ',':
        case ';':
        case ':':
        case '|':
        case '\t':
          if( quoted || c != _separator )
          {
            value.append( c );
          }
          else // separator
          {
            return _prevToken = new CsvToken( NotQuoted, value.toString(), line, offset, _pos - offset, _pos, c );
          }
          break;

        case '\n':
          _line++;
          if( quoted )
          {
            value.append( c );
          }
          else
          {
            int end = _pos == 0
                      ? 0
                      : _content.charAt( _pos - 1 ) == '\r'
                        ? _pos - 1
                        : _pos;
            int length = end > offset ? end - offset : 0;
            boolean emptyLine = length <= 0 && (_prevToken == null || _prevToken.isLastInRecord());
            if( !emptyLine )
            {
              return _prevToken = new CsvToken( NotQuoted, value.toString(), line, offset, length, _pos, c );
            }
          }
          break;

        case '\0':
          //todo: if quoted, add warning about missing terminal '"'
          return new CsvToken( quoted ? Quoted : NotQuoted, value.toString(), line, offset, _pos - offset, _pos, c );

        default:
          if( !skipFileIndentation( c ) )
          {
            value.append( c );
          }
      }

      c = nextChar();
    }

  }

  public boolean skipFileIndentation( char c )
  {
    return _indented && Character.isWhitespace( c ) && (_prevToken == null || _prevToken.isLastInRecord());
  }

  private boolean isPossibleSeparator( char c )
  {
    //noinspection ForLoopReplaceableByForEach
    for( int i = 0; i < SEPARATORS.length; i++ )
    {
      if( c == SEPARATORS[i] )
      {
        return true;
      }
    }
    return false;
  }

  private char skipSpaces( char c )
  {
    while( c == ' ' || (c == '\t' && c != _separator) )
    {
      c = nextChar();
    }
    return c;
  }

  private char skipToEofIfOnlyWhitespaceLeft( char c )
  {
    int savePos = _pos;
    char saveC = c;
    while( Character.isWhitespace( c ) )
    {
      c = _rawNextChar();
      if( c == '\0' ) // EOF
      {
        return c;
      }
    }
    _pos = savePos;
    return saveC;
  }

  private void sample()
  {
    _sampling = true;
    _separator = inferSeparator();
    resetPos();
    _indented = inferIndented();
    resetPos();
    _whitespace = inferRetainLeadingTrailingWhitespace();
    resetPos();
    _hasHeader = _hasHeader == null ? inferHeader() : _hasHeader;
    resetPos();
    _types = inferDataTypes();
    resetPos();
    _sampling = false;
  }

  private boolean inferIndented()
  {
    boolean saveWhitespace = _whitespace;
    _whitespace = true;

    boolean indented = true;
    int row = 0;
    boolean newline = true;
    while( row < 100 )
    {
      CsvToken token = nextToken();
      if( newline )
      {
        indented = indented && countLeadingSpaces( token ) > 0;
      }
      newline = false;
      if( token.isLastInRecord() )
      {
        if( token.isEof() )
        {
          break;
        }
        newline = true;
        row++;
      }
    }
    _whitespace = saveWhitespace;
    return indented;
  }

  private boolean inferHeader()
  {
    List<DataStats> header = new ArrayList<>();
    while( true )
    {
      CsvToken token = nextToken();
      if( token.isEmpty() )
      {
        // all header fields must be non-empty
        return false;
      }

      header.add( new DataStats( token ) );
      if( token.isLastInRecord() )
      {
        break;
      }
    }
    int fieldCount = 0;
    int diffCount = 0;
    int row = 0;
    int i = 0;
    while( row < 100 )
    {
      if( i == header.size() )
      {
        // more fields in data row than header row! bail
        return false;
      }

      CsvToken token = nextToken();
      DataStats stats = header.get( i );
      if( !token.isEmpty() )
      {
        // empty values are excluded from analysis

        fieldCount++;
        if( !stats.isSimilar( token ) )
        {
          diffCount++;
        }
      }

      if( token.isLastInRecord() )
      {
        boolean emptyLine = i == 0 && token.getValue().isEmpty();
        if( !emptyLine && i != header.size() - 1 )
        {
          // more fields in header row than data row! bail
          return false;
        }

        if( token.isEof() )
        {
          break;
        }
        row++;
        i = 0;
      }
      else
      {
        i++;
      }
    }
    return fieldCount != 0 && diffCount * 100 / fieldCount > 60;
  }

  private List<Class> inferDataTypes()
  {
    if( _hasHeader )
    {
      // skip header

      while( true )
      {
        CsvToken token = nextToken();
        if( token.isLastInRecord() )
        {
          break;
        }
      }
    }

    List<Class> types = new ArrayList<>();
    int row = 0;
    int i = 0;
    while( row < 1000 )
    {
      if( row > 0 && i == types.size() )
      {
        // more fields in data row than header row! bail
        return null;
      }

      CsvToken token = nextToken();
      if( row == 0 )
      {
        types.add( inferType( token.getData() ) );
      }
      else if( row <= 100 || row % 10 == 0 )
      {
        types.set( i, mergeDataType( token.getData(), types.get( i ) ) );
      }

      if( token.isLastInRecord() )
      {
        boolean emptyLine = i == 0 && token.getValue().isEmpty();
        if( !emptyLine && row > 0 && i != types.size() - 1 )
        {
          // more fields in header row than data row! bail
          return null;
        }

        if( token.isEof() )
        {
          break;
        }
        row++;
        i = 0;
      }
      else
      {
        i++;
      }
    }

    for( int t = 0; t < types.size(); t++ )
    {
       if( types.get( t ) == null )
       {
         // all null sample values for a column => String.class
         types.set( t, String.class );
       }
    }

    return types;
  }

  private Class mergeDataType( String data, Class existingType )
  {
    if( data.isEmpty() )
    {
      return mergeTypes( existingType, null );
    }
    Class inferredType = inferType( data );
    return mergeTypes( existingType, inferredType );
  }

  private Class mergeTypes( Class existingType, Class inferredType )
  {
    if( existingType == String.class )
    {
      // nothing merges with string
      return existingType;
    }

    if( inferredType == existingType || existingType == null )
    {
      return inferredType;
    }

    if( inferredType == null )
    {
      return existingType;
    }

    if( existingType == Boolean.class )
    {
      // nothing merges with boolean
      return String.class;
    }

    if( existingType == Integer.class )
    {
      if( inferredType == Long.class ||
          inferredType == Double.class ||
          inferredType == BigInteger.class ||
          inferredType == BigDecimal.class )
      {
        return inferredType;
      }
    }
    else if( existingType == Long.class )
    {
      if( inferredType == Integer.class )
      {
        return existingType;
      }
      if( inferredType == Double.class ||
          inferredType == BigInteger.class ||
          inferredType == BigDecimal.class )
      {
        return inferredType;
      }
    }
    else if( existingType == BigInteger.class )
    {
      if( inferredType == Integer.class ||
          inferredType == Long.class )
      {
        return existingType;
      }
      if( inferredType == Double.class )
      {
        return BigDecimal.class;
      }
      if( inferredType == BigDecimal.class )
      {
        return inferredType;
      }
    }
    else if( existingType == Double.class )
    {
      if( inferredType == Integer.class ||
          inferredType == Long.class )
      {
        return existingType;
      }
      if( inferredType == BigInteger.class )
      {
        return BigDecimal.class;
      }
      if( inferredType == BigDecimal.class )
      {
        return inferredType;
      }
    }
    else if( existingType == BigDecimal.class )
    {
      if( inferredType == Integer.class ||
          inferredType == Long.class ||
          inferredType == Double.class ||
          inferredType == BigInteger.class )
      {
        return existingType;
      }
    }
    return String.class;
  }

  private Class inferType( String data )
  {
    Class type;
    
    if( data.isEmpty() )
    {
      // empty data does not contribute toward type inference
      type = null;
    }
    else if( !ManStringUtil.isAlpha( data ) )
    {
      if( isInteger( data ) )
      {
        type = Integer.class;
      }
      else if( isLong( data ) )
      {
        type = Long.class;
      }
      else if( isBigInteger( data ) )
      {
        type = BigInteger.class;
      }
      else if( isDouble( data ) )
      {
        type = Double.class;
      }
      else if( isBigDecimal( data ) )
      {
        type = BigDecimal.class;
      }
      else if( isDateTime( data ) )
      {
        type = LocalDateTime.class;
      }
      else if( isDate( data ) )
      {
        type = LocalDate.class;
      }
      else if( isTime( data ) )
      {
        type = LocalTime.class;
      }
      else
      {
        type = String.class;
      }
    }
    else if( isBoolean( data ) )
    {
      type = Boolean.class;
    }
    else
    {
      type = String.class;
    }

    return type;
  }

  private boolean isDateTime( String data )
  {
    return null != ManDateTimeUtil.parseDateTime( data );
  }

  private boolean isDate( String data )
  {
    return null != ManDateTimeUtil.parseDate( data );
  }

  private boolean isTime( String data )
  {
    return null != ManDateTimeUtil.parseTime( data );
  }

  private boolean isInteger( String data )
  {
    try
    {
      Integer.parseInt( data );
      return true;
    }
    catch( Exception e )
    {
      return false;
    }
  }

  private boolean isLong( String data )
  {
    try
    {
      Long.parseLong( data );
      return true;
    }
    catch( Exception e )
    {
      return false;
    }
  }

  private boolean isBigInteger( String data )
  {
    try
    {
      new BigInteger( data );
      return true;
    }
    catch( Exception e )
    {
      return false;
    }
  }

  private boolean isDouble( String data )
  {
    try
    {
      Double.parseDouble( data );
      return true;
    }
    catch( Exception e )
    {
      return false;
    }
  }

  private boolean isBigDecimal( String data )
  {
    try
    {
      new BigDecimal( data );
      return true;
    }
    catch( Exception e )
    {
      return false;
    }
  }

  private boolean isBoolean( String data )
  {
    return "true".equalsIgnoreCase( data ) ||
           "false".equalsIgnoreCase( data ) ||
           "yes".equalsIgnoreCase( data ) ||
           "no".equalsIgnoreCase( data ) ||
           "on".equalsIgnoreCase( data ) ||
           "off".equalsIgnoreCase( data );
  }

  private boolean inferRetainLeadingTrailingWhitespace()
  {
    Map<Integer, Integer> spacesToOccurrence = new HashMap<>();
    mapSpacesToOccurrences( spacesToOccurrence );
    int[] best = {0, 0};
    spacesToOccurrence.forEach( ( key, value ) -> {
      if( value > best[1] )
      {
        best[0] = key;
        best[1] = value;
      }
    } );
    int total = spacesToOccurrence.values().size();
    if( total == 0 || best[1] * 100 / total > 80 )
    {
      return best[0] == 0;
    }
    return true;
  }

  private void mapSpacesToOccurrences( Map<Integer, Integer> spacesToOccurrence )
  {
    _whitespace = true;
    int row = 0;
    while( row < 100 )
    {
      CsvToken token = nextToken();
      int count = countLeadingSpaces( token );
      Integer existing = spacesToOccurrence.get( count );
      spacesToOccurrence.put( count, existing == null ? count : existing + 1 );

      if( token.isLastInRecord() )
      {
        if( token.isEof() )
        {
          break;
        }
        row++;
      }
    }
  }

  private int countLeadingSpaces( CsvToken token )
  {
    int spaces = 0;
    String value = token.getValue();
    for( int i = 0; i < value.length(); i++ )
    {
      char c = value.charAt( i );
      if( c == ' ' || c == '\t' )
      {
        spaces++;
      }
      else
      {
        break;
      }
    }
    return spaces;
  }

  private char inferSeparator()
  {
    char cMax = 0;
    int max = -1;
    for( char separator: SEPARATORS )
    {
      int result = sampleSeparator( separator );
      if( result > max )
      {
        max = result;
        cMax = separator;
      }
      resetPos();
    }
    if( max == -1 )
    {
      // none of the separators resulted in a consistent record size (rows had differing number of fields)
      // default to comma, but the file may parse with errors
      cMax = ',';
    }
    return cMax;
  }

  public boolean isEol()
  {
    if( _pos < 0 || isEof() )
    {
      return false;
    }
    if( _content.charAt( _pos ) == '\n' )
    {
      return true;
    }
    if( _content.charAt( _pos ) == '\r' )
    {
      return _length-1 == _pos || _content.charAt( _pos+1 ) != '\n';
    }
    return false;
  }

  public boolean isEof()
  {
    return _pos == _content.length();
  }

  private int sampleSeparator( char separator )
  {
    _separator = separator;
    int recordSize = 0;
    int count = 0;
    int row = 0;
    while( row < 10 )
    {
      count++;
      CsvToken token = nextToken();
      if( token.isLastInRecord() )
      {
        boolean emptyLine = count == 1 && token.getValue().isEmpty();
        if( !emptyLine )
        {
          if( recordSize == 0 )
          {
            recordSize = count;
          }
          if( count != recordSize )
          {
            return -1;
          }
          if( token.isEof() )
          {
            break;
          }
        }
        count = 0;
        row++;
      }
      else if( token.getSeparatorChar() != separator )
      {
        return -1;
      }
    }
    return recordSize;
  }

  private char nextChar()
  {
    char c = _rawNextChar();

    if( c == '\r' )
    {
      c = _rawNextChar();
      if( c != '\n' )
      {
        // always return '\n' as linebreak
        c = '\n';
        _pos--;
      }
    }

    if( c == '\n' )
    {
      c = skipToEofIfOnlyWhitespaceLeft( c );
    }

    return c;
  }

  private char _rawNextChar()
  {
    if( _pos < _length )
    {
      _pos++;
    }

    if( _pos == _length )
    {
      return '\0'; // EOF
    }

    if( _pos > _length )
    {
      throw new IllegalStateException( "position > length" );
    }

    return _content.charAt( _pos );
  }

  public List<Class> getTypes()
  {
    return _types;
  }
}
