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

package manifold.api.json;

import java.util.List;
import java.util.Map;
import javax.script.ScriptException;
import manifold.ext.DataBindings;
import manifold.api.util.Pair;
import manifold.util.ReflectUtil;
import org.snakeyaml.engine.v1.api.Dump;
import org.snakeyaml.engine.v1.api.DumpSettings;
import org.snakeyaml.engine.v1.api.DumpSettingsBuilder;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;
import org.snakeyaml.engine.v1.api.StreamDataWriter;
import org.snakeyaml.engine.v1.common.FlowStyle;
import org.snakeyaml.engine.v1.constructor.BaseConstructor;
import org.snakeyaml.engine.v1.constructor.StandardConstructor;
import org.snakeyaml.engine.v1.exceptions.ConstructorException;
import org.snakeyaml.engine.v1.exceptions.Mark;
import org.snakeyaml.engine.v1.exceptions.MarkedYamlEngineException;
import org.snakeyaml.engine.v1.nodes.MappingNode;
import org.snakeyaml.engine.v1.nodes.Node;
import org.snakeyaml.engine.v1.nodes.NodeTuple;

public class Yaml
{
  /**
   * Parse the YAML string as a javax.script.Bindings instance.
   *
   * @param yaml A Standard YAML 1.2 formatted string
   *
   * @return A javax.script.Bindings instance
   */
  @SuppressWarnings("UnusedDeclaration")
  public static Object fromYaml( String yaml )
  {
    return fromYaml( yaml, false, false );
  }

  public static Object fromYaml( String yaml, boolean withBigNumbers, boolean withTokens )
  {
    try
    {
      return parseYaml( yaml, withBigNumbers, withTokens );
    }
    catch( ScriptException e )
    {
      throw new RuntimeException( e );
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

  private static Object parseYaml( String yaml, boolean withBigNumbers, boolean withTokens ) throws ScriptException
  {
    LoadSettings loadSettings = new LoadSettingsBuilder()
      .setUseMarks( true )
      .setDefaultMap( DataBindings::new )
      .build();
    Load load = new Load( loadSettings, new MyConstructor( loadSettings, withTokens ) );
    return load.loadFromString( yaml );
  }

  private static class MyConstructor extends StandardConstructor
  {
    private final boolean _withTokens;

    MyConstructor( LoadSettings settings, boolean withTokens )
    {
      super( settings );
      _withTokens = withTokens;
    }

    protected void constructMapping2ndStep( MappingNode node, Map<Object, Object> mapping )
    {
      if( !_withTokens )
      {
        super.constructMapping2ndStep( node, mapping );
        return;
      }

      flattenMapping( node );

      List<NodeTuple> nodeValue = node.getValue();
      for( NodeTuple tuple: nodeValue )
      {
        Node keyNode = tuple.getKeyNode();
        Node valueNode = tuple.getValueNode();
        Object key = constructObject( keyNode );
        if( key != null )
        {
          try
          {
            key.hashCode();// check circular dependencies
          }
          catch( Exception e )
          {
            throw new ConstructorException( "while constructing a mapping",
              node.getStartMark(), "found unacceptable key " + key,
              tuple.getKeyNode().getStartMark(), e );
          }
        }
        Object value = constructObject( valueNode );

        value = makeTokensValue( keyNode, valueNode, value );

        if( keyNode.isRecursive() )
        {
          /*
           * See super.constructMapping2ndStep() for an explanation, basically the key object could be in fl
           * ux,
           * thus the hashcode could change after entry into the map, therefore the delayed entry.
           */
          String inner = "RecursiveTuple";
          inner = '$' + inner;
          ReflectUtil.ConstructorRef constructor = ReflectUtil.constructor(
            BaseConstructor.class.getTypeName() + inner, Object.class, Object.class );
          Object recursiveTuple = constructor.newInstance( key, value );
          Object element = constructor.newInstance( mapping, recursiveTuple );
          List maps2fill = this.jailbreak().maps2fill;
          maps2fill.add( 0, element );
        }
        else
        {
          mapping.put( key, value );
        }
      }

    }

    private Object makeTokensValue( Node keyNode, Node valueNode, Object value )
    {
      Token keyToken = makeToken( keyNode );
      if( keyToken != null )
      {
        Token valueToken = makeToken( valueNode );
        if( valueToken != null )
        {
          value = new Pair<>( new Token[]{keyToken, valueToken}, value );
        }
      }
      return value;
    }

    private Token makeToken( Node node )
    {
      if( node.getStartMark().isPresent() )
      {
        Mark mark = node.getStartMark().get();
        return new Token( TokenType.STRING, node.getTag().getValue(), mark.getIndex(), mark.getLine(), mark.getColumn() );
      }
      return null;
    }
  }
}
