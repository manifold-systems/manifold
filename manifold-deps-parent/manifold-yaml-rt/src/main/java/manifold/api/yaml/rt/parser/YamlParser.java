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

package manifold.api.yaml.rt.parser;

import java.util.List;
import java.util.Map;
import manifold.json.rt.api.DataBindings;
import manifold.json.rt.parser.Token;
import manifold.json.rt.parser.TokenType;
import manifold.rt.api.util.Pair;
import manifold.util.ReflectUtil;
import org.snakeyaml.engine.v1.api.Load;
import org.snakeyaml.engine.v1.api.LoadSettings;
import org.snakeyaml.engine.v1.api.LoadSettingsBuilder;
import org.snakeyaml.engine.v1.constructor.BaseConstructor;
import org.snakeyaml.engine.v1.constructor.StandardConstructor;
import org.snakeyaml.engine.v1.exceptions.ConstructorException;
import org.snakeyaml.engine.v1.exceptions.Mark;
import org.snakeyaml.engine.v1.nodes.MappingNode;
import org.snakeyaml.engine.v1.nodes.Node;
import org.snakeyaml.engine.v1.nodes.NodeTuple;

/**
 * Parses YAML formatted text using snakeyaml
 */
public class YamlParser
{
  public static Object parseYaml( String yaml, boolean withTokens )
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
