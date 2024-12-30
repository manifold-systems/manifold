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

package manifold.api.properties;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.util.function.Supplier;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.gen.AbstractSrcClass.Kind;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcArgument;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcConstructor;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcIdentifier;
import manifold.api.gen.SrcLinkedClass;
import manifold.api.gen.SrcMemberAccessExpression;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcMethodCallExpression;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawExpression;
import manifold.api.gen.SrcRawStatement;
import manifold.api.gen.SrcReturnStatement;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.gen.SrcSwitchCase;
import manifold.api.gen.SrcSwitchStatement;
import manifold.api.gen.SrcType;
import manifold.api.host.IModule;
import manifold.api.util.cache.FqnCache;
import manifold.api.util.cache.FqnCacheNode;
import manifold.rt.api.SourcePosition;
import manifold.rt.api.util.StreamUtil;

import static java.nio.charset.StandardCharsets.*;
import static manifold.api.gen.AbstractSrcClass.Kind.*;

abstract class CommonCodeGen
{
  private static final String FIELD_FILE_URL = "__FILE_URL";
  private static final String PROPERTIES_OBJECT_INTERFACE_NAME = "_PropertiesObjectIntf";
  private static final String LEAF_CLASS_NAME = "LeafClass";
  protected final String _fqn;
  protected final String _content;
  protected final FqnCache<SrcRawExpression> _model;
  private final String _leafClassFqn;
  protected IFile _file;

  CommonCodeGen( FqnCache<SrcRawExpression> model, IFile file, String fqn )
  {
    _model = model;
    _file = file;
    _fqn = fqn;
    _content = assignContent();
    _leafClassFqn = fqn + "." + LEAF_CLASS_NAME;
  }

  SrcClass make( IModule module, JavaFileManager.Location location, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcClass = new SrcClass( _fqn, SrcClass.Kind.Class, location, module, errorHandler )
        .imports( SourcePosition.class, Supplier.class, Object.class, CharSequence.class );

    addLocationAndPropertiesFileUrlField( srcClass, _model );
    addObjectInterface( srcClass );
    addLeafClass( srcClass );

    extendSrcClass( srcClass, _model );

    return make( srcClass, _model );
  }

  private void addLocationAndPropertiesFileUrlField( SrcClass srcClass, FqnCacheNode<SrcRawExpression> node )
  {
    if( _file == null )
    {
      return;
    }

    srcClass.addAnnotation( addSourcePositionAnnotation( node ) );

    srcClass.addField(
        new SrcField( srcClass )
            .name( FIELD_FILE_URL )
            .modifiers( Modifier.STATIC | Modifier.FINAL )
            .type( "String" )
            .initializer( getFile() ) );
  }

  private void addLeafClass( SrcClass srcClass )
  {
    SrcClass leafClass = new SrcClass( LEAF_CLASS_NAME, srcClass, Kind.Class )
        .modifiers( Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL )
        .addInterface( "CharSequence" )
        .addField( new SrcField( srcClass )
            .name( "_data" )
            .modifiers( Modifier.PRIVATE | Modifier.FINAL )
            .type( "Supplier<String>" ) )
        .addConstructor( new SrcConstructor()
            .addParam( "data", "Supplier<String>" )
            .body( "_data  = data;" ) )
        .addMethod( new SrcMethod( srcClass )
            .returns( "String" )
            .modifiers( Modifier.PUBLIC )
            .name( "toString" )
            .body( "return _data.get();" ) )
        .addMethod( new SrcMethod( srcClass )
            .returns( "int" )
            .modifiers( Modifier.PUBLIC )
            .name( "length" )
            .body( "return toString().length();") )
        .addMethod( new SrcMethod( srcClass )
            .returns( "char" )
            .modifiers( Modifier.PUBLIC )
            .name( "charAt" )
            .addParam("index", "int" )
            .body( "return toString().charAt(index);") )
        .addMethod( new SrcMethod( srcClass )
            .returns( "CharSequence" )
            .modifiers( Modifier.PUBLIC )
            .name( "subSequence" )
            .addParam("start", "int" )
            .addParam("end", "int" )
            .body( "return toString().subSequence(start, end);" ) )
        .addMethod( new SrcMethod( srcClass )
            .returns( "String" )
            .modifiers( Modifier.PUBLIC )
            .name( "get" )
            .body( "return toString();") );

    extendLeafClass( leafClass );

    srcClass.addInnerClass( leafClass );
  }

  protected abstract void extendLeafClass( SrcClass leafClass );

  private void addObjectInterface( SrcClass srcClass ) {
    srcClass.addInnerClass(
        new SrcLinkedClass( PROPERTIES_OBJECT_INTERFACE_NAME, srcClass, Interface )
            .modifiers( Modifier.PRIVATE )
            .addMethod( new SrcMethod( srcClass )
                .returns( "String" )
                .name("getValueByName" )
                .addParam("propertyName", "String" ) ) );
  }

