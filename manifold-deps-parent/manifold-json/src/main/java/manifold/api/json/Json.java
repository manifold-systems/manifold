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

package manifold.api.json;


import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptException;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;
import manifold.api.json.schema.JsonSchemaTransformer;
import manifold.api.json.schema.JsonSchemaTransformerSession;
import manifold.api.json.schema.JsonSchemaType;
import manifold.api.json.schema.JsonUnionType;
import manifold.api.json.schema.TypeAttributes;
import manifold.api.util.Pair;
import manifold.util.concurrent.LocklessLazyVar;

/**
 */
public class Json
{
  private static String _parser = System.getProperty( "manifold.json.parser" );

  public static String getParserName()
  {
    return _parser;
  }

  @SuppressWarnings("UnusedDeclaration")
  public static void setParserName( String fqn )
  {
    _parser = fqn;
    PARSER.clear();
  }

  private static final LocklessLazyVar<IJsonParser> PARSER =
    new LocklessLazyVar<IJsonParser>()
    {

      @Override
      protected IJsonParser init()
      {
        String fqn = getParserName();
        return fqn == null ? IJsonParser.getDefaultParser() : makeParser( fqn );
      }

      private IJsonParser makeParser( String fqn )
      {
        try
        {
          return (IJsonParser)Class.forName( fqn ).newInstance();
        }
        catch( Exception e )
        {
          throw new RuntimeException( e );
        }
      }
    };

