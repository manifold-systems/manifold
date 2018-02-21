package manifold.internal.javac;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import javax.tools.JavaFileManager;
import javax.tools.SimpleJavaFileObject;
import manifold.util.PathUtil;
import manifold.util.StreamUtil;

/**
 */
public class SourceJavaFileObject extends SimpleJavaFileObject
{
  private CharSequence _content;
  private String _fqn;

  public SourceJavaFileObject( URI uri )
  {
    this( uri, true );
  }
  public SourceJavaFileObject( URI uri, boolean preload )
  {
    super( uri, Kind.SOURCE );

    //!! Note we preload because some environments (maven cough) reuse closed URLClassLoaders from earlier Javac runs,
    //!! which will barf when trying to load any classes not already loaded e.g., those loaded during getCharContent() below
    if( preload )
    {
      try
      {
        _content = getCharContent( true );
      }
      catch( IOException ignore )
      {
        _content = "";
      }
    }
  }

  public SourceJavaFileObject( String filename )
  {
    super( PathUtil.create( filename ).toUri(), Kind.SOURCE );
  }

  public String getFqn()
  {
    return _fqn;
  }
  public void setFqn( String fqn )
  {
    _fqn = fqn;
  }

  @Override
  public CharSequence getCharContent( boolean ignoreEncodingErrors ) throws IOException
  {
    if( _content != null )
    {
      return _content;
    }

    Path file = PathUtil.create( uri );
    try( BufferedReader reader = PathUtil.createReader( file ) )
    {
      return _content = StreamUtil.getContent( reader );
    }
  }

  @SuppressWarnings("WeakerAccess")
  public String inferBinaryName( JavaFileManager.Location location )
  {
    if( _fqn == null )
    {
      throw new IllegalStateException( "Null class name" );
    }
    return _fqn;
  }
}