  protected abstract void extendSrcClass(SrcClass srcClass, FqnCache<SrcRawExpression> model);

  private SrcClass make( SrcClass srcClass, FqnCacheNode<SrcRawExpression> node )
  {
    if( srcClass.getEnclosingClass() != null ){
        srcClass.addInterface( PROPERTIES_OBJECT_INTERFACE_NAME );
    }
    for( FqnCacheNode<SrcRawExpression> childNode: node.getChildren() )
    {
      SrcType type = new SrcType(childNode.isLeaf() ? _leafClassFqn : childNode.getName());
      SrcField propertyField = new SrcField(srcClass)
          .name(childNode.getName())
          .modifiers(Modifier.PUBLIC | Modifier.FINAL | (srcClass.getEnclosingClass() == null ? Modifier.STATIC : 0))
          .type(type)
          .initializer(childNode.isLeaf()
              ? new SrcRawExpression( "new " + _leafClassFqn + "(() -> " + childNode.getUserData() + ")" )
              : new SrcRawExpression("new " + type + "()"));
      if (_file != null) {
        propertyField.addAnnotation(addSourcePositionAnnotation(childNode));
      }
      srcClass.addField(propertyField);
      if( !childNode.isLeaf() )
      {
        SrcClass innerSrcClass = new SrcClass( childNode.getName(), srcClass, SrcClass.Kind.Class )
            .modifiers( Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL );
        srcClass.addInnerClass( make( innerSrcClass, childNode ) );
      }
    }
    addMethods( srcClass, node );

    return srcClass;
  }

