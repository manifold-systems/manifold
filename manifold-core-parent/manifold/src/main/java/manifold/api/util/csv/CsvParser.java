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

package manifold.api.util.csv;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import manifold.api.util.StreamUtil;

public class CsvParser
{
  private final CsvTokenizer _tokenizer;

  public static CsvDataSet parse( InputStream inputStream )
  {
    return new CsvParser( inputStream ).parse();
  }

  private CsvParser( InputStream inputStream )
  {
    try
    {
      String content = StreamUtil.getContent( new InputStreamReader( inputStream ) );
      _tokenizer = new CsvTokenizer( content );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private CsvDataSet parse()
  {
    return new CsvDataSet( parseHeader(), parseRecords(), _tokenizer.getTypes() );
  }

  private List<CsvRecord> parseRecords()
  {
    List<CsvRecord> records = new ArrayList<>();
    List<CsvField> fields = new ArrayList<>();
    while( true )
    {
      CsvToken token = _tokenizer.nextToken();
      fields.add( new CsvField( token ) );
      if( token.isLastInRecord() )
      {
        records.add( new CsvRecord( fields ) );
        if( token.isEof() )
        {
          break;
        }
        else
        {
          fields = new ArrayList<>();
        }
      }
    }
    return records;
  }

  private CsvHeader parseHeader()
  {
    if( _tokenizer.hasHeader() )
    {
      List<CsvField> fields = new ArrayList<>();
      while( true )
      {
        CsvToken token = _tokenizer.nextToken();
        fields.add( new CsvField( token ) );
        if( token.isLastInRecord() )
        {
          return new CsvHeader( fields );
        }
      }
    }
    return null;
  }
}
