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

package manifold.api.json.codegen.schema;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.fs.def.FileFragmentImpl;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcArgument;
import manifold.api.gen.SrcMemberAccessExpression;
import manifold.api.host.IModule;
import manifold.api.json.AbstractJsonTypeManifold;
import manifold.json.rt.api.IJsonList;
import manifold.api.json.codegen.IJsonParentType;
import manifold.api.json.codegen.IJsonType;
import manifold.api.json.JsonTransformer;
import manifold.api.json.codegen.JsonBasicType;
import manifold.api.json.JsonIssue;
import manifold.api.json.codegen.JsonListType;
import manifold.api.json.JsonTypeManifold;
import manifold.json.rt.parser.Token;
import manifold.api.type.ActualName;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;
import manifold.api.type.SourcePosition;
import manifold.api.type.TypeReference;
import manifold.api.util.ManIdentifierUtil;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.ManEscapeUtil;
import manifold.rt.api.util.ManStringUtil;

/**
 * The base JSON Schema type.
 * <p>
 * <b>Attention!</b> subclasses must take care to share state with copies.
 * See the {@link State} class below.
 */
public abstract class JsonSchemaType implements IJsonParentType, Cloneable
{
  protected static final String FIELD_FILE_URL = "__FILE_URL_";
  @SuppressWarnings("unused")
  protected static final String FROM_SOURCE_METHOD = "fromSource";

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
    private boolean _synthetic;

    private IModule _module;
    private JavaFileManager.Location _location;
    private DiagnosticListener<JavaFileObject> _errorHandler;

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

  /**
   * exclusive to top-level types (facilitates inner class extensions)
   */
  @Override
  public void prepareToRender( JavaFileManager.Location location, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    _state._location = location;
    _state._module = module;
    _state._errorHandler = errorHandler;
  }