  private SrcRawExpression getFile()
  {
    try
    {
      return new SrcRawExpression( new SrcType( "String" ), _file.toURI().toURL().toString() );
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  private SrcAnnotationExpression addSourcePositionAnnotation( FqnCacheNode<SrcRawExpression> node )
  {
    return new SrcAnnotationExpression( SourcePosition.class.getSimpleName() )
        .addArgument( new SrcArgument( new SrcMemberAccessExpression( _fqn, FIELD_FILE_URL ) ).name( "url" ) )
        .addArgument( "feature", new SrcType( "String" ), node.getName() )
        .addArgument( "offset", int.class, findOffsetOf( node ) )
        .addArgument( "length", int.class, node.getName() == null ? 0 : node.getName().length() );
  }

  private void addMethods( SrcClass srcClass, FqnCacheNode<SrcRawExpression> node )
  {
    if( !node.isLeaf() )
    {
      addGetValueByNameMethod( srcClass, node );
      addGetObjectMethod( srcClass, node );

      SrcRawExpression userData = node.getUserData();
      if( userData != null )
      {
        addGetValueMethod( srcClass, node );
        addToString( srcClass );
      }
    }
  }

  private void addToString( SrcClass srcClass )
  {
    srcClass.addMethod(
        new SrcMethod( srcClass )
            .name( "toString" )
            .modifiers( Modifier.PUBLIC )
            .returns( new SrcType( "String" ) )
            .body(
                new SrcStatementBlock()
                    .addStatement(
                        new SrcReturnStatement( new SrcMethodCallExpression( "getValue" ) ) )
            )
    );
  }

  private void addGetValueByNameMethod( SrcClass srcClass, FqnCacheNode<SrcRawExpression> node )
  {
    srcClass.addMethod(
        new SrcMethod( srcClass )
            .name( "getValueByName" )
            .modifiers( Modifier.PUBLIC | (isRootProperty( node ) ? Modifier.STATIC : 0) )
            .returns( new SrcType( "String" ) )
            .addParam( new SrcParameter( "propertyName" ).type( "String" ) )
            .body( new SrcStatementBlock()
                .addStatement( makeGetValueBynameSwitch( node ) ) )
    );
  }

  private void addGetObjectMethod( SrcClass srcClass, FqnCacheNode<SrcRawExpression> node )
  {
    srcClass.addMethod(
        new SrcMethod( srcClass )
            .name( "getObject" )
            .modifiers( Modifier.PRIVATE | (isRootProperty( node ) ? Modifier.STATIC : 0) )
            .returns( new SrcType( PROPERTIES_OBJECT_INTERFACE_NAME ) )
            .addParam( new SrcParameter( "propertyName" ).type( "String" ) )
            .body( new SrcStatementBlock()
                .addStatement( makeGetObjectSwitch( node ) ) )
    );
  }

  protected boolean isRootProperty( FqnCacheNode<SrcRawExpression> node )
  {
    return node.getParent() == null;
  }

  private SrcSwitchStatement makeGetValueBynameSwitch( FqnCacheNode<SrcRawExpression> node )
  {
    SrcSwitchStatement stmt = new SrcSwitchStatement();
    stmt.expr( new SrcIdentifier( "propertyName" ) );
    for( FqnCacheNode<SrcRawExpression> childNode: node.getChildren() )
    {
      stmt.addCase(
          new SrcSwitchCase( new SrcType( "String" ), childNode.getName() )
              .statement( childNode.getUserData() == null ? new SrcReturnStatement( String.class, null) :
                  new SrcReturnStatement( childNode.getUserData() ) ) );
    }
    stmt.defaultCase( new SrcRawStatement().rawText(
      "String[] split = propertyName.split(\"\\\\.\", 2);"
          + "if (split.length == 2) {"
          + "  " + PROPERTIES_OBJECT_INTERFACE_NAME + " object = getObject(split[0]);"
          + "  if (object != null) {"
          + "    return object.getValueByName(split[1]);"
          + "  }"
          + "  return null;"
          + "}"
          + "return null;"));
    return stmt;
  }

  private SrcSwitchStatement makeGetObjectSwitch( FqnCacheNode<SrcRawExpression> node )
  {
    SrcSwitchStatement stmt = new SrcSwitchStatement();
    stmt.expr( new SrcIdentifier( "propertyName" ) );
    for( FqnCacheNode<SrcRawExpression> childNode: node.getChildren() )
    {
      stmt.addCase(
          new SrcSwitchCase( new SrcType( "String" ), childNode.getName() )
              .statement( childNode.getUserData() == null || !childNode.getChildren().isEmpty() ?
                  new SrcReturnStatement( Object.class, childNode.getName() ) :
                  new SrcReturnStatement( Object.class, null) ) );
    }
    stmt.defaultCase( new SrcReturnStatement( String.class, null ) );
    return stmt;
  }

  private void addGetValueMethod( SrcClass srcClass, FqnCacheNode<SrcRawExpression> node )
  {
    srcClass.addMethod(
        new SrcMethod( srcClass )
            .name( "getValue" )
            .modifiers( Modifier.PUBLIC | (isRootProperty( node ) ? Modifier.STATIC : 0) )
            .returns( new SrcType( "String" ) )
            .body(
                new SrcStatementBlock()
                    .addStatement( new SrcReturnStatement( node.getUserData() ) ) ) );
  }

  private int findOffsetOf( FqnCacheNode<SrcRawExpression> node )
  {
    String fqn = node.getFqn();
    String prefix = _fqn + '.';
    if( fqn.startsWith( prefix ) )
    {
      fqn = fqn.substring( prefix.length() );
    }

    // this is a crappy way to approximate the offset, we really need to parse the file ourselves and store the offsets
    int offset = -1;
    int iFqn = findProperty( fqn, false );
    if( iFqn >= 0 )
    {
      offset = useOffsetOfLastMember( fqn, iFqn );
    }
    else
    {
      iFqn = findProperty( fqn, true );
      if( iFqn >= 0 )
      {
        offset = useOffsetOfLastMember( fqn, iFqn );
      }
    }

    //assert offset >= 0;

    if( _file instanceof IFileFragment )
    {
      offset += ((IFileFragment)_file).getOffset();
    }

    return offset;
  }

  private int useOffsetOfLastMember( String fqn, int offset )
  {
    if( offset < 0 )
    {
      return offset;
    }

    int iDot = fqn.lastIndexOf( '.' );
    if( iDot > 0 )
    {
      offset += iDot + 1;
    }
    return offset;
  }

  private int findProperty( String property, boolean bPartialMatch )
  {
    String content = _content;
    int index = 0;
    while( true )
    {
      index = content.indexOf( property, index );
      if( index < 0 )
      {
        break;
      }
      char op;
      if( isPropertyStart( content, index ) &&
          content.length() > index + property.length() &&
          ((op = content.charAt( index + property.length() )) == '=' || op == ' ' || (bPartialMatch && op == '.')) )
      {
        break;
      }

      index += property.length();
      if( index >= content.length() )
      {
        index = -1;
        break;
      }
    }
    return index;
  }

  private boolean isPropertyStart( String content, int index )
  {
    if( --index < 0 )
    {
      return true;
    }

    char c = content.charAt( index );
    if( c == '\n' )
    {
      return true;
    }
    if( c == ' ' || c == '\t' )
    {
      // ignore indentation
      return isPropertyStart( content, index );
    }
    return false;
  }

  private String assignContent()
  {
    if( _file != null )
    {
      try( InputStream inputStream = _file.openInputStream() )
      {
        return StreamUtil.getContent( new InputStreamReader( inputStream, UTF_8 ) ); //.replace( "\r\n", "\n" );
      }
      catch( Exception e )
      {
        throw new RuntimeException( e );
      }
    }
    return null;
  }
}
