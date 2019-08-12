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

package manifold.api.json.schema;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcArgument;
import manifold.api.gen.SrcMemberAccessExpression;
import manifold.api.json.AbstractJsonTypeManifold;
import manifold.api.json.IJsonList;
import manifold.api.json.IJsonParentType;
import manifold.api.json.IJsonType;
import manifold.api.json.Json;
import manifold.api.json.JsonBasicType;
import manifold.api.json.JsonIssue;
import manifold.api.json.JsonListType;
import manifold.api.json.Token;
import manifold.api.type.ActualName;
import manifold.api.type.SourcePosition;
import manifold.api.type.TypeReference;
import manifold.api.util.JsonUtil;
import manifold.api.util.ManClassUtil;
import manifold.api.util.ManEscapeUtil;
import manifold.api.util.ManStringUtil;

/**
 * The base JSON Schema type.
 * <p>
 * <b>Attention!</b> subclasses must take care to share state with copies.
 * See the {@link State} class below.
 */
public abstract class JsonSchemaType implements IJsonParentType, Cloneable
{
  @SuppressWarnings("WeakerAccess")
  protected static final String FIELD_FILE_URL = "__FILE_URL_";

  /**
   * Since we use clone() to copy, assignment to these fields must be reflected across all copies,
   * hence the encapsulation/indirection with the State class.
   */
  private static class State
  {
    private final String _name;
    private JsonSchemaType _parent;
    private final IFile _file;
    private List<IJsonType> _definitions;
    private List<JsonIssue> _issues;
    private boolean _bSchemaType;
    private ResolveState _resolveState;
    private Token _token;

    private State( String name, JsonSchemaType parent, IFile file )
    {
      _name = name;
      _parent = parent;
      _file = file;

      _issues = Collections.emptyList();
      _resolveState = ResolveState.Unresolved;
    }
  }

  enum ResolveState
  {
    Unresolved, Resolving, Resolved
  }

  private final State _state; // shared state across type copies
  private TypeAttributes _typeAttributes; // non-shared state that is different per type copy
  private AbstractJsonTypeManifold _tm;
  
  protected JsonSchemaType( String name, IFile source, JsonSchemaType parent, TypeAttributes attr )
  {
    _state = new State( name, parent, source );
    _typeAttributes = attr;
  }

  public String getFqn()
  {
    String result = "";
    if( !isParentRoot() )
    {
      result = getParent().getFqn();
      result += '.';
    }
    return result + JsonUtil.makeIdentifier( getLabel() );
  }

  protected AbstractJsonTypeManifold getTm()
  {
    if( _tm == null && getParent() != null )
    {
      return getParent().getTm();
    }
    return _tm;
  }
  public void setTm( AbstractJsonTypeManifold tm )
  {
    _tm = tm;
  }

  final public void resolveRefs()
  {
    if( _state._resolveState != ResolveState.Unresolved )
    {
      return;
    }

    _state._resolveState = ResolveState.Resolving;
    try
    {
      resolveRefsImpl();
    }
    finally
    {
      _state._resolveState = ResolveState.Resolved;
    }
  }

  protected void resolveRefsImpl()
  {
    List<IJsonType> definitions = getDefinitions();
    if( definitions != null && !definitions.isEmpty() )
    {
      List<IJsonType> resolved = new ArrayList<>();
      for( IJsonType type : definitions )
      {
        if( type instanceof JsonSchemaType )
        {
          ((JsonSchemaType)type).resolveRefs();
        }
        else if( type instanceof LazyRefJsonType )
        {
          type = ((LazyRefJsonType)type).resolve();
        }
        resolved.add( type );
      }
      _state._definitions = resolved;
    }
  }

  @SuppressWarnings("WeakerAccess")
  protected boolean isParentRoot()
  {
    return getParent() == null ||
           getParent().getParent() == null && !getParent().getName().equals( JsonSchemaTransformer.JSCH_DEFINITIONS );
  }

  public IFile getFile()
  {
    return _state._file != null
           ? _state._file
           : _state._parent != null
             ? _state._parent.getFile()
             : null;
  }

  public String getLabel()
  {
    return getName();
  }

  @Override
  public String getName()
  {
    return _state._name;
  }

  @Override
  public String getIdentifier()
  {
    return JsonUtil.makeIdentifier( getName() );
  }

  public Token getToken()
  {
    return _state._token;
  }
  public void setToken( Token token )
  {
    _state._token = token;
  }

  @Override
  public JsonSchemaType getParent()
  {
    return _state._parent;
  }
  public void setParent( IJsonParentType parent )
  {
    _state._parent = (JsonSchemaType)parent;
  }

  @Override
  public List<IJsonType> getDefinitions()
  {
    return _state._definitions;
  }

  public void setDefinitions( List<IJsonType> definitions )
  {
    _state._definitions = definitions;
  }

  protected boolean isSchemaType()
  {
    return _state._bSchemaType;
  }

