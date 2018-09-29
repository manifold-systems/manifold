package manifold.api.json.schema;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import manifold.api.json.DynamicType;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.Json;
import manifold.api.json.JsonStructureType;

/**
 */
public class JsonUnionType extends JsonStructureType
{
  private Map<String, IJsonType> _constituentTypes;

  public JsonUnionType( JsonSchemaType parent, URL source, String name )
  {
    super( parent, source, name );
    _constituentTypes = Collections.emptyMap();
  }

  @Override
  protected void resolveRefsImpl()
  {
    super.resolveRefsImpl();
    for( Map.Entry<String, IJsonType> entry: new HashSet<>( _constituentTypes.entrySet() ) )
    {
      IJsonType type = entry.getValue();
      if( type instanceof JsonSchemaType )
      {
        ((JsonSchemaType)type).resolveRefs();
      }
      else if( type instanceof LazyRefJsonType )
      {
        type = ((LazyRefJsonType)type).resolve();
        _constituentTypes.put( entry.getKey(), type );
      }
    }
  }

  public Collection<? extends IJsonType> getConstituents()
  {
    return _constituentTypes.values();
  }

  public void addConstituent( String name, IJsonType type )
  {
    if( _constituentTypes.isEmpty() )
    {
      _constituentTypes = new HashMap<>();
    }
    _constituentTypes.put( name, type );
    if( type instanceof IJsonParentType && !isDefinition( type ) )
    {
      super.addChild( name, (IJsonParentType)type );
    }
  }

  private boolean isDefinition( IJsonType type )
  {
    return type.getParent() != null &&
           type.getParent().getName().equals( JsonSchemaTransformer.JSCH_DEFINITIONS );
  }

  public IJsonType merge( IJsonType type )
  {
    IJsonType mergedType = null;
    for( IJsonType c: getConstituents() )
    {
      mergedType = Json.mergeTypesNoUnion( c, type );
      if( mergedType != null && mergedType != DynamicType.instance() )
      {
        break;
      }
    }

    if( mergedType == null )
    {
      mergedType = type;
    }
    addConstituent( mergedType.getName(), mergedType );
    return this;
  }
}
