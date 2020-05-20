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

package manifold.json.rt.parser;

import java.io.StringReader;
import java.util.List;
import manifold.rt.api.ScriptException;

import manifold.json.rt.api.IJsonParser;
import manifold.rt.api.util.Pair;

public class DefaultParser implements IJsonParser
{
  private static final DefaultParser INSTANCE = new DefaultParser();

  public static IJsonParser instance()
  {
    return INSTANCE;
  }

  @Override
  public Object parseJson( String jsonText, boolean withBigNumbers, boolean withTokens ) throws ScriptException
  {
    SimpleParserImpl parser = new SimpleParserImpl( new Tokenizer( new StringReader( jsonText ) ), withBigNumbers );
    Object result = parser.parse( withTokens );
    List<String> errors = parser.getErrors();
    if( errors.size() != 0 )
    {
      StringBuilder sb = new StringBuilder( "Found errors:\n" );
      for( String err: errors )
      {
        sb.append( err ).append( "\n" );
      }
      throw new ScriptException( sb.toString() );
    }
    if( result instanceof Pair )
    {
      result = ((Pair)result).getSecond();
    }
    return result;
  }
}
