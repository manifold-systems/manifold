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

package manifold.api.gen;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.lang.model.element.ElementKind;
import manifold.util.ManClassUtil;

/**
 */
public class SrcClass extends SrcStatement<SrcClass>
{
  private String _package;
  private List<String> _imports;
  private final Kind _kind;
  private SrcType _superClass;
  private final SrcClass _enclosingClass;
  private List<SrcType> _interfaces = new ArrayList<>();
  private List<SrcField> _fields = new ArrayList<>();
  private List<SrcField> _enumConsts = new ArrayList<>();
  private List<SrcConstructor> _constructors = new ArrayList<>();
  private List<AbstractSrcMethod> _methods = new ArrayList<>();
  private List<SrcStatementBlock> _staticBlocks = new ArrayList<>();
  private TreeMap<String, SrcGetProperty> _getProperties = new TreeMap<>();
  private TreeMap<String, SrcSetProperty> _setProperties = new TreeMap<>();
  private List<SrcClass> _innerClasses = new ArrayList<>();
  private List<SrcType> _typeVars;


  public SrcClass( String fqn, Kind kind )
  {
    this( fqn, null, kind );
  }

  public SrcClass( String fqn, SrcClass enclosingClass, Kind kind )
  {
    super( enclosingClass );
    fullName( fqn );
    _enclosingClass = enclosingClass;
    _kind = kind;
    _imports = new ArrayList<>();
    _typeVars = new ArrayList<>();
  }

  private SrcClass fullName( String fqn )
  {
    _package = ManClassUtil.getPackage( fqn );
    return name( ManClassUtil.getShortClassName( fqn ) );
  }

  public SrcClass superClass( SrcType superClass )
  {
    _superClass = superClass;
    return this;
  }

  public SrcClass superClass( Class superClass )
  {
    _superClass = new SrcType( superClass );
    return this;
  }

  public SrcClass superClass( String superClass )
  {
    _superClass = new SrcType( superClass );
    return this;
  }

  public SrcClass addInterface( SrcType iface )
  {
    _interfaces.add( iface );
    iface.setOwner( this );
    return this;
  }

  public SrcClass addInterface( Class iface )
  {
    SrcType t = new SrcType( iface );
    return addInterface( t );
  }

  public SrcClass addInterface( String iface )
  {
    SrcType t = new SrcType( iface );
    return addInterface( t );
  }

  public SrcClass addField( SrcField field )
  {
    _fields.add( field );
    field.setOwner( this );
    return this;
  }

  public SrcClass addEnumConst( SrcField enumConst )
  {
    _enumConsts.add( enumConst );
    enumConst.setOwner( this );
    return this;
  }

  public SrcClass addConstructor( SrcConstructor ctor )
  {
    _constructors.add( ctor );
    ctor.setOwner( this );
    return this;
  }

  public SrcClass addMethod( AbstractSrcMethod method )
  {
    _methods.add( method );
    method.setOwner( this );
    return this;
  }

  public SrcClass addGetProperty( SrcGetProperty property )
  {
    _getProperties.put( property.getSimpleName(), property );
    property.setOwner( this );
    return this;
  }

  public SrcClass addSetProperty( SrcSetProperty property )
  {
    _setProperties.put( property.getSimpleName(), property );
    property.setOwner( this );
    return this;
  }

  public SrcClass addInnerClass( SrcClass innerClass )
  {
    _innerClasses.add( innerClass );
    innerClass.setOwner( this );
    return this;
  }

  public SrcClass addStaticBlock( SrcStatementBlock block )
  {
    _staticBlocks.add( block );
    block.setOwner( this );
    return this;
  }

  public SrcClass imports( Class<?>... classes )
  {
    for( Class c : classes )
    {
      _imports.add( c.getName() );
    }
    return this;
  }

  public SrcClass imports( String... classes )
  {
    for( String c : classes )
    {
      _imports.add( c );
    }
    return this;
  }

  public SrcClass addImport( Class cls )
  {
    return addImport( cls.getName() );
  }

  public SrcClass addImport( String path )
  {
    _imports.add( path );
    return this;
  }

  public SrcClass addStaticImport( String path )
  {
    _imports.add( " static " + path );
    return this;
  }

  public String getPackage()
  {
    return _package;
  }

  public Kind getKind()
  {
    return _kind;
  }

  public SrcType getSuperClass()
  {
    return _superClass;
  }

  public SrcClass getEnclosingClass()
  {
    return _enclosingClass;
  }

  public List<SrcType> getInterfaces()
  {
    return _interfaces;
  }

