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
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcArgument;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcExpression;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcIdentifier;
import manifold.api.gen.SrcMemberAccessExpression;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcMethodCallExpression;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawExpression;
import manifold.api.gen.SrcReturnStatement;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.gen.SrcSwitchCase;
import manifold.api.gen.SrcSwitchStatement;
import manifold.api.gen.SrcType;
import manifold.api.host.IModule;
import manifold.rt.api.SourcePosition;
import manifold.rt.api.util.StreamUtil;
import manifold.api.util.cache.FqnCache;
import manifold.api.util.cache.FqnCacheNode;


import static java.nio.charset.StandardCharsets.UTF_8;


/**
 *
 */
class PropertiesCodeGen
{
  private static final String FIELD_FILE_URL = "__FILE_URL_";
  private final String _fqn;
  private final String _content;
  private final FqnCache<String> _model;
  private IFile _file;

  PropertiesCodeGen( FqnCache<String> model, IFile file, String fqn )
  {
    _model = model;
    _file = file;
    _fqn = fqn;
    _content = assignContent();
  }

  SrcClass make( IModule module, JavaFileManager.Location location, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcClass = new SrcClass( _fqn, SrcClass.Kind.Class, location, module, errorHandler )
      .imports( SourcePosition.class );

    addLocationAndPropertiesFileUrlField( srcClass, _model );

    return make( srcClass, _model );
  }

  private void addLocationAndPropertiesFileUrlField( SrcClass srcClass, FqnCacheNode<String> node )
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

  private SrcClass make( SrcClass srcClass, FqnCacheNode<String> node )
  {
    for( FqnCacheNode<String> childNode: node.getChildren() )
    {
      SrcType type = new SrcType( childNode.isLeaf() ? "String" : childNode.getName() );
      SrcField propertyField = new SrcField( srcClass )
        .name( childNode.getName() )
        .modifiers( Modifier.PUBLIC | Modifier.FINAL | (srcClass.getEnclosingClass() == null ? Modifier.STATIC : 0) )
        .type( type )
        .initializer( childNode.isLeaf()
                      ? new SrcRawExpression( new SrcType( "String" ), childNode.getUserData() )
                      : new SrcRawExpression( "new " + type + "()" ) );
      if( _file != null )
      {
        propertyField.addAnnotation( addSourcePositionAnnotation( childNode ) );
      }
      srcClass.addField( propertyField );
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

  private SrcExpression getFile()
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

  private SrcAnnotationExpression addSourcePositionAnnotation( FqnCacheNode<String> node )
  {
    return new SrcAnnotationExpression( SourcePosition.class.getSimpleName() )
      .addArgument( new SrcArgument( new SrcMemberAccessExpression( _fqn, FIELD_FILE_URL ) ).name( "url" ) )
      .addArgument( "feature", new SrcType( "String" ), node.getName() )
      .addArgument( "offset", int.class, findOffsetOf( node ) )
      .addArgument( "length", int.class, node.getName() == null ? 0 : node.getName().length() );
  }

  private void addMethods( SrcClass srcClass, FqnCacheNode<String> node )
  {
    if( !node.isLeaf() )
    {
      addGetValueByNameMethod( srcClass, node );

      String userData = node.getUserData();
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

  private void addGetValueByNameMethod( SrcClass srcClass, FqnCacheNode<String> node )
  {
    srcClass.addMethod(
      new SrcMethod( srcClass )
        .name( "getValueByName" )
        .modifiers( Modifier.PUBLIC | (isRootProperty( node ) ? Modifier.STATIC : 0) )
        .returns( new SrcType( "String" ) )
        .addParam( new SrcParameter( "propertyName" ).type( "String" ) )
        .body(
          new SrcStatementBlock()
            .addStatement( makeGetValueBynameSwitch( node ) )
            .addStatement( new SrcReturnStatement( String.class, null ) )
        )
    );

  }

  private boolean isRootProperty( FqnCacheNode<String> node )
  {
    return node.getParent() == null;
  }

  private SrcSwitchStatement makeGetValueBynameSwitch( FqnCacheNode<String> node )
  {
    SrcSwitchStatement stmt = new SrcSwitchStatement();
    stmt.expr( new SrcIdentifier( "propertyName" ) );
    for( FqnCacheNode<String> childNode: node.getChildren() )
    {
      stmt.addCase(
        new SrcSwitchCase( new SrcType( "String" ), childNode.getName() )
          .statement( new SrcReturnStatement( String.class, childNode.getUserData() ) ) );
    }
    return stmt;
  }

  private void addGetValueMethod( SrcClass srcClass, FqnCacheNode<String> node )
  {
    srcClass.addMethod(
      new SrcMethod( srcClass )
        .name( "getValue" )
        .modifiers( Modifier.PUBLIC | (isRootProperty( node ) ? Modifier.STATIC : 0) )
        .returns( new SrcType( "String" ) )
        .body(
          new SrcStatementBlock()
            .addStatement( new SrcReturnStatement( String.class, node.getUserData() ) ) ) );
  }

  private int findOffsetOf( FqnCacheNode<String> node )
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