  public String getFqn()
  {
    String result = "";
    if( !isParentRoot() )
    {
      result = getParent().getFqn();
      result += '.';
    }
    return result + ManIdentifierUtil.makeIdentifier( getLabel() );
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
    return ManIdentifierUtil.makeIdentifier( getName() );
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

  protected boolean isSyntheticSchema()
  {
    return _state._synthetic;
  }
  @SuppressWarnings("WeakerAccess")
  protected void setSyntheticSchema( boolean synthetic )
  {
    _state._synthetic = synthetic;
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
        innerType = JsonTransformer.mergeTypes( e.getValue(), innerType );
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

  protected void addFromSourceMethod( StringBuilder sb, int indent )
  {
    IFile file = getIFile();
    if( (isSchemaType() && !isSyntheticSchema()) || !isParentRoot() )
    {
      return;
    }

    //noinspection unused
    String typeName = getIdentifier();
    indent( sb, indent );
    sb.append( "static $typeName $FROM_SOURCE_METHOD() {\n" );
    indent( sb, indent );

    //## todo: this switch is ripe, should be configurable as part of AbstractJsonTypeManifold somehow?
    String methodName;
    switch( file.getExtension().toLowerCase() )
    {
      case JsonTypeManifold.FILE_EXTENSION:
        methodName = "fromJson";
        break;
      case "yaml":
      case "yml":
        methodName = "fromYaml";
        break;
      case "xml":
        methodName = "fromXml";
        break;
      case "csv":
      case "tsv":
      case "tab":
        methodName = "fromCsv";
        break;
      default:
        throw new IllegalStateException();
    }

    if( file instanceof FileFragmentImpl )
    {
      // include fragment directly as string literal

      sb.append( "  return load()." ).append( methodName )
        .append( "(\"" ).append( getContentForLiteral( (FileFragmentImpl)file ) ).append( "\");\n" );
    }
    else
    {
      // avoid using a string literal, file could be very large, instead reference the corresponding resource file

      //## todo: using getFqn(), which may not correspond with resource file name
      //noinspection unused
      String resourceFile = '/' + getFqn( this ).replace( '.', '/' ) + '.' + file.getExtension();
      sb.append( "  return load()." ).append( methodName ).append( "Reader" )
        .append( "(new java.io.InputStreamReader($typeName.class.getResourceAsStream(\"$resourceFile\")));\n" );
    }
    indent( sb, indent );
    sb.append( "}\n" );
  }

  private String getContentForLiteral( FileFragmentImpl file )
  {
    String content = file.getContent();
    return ManEscapeUtil.escapeForJavaStringLiteral( content );
  }

  protected void addRequestMethods( StringBuilder sb, int indent, @SuppressWarnings("unused") String typeName )
  {
    indent( sb, indent );
    //noinspection unused
    sb.append( "static " ).append( "Requester<$typeName>" ).append( " request(String urlBase) {\n" );
    indent( sb, indent );
    sb.append( "  return new Requester<>(urlBase, result -> RuntimeMethods.coerce( result, $typeName.class));\n" );
    indent( sb, indent );
    sb.append( "}\n" );

    indent( sb, indent );
    //noinspection unused
    sb.append( "static " ).append( "Requester<$typeName>" ).append( " request(Endpoint endpoint) {\n" );
    indent( sb, indent );
    sb.append( "  return new Requester<>(endpoint, result -> RuntimeMethods.coerce( result, $typeName.class));\n" );
    indent( sb, indent );
    sb.append( "}\n" );
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
    return capitalize ? ManStringUtil.capitalize( ManIdentifierUtil.makeIdentifier( name ) ) : ManIdentifierUtil.makeIdentifier( name );
  }

  protected void renderFileField( StringBuilder sb, int indent )
  {
    renderFileField( sb, indent, null );
  }

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

  protected String getActualFqn( AbstractJsonTypeManifold tm )
  {
    if( getParent() == null )
    {
      String pkg = getPackage( tm, this );
      return pkg.isEmpty() ? getIdentifier() : pkg + '.' + getIdentifier();
    }
    return getParent().getActualFqn( tm ) + '.' + getIdentifier();
  }

  private String getPackage( JsonSchemaType type )
  {
    return getPackage( getTm(), type );
  }
  private String getPackage( AbstractJsonTypeManifold tm, JsonSchemaType type )
  {
    if( type.getParent() != null )
    {
      return getPackage( type.getParent() );
    }
    IFile file = type.getFile();
    String[] types = (tm != null ? tm : getTm()).getTypesForFile( file );
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

  public JavaFileManager.Location getLocation()
  {
    return _state._location != null ? _state._location : getParent() != null ? getParent().getLocation() : null;
  }
  public IModule getModule()
  {
    return _state._module != null ? _state._module : getParent() != null ? getParent().getModule() : null;
  }
  public DiagnosticListener<JavaFileObject> getErrorHandler()
  {
    return _state._errorHandler != null ? _state._errorHandler : getParent() != null ? getParent().getErrorHandler() : null;
  }

  public void renderInner( AbstractJsonTypeManifold tm, StringBuilder sb, int indent, boolean mutable )
  {
    StringBuilder innerSb = new StringBuilder();
    render( tm, innerSb, indent, mutable );
    if( getParent() != null )
    {
      if( getModule() != null && getLocation() != null )
      {
        // Location can be null within an IDE plugin. In this case the IDE must supplement classes its own way on its AST.
        //
        // add extensions to inner types
        innerSb = contributeInner( getActualFqn( tm ), innerSb );
      }
    }
    sb.append( innerSb );
  }

  private StringBuilder contributeInner( String fqnInner, StringBuilder innerSource )
  {
    Set<ITypeManifold> sps = getModule().findTypeManifoldsFor( fqnInner );
    for( ITypeManifold sp: sps )
    {
      if( sp.getContributorKind() == ContributorKind.Supplemental )
      {
        innerSource = new StringBuilder( sp.contribute( getLocation(), fqnInner, false, innerSource.toString(), getErrorHandler() ) );
      }
    }
    return innerSource;
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
