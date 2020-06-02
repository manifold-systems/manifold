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

package manifold.api.yaml.rt;

import manifold.rt.api.ScriptException;
import manifold.api.yaml.rt.parser.YamlParser;
import org.snakeyaml.engine.v1.api.Dump;
import org.snakeyaml.engine.v1.api.DumpSettings;
import org.snakeyaml.engine.v1.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v1.api.StreamDataWriter;
import org.snakeyaml.engine.v1.common.FlowStyle;
import org.snakeyaml.engine.v1.exceptions.Mark;
import org.snakeyaml.engine.v1.exceptions.MarkedYamlEngineException;

public class Yaml
{
  /**
   * Parse the YAML string as a manifold.rt.api.Bindings instance.
   *
   * @param yaml A Standard YAML 1.2 formatted string
   *
   * @return A manifold.rt.api.Bindings instance
   */
  @SuppressWarnings("UnusedDeclaration")
  public static Object fromYaml( String yaml )
  {
    return fromYaml( yaml, false, false );
  }

  public static Object fromYaml( String yaml, @SuppressWarnings( "unused" ) boolean withBigNumbers, boolean withTokens )
  {
    try
    {
      return YamlParser.parseYaml( yaml, withTokens );
    }
    catch( MarkedYamlEngineException me )
    {
      Mark mark = me.getContextMark().isPresent() ? me.getContextMark().get() : null;
      throw new RuntimeException(
        new ScriptException( me.getMessage(),
          null, mark == null ? 0 : mark.getLine(), mark == null ? 0 : mark.getColumn() ) );
    }
  }

  /**
   * Serializes a JSON value to a YAML 1.2 formatted StringBuilder {@code target}
   * with the specified {@code indent} of spaces.
   *
   * @param target A {@link StringBuilder} to write the YAML in
   */
  public static void toYaml( Object jsonValue, StringBuilder target )
  {
    DumpSettings settings = new DumpSettingsBuilder()
      .setBestLineBreak( "\n" )
      .setMultiLineFlow( true )
      .setDefaultFlowStyle( FlowStyle.BLOCK )
      .setIndent( 2 )
      .build();
    new Dump( settings ).dump( jsonValue,
      new StreamDataWriter()
      {
        @Override
        public void write( String str )
        {
          target.append( str );
        }

        @Override
        public void write( String str, int offset, int length )
        {
          target.append( str, offset, offset + length );
        }
      } );
  }
}