  public List<SrcField> getFields()
  {
    return _fields;
  }

  public List<SrcField> getEnumConsts()
  {
    return _enumConsts;
  }

  public List<SrcConstructor> getConstructors()
  {
    return _constructors;
  }

  public List<AbstractSrcMethod> getMethods()
  {
    return _methods;
  }

  public List<SrcStatementBlock> getStaticBlocks()
  {
    return _staticBlocks;
  }

  public List<SrcClass> getInnerClasses()
  {
    return _innerClasses;
  }

  public void addTypeVar( SrcType typeVar )
  {
    _typeVars.add( typeVar );
  }

  public String getName()
  {
    return (_package.isEmpty() ? "" : (_package + '.')) + getSimpleName();
  }

  public boolean isInterface()
  {
    return _kind == Kind.Interface;
  }

  public boolean isEnum()
  {
    return _kind == Kind.Enum;
  }

  public boolean isAnnotation()
  {
    return _kind == Kind.Annotation;
  }

  public List<SrcType> getTypeVariables()
  {
    return _typeVars;
  }

  public StringBuilder render()
  {
    return render( 0 );
  }
  public StringBuilder render( int indent )
  {
    return render( new StringBuilder(), indent );
  }
  @Override
  public StringBuilder render( StringBuilder sb, int indent )
  {
    return render( sb, indent, true );
  }

  public StringBuilder render( StringBuilder sb, int indent, boolean includePackage )
  {
    if( includePackage )
    {
      renderPackage( sb );
    }

    if( _kind == SrcClass.Kind.Enum )
    {
      renderEnum( sb, indent );
    }
    else if( _kind == SrcClass.Kind.Annotation )
    {
      renderAnnotation( sb, indent );
    }
    else
    {
      renderClassOrInterface( sb, indent );
    }
    return sb;
  }

  private void renderPackage( StringBuilder sb )
  {
    sb.append( "/* Generated */\n" );
    if( !_package.isEmpty() )
    {
      sb.append( "package " ).append( _package ).append( ";\n\n" );
    }
    for( String u : _imports )
    {
      sb.append( "import " ).append( u ).append( ";\n" );
    }
  }

  private void renderAnnotation( StringBuilder sb, int indent )
  {
    renderAnnotations( sb, indent, false );
    indent( sb, indent );
    renderModifiers( sb, getModifiers() & ~(Modifier.FINAL | Modifier.ABSTRACT), false, Modifier.PUBLIC );
    sb.append( "@interface " ).append( getSimpleName() )
      .append( " {\n" );

    renderClassFeatures( sb, indent + INDENT );

    indent( sb, indent );
    sb.append( "}\n\n" );
  }

  private void renderEnum( StringBuilder sb, int indent )
  {
    renderAnnotations( sb, indent, false );
    indent( sb, indent );
    renderModifiers( sb, getModifiers() & ~Modifier.FINAL, false, Modifier.PUBLIC );
    sb.append( "enum " ).append( getSimpleName() )
      .append( renderClassImplements( sb ) )
      .append( " {\n" );

    renderEnumConstants( sb, indent + INDENT );
    renderClassFeatures( sb, indent + INDENT );

    indent( sb, indent );
    sb.append( "}\n\n" );
  }

  private void renderEnumConstants( StringBuilder sb, int indent )
  {
    for( int i = 0; i < _enumConsts.size(); i++ )
    {
      SrcField c = _enumConsts.get( i );
      c.renderAnnotations( sb, indent, false );
      sb.append( i > 0 ? ",\n" : "" )
        .append( indent( sb, indent ) )
        .append( c.getSimpleName() )
        .append( i == _enumConsts.size() - 1 ? ";\n\n" : "" );
    }
  }

  private void renderClassOrInterface( StringBuilder sb, int indent )
  {
    renderAnnotations( sb, indent, false );
    indent( sb, indent );
    renderModifiers( sb, false, Modifier.PUBLIC );
    sb.append( _kind == Kind.Interface ? "interface " : "class " ).append( getSimpleName() ).append( renderTypeVars( _typeVars, sb ) )
      .append( genClassExtends( sb ) )
      .append( renderClassImplements( sb ) )
      .append( " {\n" );

    renderClassFeatures( sb, indent + INDENT );

    indent( sb, indent );
    sb.append( "}\n\n" );
  }

  private void renderClassFeatures( StringBuilder sb, int indent )
  {
    renderFields( sb, indent );
    renderConstructors( sb, indent );
    renderProperties( sb, indent );
    renderMethods( sb, indent );
    renderInnerClasses( sb, indent );
    renderStaticBlocks( sb, indent );
  }

