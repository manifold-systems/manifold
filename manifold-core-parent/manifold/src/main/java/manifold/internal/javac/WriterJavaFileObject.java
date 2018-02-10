package manifold.internal.javac;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;
import manifold.api.fs.IDirectory;
import manifold.internal.host.ManifoldHost;

/**
 * A utility for other compilers hosting Manifold, primarily for exposing class files as JavaFileObjects
 * where APIs require it.
 */
public class WriterJavaFileObject extends SimpleJavaFileObject
{
  private OutputStream _outputStream;

  public WriterJavaFileObject( String fqn )
  {
    super( getUriFrom( fqn ), Kind.CLASS );
  }

  public WriterJavaFileObject( String pkg, String filename )
  {
    super( getUriFrom( pkg, filename ), Kind.OTHER );
  }

  private static URI getUriFrom( String fqn )
  {
    final String outRelativePath = fqn.replace( '.', File.separatorChar ) + ".class";
    IDirectory outputPath = ManifoldHost.getGlobalModule().getOutputPath().stream().findFirst().orElse( null );
    File file = new File( outputPath.getPath().getFileSystemPathString(), outRelativePath );
    return file.toURI();
  }

  private static URI getUriFrom( String fqn, String filename )
  {
    final String outRelativePath = fqn.replace( '.', File.separatorChar ) + File.separatorChar + filename;
    IDirectory outputPath = ManifoldHost.getGlobalModule().getOutputPath().stream().findFirst().orElse( null );
    File file = new File( outputPath.getPath().getFileSystemPathString(), outRelativePath );
    return file.toURI();
  }

  @Override
  public OutputStream openOutputStream() throws IOException
  {
    throwIfInUse();
    synchronized( this )
    {
      throwIfInUse();
      File file = new File( toUri() );
      if( !file.isFile() )
      {
        //noinspection ResultOfMethodCallIgnored
        file.getParentFile().mkdirs();
        //noinspection ResultOfMethodCallIgnored
        file.createNewFile();
      }
      return _outputStream = new BufferedOutputStream( new FileOutputStream( file ) );
    }
  }

  private void throwIfInUse() throws IOException
  {
    if( _outputStream != null )
    {
      throw new IOException( "OutputStream in use" );
    }
  }

  @Override
  public InputStream openInputStream() throws IOException
  {
    throw new UnsupportedOperationException();
  }
}
