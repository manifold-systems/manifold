package manifold.api.json;


import java.net.URL;
import java.util.List;
import javax.script.Bindings;
import javax.script.ScriptException;
import manifold.api.json.schema.JsonSchemaTransformer;
import manifold.util.Pair;
import manifold.util.concurrent.LocklessLazyVar;

/**
 */
public class Json
{
  private static String _parser = System.getProperty( "gosu.json.parser" );

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
   * @return A javax.script.Bindings instance
   */
  @SuppressWarnings("UnusedDeclaration")
  public static Bindings fromJson( String json )
  {
    return fromJson( json, false, false );
  }
  public static Bindings fromJson( String json, boolean withBigNumbers, boolean withTokens )
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
  public static String makeStructureTypes( String nameForStructure, Bindings bindings, boolean mutable )
  {
    JsonStructureType type = (JsonStructureType)transformJsonObject( nameForStructure, null, bindings );
    StringBuilder sb = new StringBuilder();
    type.render( sb, 0, mutable );
    return sb.toString();
  }

  public static IJsonType transformJsonObject( String name, JsonSchemaType parent, Object jsonObj )
  {
    return transformJsonObject( name, null, parent, jsonObj );
  }
  public static IJsonType transformJsonObject( String name, URL source, JsonSchemaType parent, Object jsonObj )
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
        type = JsonSchemaTransformer.transform( name, source, (Bindings)jsonObj );
      }
      else
      {
        if( type == null )
        {
          type = new JsonStructureType( parent, name );
          ((JsonSchemaType)type).setFile( source );
        }
        for( Object k : ((Bindings)jsonObj).keySet() )
        {
          String key = (String)k;
          Object value = ((Bindings)jsonObj).get( key );
          Token token = null;
          if( value instanceof Pair )
          {
            token = (Token)((Pair)value).getFirst();
          }
          IJsonType memberType = transformJsonObject( key, (JsonSchemaType)type, value );
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
      if( type == null )
      {
        type = new JsonListType( name, parent );
        ((JsonListType)type).setFile( source );
      }
      IJsonType compType = ((JsonListType)type).getComponentType();
      if( !((List)jsonObj).isEmpty() )
      {
        for( Object elem : (List)jsonObj )
        {
          IJsonType csr = transformJsonObject( name, (JsonSchemaType)type, elem );
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
      type = JsonSimpleType.get( jsonObj );
    }
    return type;
  }

  public static IJsonType mergeTypes( IJsonType type1, IJsonType type2 )
  {
    if( type1 == null && type2 != null )
    {
      return type2;
    }

    if( type2 == null && type1 != null )
    {
      return type1;
    }

    if( type1.equals( type2 ) )
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

    IJsonType mergedType = null;

    if( type1 instanceof JsonSimpleType && type2 instanceof JsonSimpleType )
    {
      mergedType = ((JsonSimpleType)type1).merge( (JsonSimpleType)type2 );
    }

    if( type1 instanceof JsonStructureType && type2 instanceof JsonStructureType )
    {
      mergedType = ((JsonStructureType)type1).merge( (JsonStructureType)type2 );
    }

    if( type1 instanceof JsonListType && type2 instanceof JsonListType )
    {
      mergedType = ((JsonListType)type1).merge( (JsonListType)type2 );
    }

    if( mergedType != null )
    {
      return mergedType;
    }

    // if the existing type is dynamic, override it with a more specific type,
    // otherwise the types disagree...
    throw new RuntimeException( "Incompatible types: " + type1.getIdentifier() + " vs: " + type2.getIdentifier() );
  }
}