  protected void setJsonSchema()
  {
    _state._bSchemaType = true;
  }

  @Override
  public TypeAttributes getTypeAttributes()
  {
    return _typeAttributes;
  }
  @Override
  public JsonSchemaType copyWithAttributes( TypeAttributes attributes )
  {
    if( getTypeAttributes().equals( attributes ) )
    {
      return this;
    }

    try
    {
      JsonSchemaType copy = (JsonSchemaType)clone();
      copy._typeAttributes = copy._typeAttributes.overrideWith( attributes );
      return copy;
    }
    catch( CloneNotSupportedException e )
    {
      throw new RuntimeException( e );
    }
  }

  protected boolean mergeInnerTypes( IJsonParentType other, IJsonParentType mergedType, Map<String, IJsonParentType> innerTypes )
  {
    for( Map.Entry<String, IJsonParentType> e : innerTypes.entrySet() )
    {
      String name = e.getKey();
      IJsonType innerType = other.findChild( name );
      if( innerType != null )
      {
        innerType = Json.mergeTypes( e.getValue(), innerType );
      }
      else
      {
        innerType = e.getValue();
      }

      if( innerType != null )
      {
        mergedType.addChild( name, (IJsonParentType)innerType );
      }
      else
      {
        return false;
      }
    }
    return true;
  }

  @Override
  public List<JsonIssue> getIssues()
  {
    if( getParent() != null )
    {
      return getParent().getIssues();
    }

    return _state._issues;
  }

  @Override
  public void addIssue( JsonIssue issue )
  {
    if( getParent() != null )
    {
      getParent().addIssue( issue );
      return;
    }

    if( _state._issues.isEmpty() )
    {
      _state._issues = new ArrayList<>();
    }
    _state._issues.add( issue );
  }

  protected void indent( StringBuilder sb, int indent )
  {
    for( int i = 0; i < indent; i++ )
    {
      sb.append( ' ' );
    }
  }

  protected void addTypeReferenceAnnotation( StringBuilder sb, int indent, JsonSchemaType type )
  {
    SrcAnnotationExpression annotation = new SrcAnnotationExpression( TypeReference.class.getName() )
      .addArgument( "value", String.class, getPropertyType( type, false, true ) );
    annotation.render( sb, indent );
  }

  protected boolean addSourcePositionAnnotation( StringBuilder sb, int indent, String name, Token token )
  {
    int offset = token.getOffset();
    IFile file = getIFile();
    if( file instanceof IFileFragment )
    {
      offset += ((IFileFragment)file).getOffset();
    }

    SrcAnnotationExpression annotation = new SrcAnnotationExpression( SourcePosition.class.getName() )
      .addArgument( new SrcArgument( new SrcMemberAccessExpression( getIdentifier(), FIELD_FILE_URL ) ).name( "url" ) )
      .addArgument( "feature", String.class, name )
      .addArgument( "offset", int.class, offset )
      .addArgument( "length", int.class, name.length() );
    annotation.render( sb, indent );
    return true;
  }

  protected IFile getIFile()
  {
    return getFile();
  }

  protected String addActualNameAnnotation( StringBuilder sb, int indent, String name, boolean capitalize )
  {
    String identifier = makeIdentifier( name, capitalize );
    if( !identifier.equals( name ) )
    {
      indent( sb, indent );
      sb.append( "@" ).append( ActualName.class.getName() ).append( "( \"" ).append( name ).append( "\" )\n" );
    }
    return identifier;
  }

  protected String makeMemberIdentifier( IJsonType type )
  {
    return makeIdentifier( type.getName(), false );
  }
  protected String makeIdentifier( String name, boolean capitalize )
  {
    return capitalize ? ManStringUtil.capitalize( JsonUtil.makeIdentifier( name ) ) : JsonUtil.makeIdentifier( name );
  }

  protected void renderFileField( StringBuilder sb, int indent )
  {
    renderFileField( sb, indent, null );
  }

