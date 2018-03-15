package manifold.ext.producer.sample;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
import manifold.util.StreamUtil;
import manifold.util.concurrent.LocklessLazyVar;

public class Model implements IModel
{
  private static final String FAVS_EXTENSIONS = "favs.extensions.";
  private static final String FIELD_FILE_URL = "_FILE_URL_";

  final private String _extensionFqn;
  final private Set<IFile> _favsFiles;
  final private LocklessLazyVar<Map<Token, Token>> _mapFavToValue;

  Model( String extensionFqn, Set<IFile> favsFiles )
  {
    _extensionFqn = extensionFqn;
    _favsFiles = new HashSet<>( favsFiles );
    _mapFavToValue = LocklessLazyVar.make( this::buildFavsMap );
  }

  private Map<Token, Token> buildFavsMap()
  {
    // Using LinkedHashMap to preserve insertion order, an impl detail currently required by the IJ plugin for rename
    // refactoring i.e., renaming a json property should result in a source file that differs only in the naming
    // difference -- there should be no difference in ordering of methods etc.
    Map<Token, Token> mapFavToValue = new LinkedHashMap<>();

    for( IFile file : _favsFiles )
    {
      Objects.requireNonNull( file );
      List<List<Token>> rows = tokenize( file, "|" );

      for( List<Token> line : rows )
      {
        Iterator<Token> tokens = line.iterator();
        if( !tokens.hasNext() )
        {
          //## todo: error
          break;
        }
        Token fqn = tokens.next();
        String extensionFqn = makeExtensionClassName( fqn.toString() );
        if( extensionFqn.equals( _extensionFqn ) )
        {
          if( !tokens.hasNext() )
          {
            //## todo: error
            break;
          }
          Token fav = tokens.next();

          if( !tokens.hasNext() )
          {
            //## todo: error
            break;
          }
          Token value = tokens.next();

          mapFavToValue.put( fav, value );
        }
      }
    }
    return mapFavToValue;
  }

  static class Token
  {
    int _pos;
    StringBuilder _value;
    IFile _file;

    Token( int pos, IFile file )
    {
      _value = new StringBuilder();
      _pos = pos;
      _file = file;
    }

    private void append( char c )
    {
      _value.append( c );
    }

    public String toString()
    {
      return _value.toString();
    }
  }

  static class Line
  {
    List<Token> _tokens;
  }

  private static List<List<Token>> tokenize( IFile file, String separatorChars )
  {
    String content;
    try
    {
      content = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }

    List<List<Token>> rows = new ArrayList<>();
    List<Token> row = null;
    Token token = null;
    for( int pos = 0; pos <= content.length(); pos++ )
    {
      char c = pos == content.length() ? 0 : content.charAt( pos );
      if( separatorChars.indexOf( c ) >= 0 ||
          c == '\n' ||
          c == 0 )
      {
        if( token == null )
        {
          // eof
          break;
        }

        row = row == null ? new ArrayList<>() : row;
        row.add( token );
        token = null;

        if( c == '\n' ||
            c == 0 )
        {
          rows.add( row );
          row = null;
        }
      }
      else if( c != '\r' )
      {
        token = token == null ? new Token( pos, file ) : token;
        token.append( c );
      }
    }

    return rows;
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
    _mapFavToValue.clear();
  }

  @Override
  public void removeFile( IFile file )
  {
    _favsFiles.remove( file );
    _mapFavToValue.clear();
  }

  @Override
  public void updateFile( IFile file )
  {
    _favsFiles.remove( file );
    _favsFiles.add( file );
    _mapFavToValue.clear();
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

  public String makeSource( String extensionClassFqn )
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
    for( Map.Entry<Token, Token> entry : _mapFavToValue.get().entrySet() )
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