  /**
   * Parse the JSON string as a javax.script.Bindings instance.
   *
   * @param json A Standard JSON formatted string
   *
   * @return A JSON value (primitive/boxed type, String, List of JSON values, or Bindings of String/JSON value)
   */
  @SuppressWarnings("UnusedDeclaration")
  public static Object fromJson( String json )
  {
    return fromJson( json, false, false );
  }
  public static Object fromJson( String json, boolean withBigNumbers, boolean withTokens )
  {
    try
    {
      return PARSER.get().parseJson( json, withBigNumbers, withTokens );
    }
    catch( ScriptException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Makes a tree of structure types reflecting the Bindings.
   * <p/>
   * If the bindings represent a Json Schema format (v.4), the types are transformed according to that schema.
   * Otherwise, the bindings are assumed to be a sample Json document whereby types are derived directly
   * from the sample.  Note when multiple samples exist for the same type eg., array elements, the samples
   * are merged and reconciled.
   * <p/>
   * A structure type contains a property member for each name/value pair in the Bindings.  A property has the same name as the key and follows these rules:
   * <ul>
   * <li> If the type of the value is a "simple" type, such as a String or Integer, the type of the property matches the simple type exactly
   * <li> Otherwise, if the value is a Bindings type, the property type is that of a child structure with the same name as the property and recursively follows these rules
   * <li> Otherwise, if the value is a List, the property is a List parameterized with the component type, and the component type recursively follows these rules
   * </ul>
   */
  @SuppressWarnings("unused")
  public static String makeStructureTypes( IManifoldHost host, String nameForStructure, Bindings bindings, AbstractJsonTypeManifold tm,
                                           boolean mutable )
  {
    JsonStructureType type = (JsonStructureType)transformJsonObject( host, nameForStructure, null, bindings );
    StringBuilder sb = new StringBuilder();
    type.render( tm, sb, 0, mutable );
    return sb.toString();
  }

  public static IJsonType transformJsonObject( IManifoldHost host, String name, JsonSchemaType parent, Object jsonObj )
  {
    return transformJsonObject( host, name, null, parent, jsonObj );
  }
  public static IJsonType transformJsonObject( IManifoldHost host, String name, IFile source, final JsonSchemaType parent, Object jsonObj )
  {
    IJsonType type = null;

    if( parent != null )
    {
      type = parent.findChild( name );
    }

    if( jsonObj instanceof Pair )
    {
      jsonObj = ((Pair)jsonObj).getSecond();
    }

    if( jsonObj == null )
    {
      return DynamicType.instance();
    }
    else if( jsonObj instanceof Bindings )
    {
      if( JsonSchemaTransformer.isSchema( (Bindings)jsonObj ) )
      {
        type = JsonSchemaTransformer.transform( host, name, source, jsonObj );
      }
      else
      {
        if( !(type instanceof JsonStructureType) )
        {
          // handle case for mixed array where component types are different (object and array)
          type = new JsonStructureType( parent, source, name, new TypeAttributes() );
        }
        for( Object k : ((Bindings)jsonObj).keySet() )
        {
          String key = (String)k;
          Object value = ((Bindings)jsonObj).get( key );
          Token token = null;
          if( value instanceof Pair )
          {
            token = ((Token[])((Pair)value).getFirst())[0];
          }
          IJsonType memberType = transformJsonObject( host, key, (JsonSchemaType)type, value );
          if( memberType != null )
          {
            ((JsonStructureType)type).addMember( key, memberType, token );
          }
        }
        if( parent != null )
        {
          parent.addChild( name, (IJsonParentType)type );
        }
      }
    }
    else if( jsonObj instanceof List )
    {
      if( !(type instanceof JsonListType) )
      {
        // handle case for mixed array where component types are different (object and array)
        type = new JsonListType( name, source, parent, new TypeAttributes() );
      }
      IJsonType compType = ((JsonListType)type).getComponentType();
      if( !((List)jsonObj).isEmpty() )
      {
        name += "Item";
        int i = 0;
        boolean isDissimilar = isDissimilar( (List)jsonObj );
        for( Object elem : (List)jsonObj )
        {
          IJsonType csr = transformJsonObject( host, name + (isDissimilar ? i++ : ""), (JsonSchemaType)type, elem );
          if( compType != null && csr != compType && compType != DynamicType.instance() )
          {
            csr = mergeTypes( compType, csr );
          }
          compType = csr;
        }
      }
      else if( compType == null )
      {
        // Empty list implies dynamic component type
        System.out.println( "\nWarning: there are no sample elements in list: " + name +
                            "\nThe component type for this list will be Dynamic.\n" );
        compType = DynamicType.instance();
      }
      ((JsonListType)type).setComponentType( compType );
      if( parent != null )
      {
        parent.addChild( name, (IJsonParentType)type );
      }
    }
    else
    {
      type = JsonBasicType.get( jsonObj );
    }
    if( parent == null && type instanceof JsonSchemaType )
    {
      ((JsonSchemaType)type).resolveRefs();
      JsonSchemaTransformerSession.instance().maybeClear();
    }
    return type;
  }

  private static boolean isDissimilar( List jsonObj )
  {
    Class type = null;
    for( Object o: jsonObj )
    {
      if( type == null )
      {
        type = o == null ? null : o.getClass();
      }
      else
      {
        Class csr = o == null ? null : o.getClass();
        if( csr != null && csr != type )
        {
          return true;
        }
        type = csr;
      }
    }
    return false;
  }

  public static IJsonType mergeTypes( IJsonType type1, IJsonType type2 )
  {
    IJsonType mergedType = mergeTypesNoUnion( type1, type2 );

    if( mergedType == null && type1.getParent() instanceof JsonListType )
    {
      JsonListType listType = (JsonListType)type1.getParent();
      JsonUnionType unionType = new JsonUnionType( listType, listType.getFile(), "UnionType", new TypeAttributes() );
      unionType.merge( type1 );
      unionType.merge( type2 );
      mergedType = unionType;
    }

    if( mergedType != null )
    {
      return mergedType;
    }

    // if the existing type is dynamic, override it with a more specific type,
    // otherwise the types disagree...
    throw new RuntimeException( "Incompatible types: " + type1.getIdentifier() + " vs: " +
                                (type2 instanceof JsonListType ? "Array: " : "") + type2.getIdentifier() );
  }

  public static IJsonType mergeTypesNoUnion( IJsonType type1, IJsonType type2 )
  {
    if( type1 == null )
    {
      return type2;
    }

    if( type2 == null )
    {
      return type1;
    }

    if( type1.equalsStructurally( type2 ) )
    {
      return type1;
    }

    if( type1 == DynamicType.instance() )
    {
      // Keep the more specific type (Dynamic type is inferred from a 'null', thus the more specific type wins)
      return type2;
    }

    if( type2 == DynamicType.instance() )
    {
      // Keep the more specific type
      return type1;
    }

    // merge the types
    IJsonType mergedType = type1.merge( type2 );
    if( mergedType == null )
    {
      mergedType = type2.merge( type1 );
    }

    return mergedType;
  }
}
