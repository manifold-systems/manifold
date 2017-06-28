package manifold.api.properties;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import manifold.api.fs.IFile;
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
import manifold.api.sourceprod.SourcePosition;
import manifold.util.StreamUtil;
import manifold.util.cache.FqnCache;
import manifold.util.cache.FqnCacheNode;


/**
 */
public class PropertiesCodeGen
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

  public SrcClass make()
  {
    SrcClass srcClass = new SrcClass( _fqn, SrcClass.Kind.Class ).imports( SourcePosition.class );

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
        .type( String.class )
        .initializer( getFile() ) );
  }

  public SrcClass make( SrcClass srcClass, FqnCacheNode<String> node )
  {
    for( FqnCacheNode<String> childNode : node.getChildren() )
    {
      SrcType type = new SrcType( childNode.isLeaf() ? "String" : childNode.getName() );
      SrcField propertyField = new SrcField( srcClass )
        .name( childNode.getName() )
        .modifiers( Modifier.PUBLIC | Modifier.STATIC )
        .type( type )
        .initializer( childNode.isLeaf()
                      ? new SrcRawExpression( String.class, childNode.getUserData() )
                      : new SrcRawExpression( "new " + type + "()" ) );
      if( _file != null )
      {
        propertyField.addAnnotation( addSourcePositionAnnotation( childNode ) );
      }
      srcClass.addField( propertyField );
      if( !childNode.isLeaf() )
      {
        SrcClass innerSrcClass = new SrcClass( childNode.getName(), srcClass, SrcClass.Kind.Class )
          .modifiers( Modifier.PUBLIC | Modifier.STATIC );
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
      return new SrcRawExpression( String.class, _file.toURI().toURL().toString() );
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
      .addArgument( "feature", String.class, node.getFqn() )
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
        .returns( String.class )
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
        .returns( String.class )
        .addParam( new SrcParameter( "propertyName" ).type( String.class ) )
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
    for( FqnCacheNode<String> childNode : node.getChildren() )
    {
      stmt.addCase(
        new SrcSwitchCase( String.class, childNode.getName() )
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
        .returns( String.class )
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
    return _content.indexOf( fqn );
  }

  private String assignContent()
  {
    if( _file != null )
    {
      try( InputStream inputStream = _file.openInputStream() )
      {
        return StreamUtil.getContent( new InputStreamReader( inputStream ) ).replace( "\r\n", "\n" );
      }
      catch( Exception e )
      {
        throw new RuntimeException( e );
      }
    }
    return null;
  }
}