  private String renderClassImplements( StringBuilder sb )
  {
    if( _interfaces.size() == 0 )
    {
      return "";
    }

    if( getKind() == Kind.Interface )
    {
      sb.append( " extends " );
    }
    else
    {
      sb.append( " implements " );
    }

    for( int i = 0; i < _interfaces.size(); i++ )
    {
      SrcType iface = _interfaces.get( i );
      sb.append( i > 0 ? ", " : "" );
      iface.render( sb, 0 );
    }
    return "";
  }

  private String genClassExtends( StringBuilder sb )
  {
    if( _superClass == null )
    {
      return "";
    }
    sb.append( " extends " );
    _superClass.render( sb, 0 );
    return "";
  }

  private void renderFields( StringBuilder sb, int indent )
  {
    sb.append( "\n" ).append( indent( sb, indent ) ).append( "// fields //\n" );
    for( SrcField field : _fields )
    {
      field.render( sb, indent );
    }
  }

  private void renderMethods( StringBuilder sb, int indent )
  {
    sb.append( "\n" ).append( indent( sb, indent ) ).append( "// methods //\n" );
    for( AbstractSrcMethod method : _methods )
    {
      method.render( sb, indent );
    }

    if( isEnum() &&
        _constructors.stream().noneMatch( c -> c.getParameters().isEmpty() ) &&
        _methods.stream().noneMatch( m -> m.isConstructor() && m.getParameters().isEmpty() ) )
    {
      // Enums need a no-arg ctor because for the stub we render the constants to not call a ctor.
      indent( sb, indent );
      sb.append( getSimpleName() ).append( "() { throw new RuntimeException(); }" );
    }
  }

  private void renderStaticBlocks( StringBuilder sb, int indent )
  {
    sb.append( "\n" ).append( indent( sb, indent ) ).append( "// static blocks //\n" );
    for( SrcStatementBlock block : _staticBlocks )
    {
      sb.append( "\n" ).append( indent( sb, indent ) ).append( "static {" );
      block.render( sb, indent );
      sb.append( "\n" ).append( indent( sb, indent ) ).append( "}" );
    }
  }

  private void renderConstructors( StringBuilder sb, int indent )
  {
    sb.append( "\n" ).append( indent( sb, indent ) ).append( "// constructors //\n" );
    for( SrcConstructor ctor : _constructors )
    {
      ctor.render( sb, indent );
    }
  }

  private void renderProperties( StringBuilder sb, int indent )
  {
    sb.append( "\n" ).append( indent( sb, indent ) ).append( "// properties //\n" );
    for( Map.Entry<String, SrcGetProperty> entry : _getProperties.entrySet() )
    {
      entry.getValue().render( sb, indent );
      SrcSetProperty srcSetProperty = _setProperties.get( entry.getKey() );
      if( srcSetProperty != null )
      {
        srcSetProperty.render( sb, indent );
      }
      sb.append( "\n" );
    }
    for( Map.Entry<String, SrcSetProperty> entry : _setProperties.entrySet() )
    {
      SrcGetProperty srcGetProperty = _getProperties.get( entry.getKey() );
      if( srcGetProperty == null )
      {
        entry.getValue().render( sb, indent );
      }
    }
  }

//  private String getTypeVariables( StringBuilder sb )
//  {
//    if( _typeVars.isEmpty() )
//    {
//      return "";
//    }
//
//    sb.append( '<' );
//    for( int i = 0; i < gtvs.size(); i++ )
//    {
//      SrcTypeVar gtv = gtvs[i];
//      sb.append( i > 0 ? ", " : "" ).append( gtv.getSimpleName() );
//      String boundingType = gtv.getBoundingType();
//      if( boundingType != null )
//      {
//        sb.append( " extends ").append( boundingType );
//      }
//    }
//    sb.append( "> " );
//    return sb.toString();
//  }

  private void renderInnerClasses( StringBuilder sb, int indent )
  {
    sb.append( "\n" ).append( indent( sb, indent ) ).append( "// inner classes //\n" );
    for( SrcClass innerClass : _innerClasses )
    {
      innerClass.render( sb, indent, false );
    }
  }

  public enum Kind
  {
    Class,
    Interface,
    Annotation,
    Enum;

    public static Kind from( ElementKind kind )
    {
      switch( kind )
      {
        case ENUM:
          return Enum;
        case CLASS:
          return Class;
        case ANNOTATION_TYPE:
          return Annotation;
        case INTERFACE:
          return Interface;
      }
      throw new IllegalArgumentException( "Bad kind: " + kind );
    }
  }
}