  @SuppressWarnings("WeakerAccess")
  protected void renderFileField( StringBuilder sb, int indent, String modifiers )
  {
    indent( sb, indent );
    try
    {
      String url = getFile() == null ? "null" : getFile().toURI().toURL().toString();
      url = ManEscapeUtil.escapeForJavaStringLiteral( url );
      sb.append( modifiers == null ? "" : modifiers + " " ).append( "String " ).append( FIELD_FILE_URL ).append( " = \"" ).append( url ).append( "\";\n" );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  protected String getPropertyType( IJsonType propertyType )
  {
    return getPropertyType( propertyType, false, false );
  }
  protected String getPropertyType( IJsonType propertyType, boolean qualifiedWithMe, boolean param )
  {
    String name;
    if( propertyType instanceof JsonListType )
    {
      if( param )
      {
        String componentTypeName = makeTypeParameter( ((JsonListType)propertyType).getComponentType(), qualifiedWithMe, param );
        name = List.class.getTypeName() + '<' + componentTypeName + '>';
      }
      else
      {
        name = getNameRelativeFromMe( propertyType, qualifiedWithMe );
      }
    }
    else if( propertyType instanceof JsonUnionType )
    {
      JsonEnumType enumType = ((JsonUnionType)propertyType).getCollapsedEnumType();
      name = enumType != null
             ? getNameRelativeFromMe( enumType, qualifiedWithMe )
             : Object.class.getSimpleName();
    }
    else
    {
      name = propertyType instanceof JsonSchemaType
             ? getNameRelativeFromMe( propertyType, qualifiedWithMe )
             : propertyType.getIdentifier();
    }
    return name;
  }

  protected String makeTypeParameter( IJsonType type, boolean qualifiedWithMe, boolean param )
  {
    if( type instanceof JsonBasicType && ((JsonBasicType)type).isPrimitive() )
    {
      // Must box primitive type for List<T>
      return ((JsonBasicType)type).box().getTypeName();
    }
    return getPropertyType( type, qualifiedWithMe, param );
  }

  protected IJsonType getConstituentQnComponent( IJsonType constituentType )
  {
    if( constituentType instanceof JsonListType )
    {
      return getConstituentQnComponent( ((JsonListType)constituentType).getComponentType() );
    }
    return constituentType;
  }

  private String getNameRelativeFromMe( IJsonType type, boolean qualifiedWithMe )
  {
    if( type instanceof JsonSchemaType && !Objects.equals( getPackage( (JsonSchemaType)type ), getPackage( this ) ) )
    {
      return getFqn( (JsonSchemaType)type );
    }

    IJsonType parent = getParentFromMe( type, qualifiedWithMe );
    if( parent == null )
    {
      return type.getIdentifier();
    }

    return getNameRelativeFromMe( parent, qualifiedWithMe ) + '.' + type.getIdentifier();
  }

  protected String getFqn( JsonSchemaType type )
  {
    if( type.getParent() == null )
    {
      return getPackage( type ) + '.' + type.getIdentifier();
    }

    return getFqn() + '.' + getIdentifier();
  }

  private String getPackage( JsonSchemaType type )
  {
    if( type.getParent() != null )
    {
      return getPackage( type.getParent() );
    }
    IFile file = type.getFile();
    String[] types = getTm().getTypesForFile( file );
    String fqn = Arrays.stream( types ).filter( e -> e.endsWith( type.getIdentifier() ) ).findFirst().orElse( null );
    return ManClassUtil.getPackage( fqn );
  }

  private IJsonType getParentFromMe( IJsonType type, boolean qualifiedWithMe )
  {
    IJsonParentType parent = type.getParent();
    if( parent != null )
    {
//      if( parent instanceof JsonListType && parent.getParent() != null )
//      {
//        return getParentFromMe( parent, qualifiedWithMe );
//      }

      if( parent.getIdentifier().equals( JsonSchemaTransformer.JSCH_DEFINITIONS ) )
      {
        return getParentFromMe( parent, qualifiedWithMe );
      }

      if( parent == this )
      {
        return qualifiedWithMe ? this : null;
      }
    }

    return parent;
  }

  protected String getConstituentQn( IJsonType constituentType, IJsonType propertyType )
  {
    return getConstituentQn( constituentType, propertyType, false );
  }
  protected String getConstituentQn( IJsonType constituentType, IJsonType propertyType, boolean param )
  {
    String qn;
    if( !(propertyType instanceof JsonListType) )
    {
      qn = getPropertyType( constituentType, false, param );
    }
    else
    {
      qn = makeTypeParameter( constituentType, false, param );
      while( propertyType instanceof JsonListType )
      {
        //noinspection StringConcatenationInLoop
        qn = (param ? List.class.getTypeName() : IJsonList.class.getTypeName()) + '<' + qn + '>';
        propertyType = ((JsonListType)propertyType).getComponentType();
      }
    }
    return qn;
  }

  protected boolean isCollapsedUnionEnum( IJsonType type )
  {
    while( type instanceof JsonListType )
    {
      type = ((JsonListType)type).getComponentType();
    }
    JsonEnumType enumType = type instanceof JsonUnionType ? ((JsonUnionType)type).getCollapsedEnumType() : null;
    return enumType != null;
  }

  protected String removeGenerics( String specificPropertyType )
  {
    String rawSpecificPropertyType = specificPropertyType;
    int iAngle = specificPropertyType.indexOf( "<" );
    if( iAngle > 0 )
    {
      rawSpecificPropertyType = rawSpecificPropertyType.substring( 0, iAngle );
      if( rawSpecificPropertyType.contains( IJsonList.class.getSimpleName() ) )
      {
        rawSpecificPropertyType = List.class.getSimpleName();
      }
    }
    return rawSpecificPropertyType;
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    JsonSchemaType that = (JsonSchemaType)o;

    return getName().equals( that.getName() );
  }

  @Override
  public int hashCode()
  {
    return getName().hashCode();
  }
}
