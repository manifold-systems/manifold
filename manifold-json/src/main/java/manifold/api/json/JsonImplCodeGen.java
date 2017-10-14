package manifold.api.json;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcConstructor;
import manifold.api.gen.SrcGetProperty;
import manifold.api.gen.SrcRawStatement;
import manifold.api.gen.SrcSetProperty;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.type.SourcePosition;
import manifold.util.JsonUtil;

/**
 */
public class JsonImplCodeGen
{
  private final String _fqn;
  private final JsonStructureType _model;

  JsonImplCodeGen( JsonStructureType model, String topLevelFqn )
  {
    _model = model;
    _fqn = topLevelFqn;
  }

  public SrcClass make()
  {
    return genClass( _fqn, _model );
    //return genClass( _fqn, _fqn.substring( 0, _fqn.length() - JsonImplTypeManifold.IMPL_SUFFIX.length() ), _model );
  }

  private SrcClass genClass( String fqnImpl, JsonStructureType model )
  {
    SrcClass srcClass = new SrcClass( fqnImpl, SrcClass.Kind.Class )
      .modifiers( !fqnImpl.equals( _fqn ) ? Modifier.STATIC : 0 )
      .imports( Bindings.class )
      .imports( SimpleBindings.class )
      .imports( IJsonIO.class )
      .imports( List.class )
      .imports( SourcePosition.class )
      //.iface( fqnIface )
      .superClass( JsonImplBase.class );
    addConstructors( srcClass );
    addProperties( srcClass, model );
    addInnerTypes( srcClass, model.getInnerTypes() );
    return srcClass;
  }

  private void addConstructors( SrcClass srcClass )
  {
    srcClass.addConstructor( new SrcConstructor()
                               .body( new SrcStatementBlock()
                                        .addStatement( new SrcRawStatement()
                                                         .rawText( "super();" ) ) ) );
    srcClass.addConstructor( new SrcConstructor()
                               .addParam( "bindings", Bindings.class.getSimpleName() )
                               .body( new SrcStatementBlock()
                                        .addStatement( new SrcRawStatement()
                                                         .rawText( "super(bindings);" ) ) ) );
  }

  private void addInnerTypes( SrcClass srcClass, Map<String, IJsonParentType> innerClasses )
  {
    for( Map.Entry<String, IJsonParentType> e : innerClasses.entrySet() )
    {
      IJsonParentType type = e.getValue();
      if( type instanceof JsonStructureType )
      {
        String simpleInnerIface = e.getValue().getIdentifier();

        String fqnInnerImpl = srcClass.getPackage() + '.' + srcClass.getSimpleName() + '.' + simpleInnerIface;

        srcClass.addInnerClass( genClass( fqnInnerImpl, (JsonStructureType)type ) );
      }
      else if( type instanceof JsonListType )
      {
        addInnerTypes( srcClass, ((JsonListType)type).getInnerTypes() );
      }
    }
  }

  private void addProperties( SrcClass srcClass, JsonStructureType model )
  {
    Map<String, IJsonType> members = model.getMembers();
    for( Map.Entry<String, IJsonType> e : members.entrySet() )
    {
      String key = e.getKey();
      String identifier = makePropertyName( key );
      IJsonType type = e.getValue();
      String implType = type.getIdentifier();
      if( type instanceof JsonStructureType )
      {
        srcClass
          .addGetProperty( new SrcGetProperty( identifier, implType )
                             .body( new SrcStatementBlock()
                                      .addStatement( new SrcRawStatement().rawText( "return new " + implType + "((" + Bindings.class.getSimpleName() + ")_bindings.get(\"" + key + "\"));" ) ) ) )
          .addSetProperty( new SrcSetProperty( identifier, Object.class.getSimpleName() ) // Object to be structurally assignable to the none impl structures
                             .body( new SrcStatementBlock()
                                      .addStatement( new SrcRawStatement().rawText( "_bindings.put(\"" + key + "\", getBindings(" + SrcSetProperty.VALUE_PARAM + "));" ) ) ) );

      }
      else
      {
        IJsonType componentType = getComponentTypeSimple( type );
        if( type instanceof JsonListType && componentType instanceof JsonStructureType )
        {
          srcClass
            .addGetProperty( new SrcGetProperty( identifier, implType )
                               .body( new SrcStatementBlock()
                                        .addStatement( new SrcRawStatement().rawText( "return wrapList((List)_bindings.get(\"" + key + "\"), b ->  new " + componentType.getIdentifier() + "(b));" ) ) ) )
            .addSetProperty( new SrcSetProperty( identifier, List.class.getSimpleName() ) // Just List to be structurally assignable to the none impl structures
                               .body( new SrcStatementBlock()
                                        .addStatement( new SrcRawStatement().rawText( "_bindings.put(\"" + key + "\", unwrapList(" + SrcSetProperty.VALUE_PARAM + "));" ) ) ) );

        }
        else
        {
          if( type == DynamicType.instance() )
          {
            // No dynamic type, this is Java...  ¯\_(ツ)_/¯
            implType = Object.class.getSimpleName();
          }

          srcClass
            .addGetProperty( new SrcGetProperty( identifier, implType )
                               .body( new SrcStatementBlock()
                                        .addStatement( new SrcRawStatement().rawText( "return (" + implType + ")_bindings.get(\"" + key + "\");" ) ) ) )
            .addSetProperty( new SrcSetProperty( identifier, implType )
                               .body( new SrcStatementBlock()
                                        .addStatement( new SrcRawStatement().rawText( "_bindings.put(\"" + key + "\", " + SrcSetProperty.VALUE_PARAM + ");" ) ) ) );

        }
      }
    }
  }

  private String makePropertyName( String key )
  {
    StringBuilder name = new StringBuilder( JsonUtil.makeIdentifier( key ) );
    char firstChar = name.charAt( 0 );
    if( Character.isLowerCase( firstChar ) )
    {
      name.setCharAt( 0, Character.toUpperCase( firstChar ) );
    }
    return name.toString();
  }

  private IJsonType getComponentTypeSimple( IJsonType type )
  {
    if( type instanceof JsonListType )
    {
      IJsonType componentType = ((JsonListType)type).getComponentType();
      if( componentType instanceof JsonSimpleType )
      {
        return componentType;
      }

      return getComponentTypeSimple( componentType );
    }
    return type;
  }
}