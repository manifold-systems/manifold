package manifold.ext.producer.sample;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcAnnotationExpression;
import manifold.api.gen.SrcArgument;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMemberAccessExpression;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcParameter;
import manifold.api.gen.SrcRawExpression;
import manifold.api.gen.SrcReturnStatement;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.type.IModel;
import manifold.api.type.SourcePosition;
import manifold.ext.api.Extension;
import manifold.ext.api.This;
import manifold.util.ManClassUtil;

public class Model implements IModel
{
  private static final String FAVS_EXTENSIONS = "favs.extensions.";
  private static final String FIELD_FILE_URL = "_FILE_URL_";

  final private String _extensionFqn;
  final private Set<IFile> _favsFiles;

  Model( String extensionFqn, Set<IFile> favsFiles )
  {
    _extensionFqn = extensionFqn;
    _favsFiles = new HashSet<>( favsFiles );
  }

  @Override
  public String getFqn()
  {
    return _extensionFqn;
  }

  @Override
  public Set<IFile> getFiles()
  {
    return _favsFiles;
  }

  @Override
  public void addFile( IFile file )
  {
    _favsFiles.add( file );
  }

  @Override
  public void removeFile( IFile file )
  {
    _favsFiles.remove( file );
  }

  @Override
  public void updateFile( IFile file )
  {
    _favsFiles.remove( file );
    _favsFiles.add( file );
  }

  static String makeExtensionClassName( String extendedClassname )
  {
    String simpleName = ManClassUtil.getShortClassName( extendedClassname );
    return FAVS_EXTENSIONS + extendedClassname + ".My" + simpleName + "Ext";
  }

  static String deriveExtendedClassFrom( String extensionClassName )
  {
    if( extensionClassName.startsWith( FAVS_EXTENSIONS ) )
    {
      String extended = extensionClassName.substring( FAVS_EXTENSIONS.length() );
      int iDot = extended.lastIndexOf( '.' );
      return extended.substring( 0, iDot );
    }
    return null;
  }

  public String makeSource( String extensionClassFqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    SrcClass srcClass = new SrcClass( extensionClassFqn, SrcClass.Kind.Class )
      .addAnnotation( new SrcAnnotationExpression( Extension.class ) )
      .modifiers( Modifier.PUBLIC );
    int i = 0;
    for( IFile file : _favsFiles )
    {
      srcClass.addField(
        new SrcField( FIELD_FILE_URL + i++, String.class )
          .initializer( new SrcRawExpression( String.class, file.getPath().getFileSystemPathString() ) )
          .modifiers( Modifier.STATIC | Modifier.FINAL ) );
    }
    //noinspection ConstantConditions
    for( Map.Entry<Token, Token> entry : FavsParser.instance().parseFavsForType( _favsFiles, _extensionFqn, errorHandler ).entrySet() )
    {
      SrcMethod method = new SrcMethod()
        .modifiers( Modifier.PUBLIC | Modifier.STATIC )
        .name( "favorite" + entry.getKey() )
        .addParam(
          new SrcParameter( "thiz", deriveExtendedClassFrom( extensionClassFqn ) )
            .addAnnotation( new SrcAnnotationExpression( This.class ) ) )
        .returns( String.class )
        .body( new SrcStatementBlock()
          .addStatement(
            new SrcReturnStatement( String.class, entry.getValue()._value.toString() ) ) );
      method.addAnnotation( makeSourcePositionAnnotation( entry.getKey() ) );
      srcClass.addMethod( method );
    }
    return srcClass.render().toString();
  }

  private SrcAnnotationExpression makeSourcePositionAnnotation( Token token )
  {
    int i = getFileIndex( token );
    return new SrcAnnotationExpression( SourcePosition.class.getName() )
      .addArgument( new SrcArgument( new SrcMemberAccessExpression( ManClassUtil.getShortClassName( _extensionFqn ), FIELD_FILE_URL + i ) ).name( "url" ) )
      .addArgument( "feature", String.class, token._value.toString() )
      .addArgument( "kind", String.class, "favorite" )
      .addArgument( "offset", int.class, token._pos )
      .addArgument( "length", int.class, token._value.length() );
  }

  private int getFileIndex( Token token )
  {
    int i = 0;
    for( IFile file : _favsFiles )
    {
      if( file.equals( token._file ) )
      {
        break;
      }
      i++;
    }
    return i;
  }
}
