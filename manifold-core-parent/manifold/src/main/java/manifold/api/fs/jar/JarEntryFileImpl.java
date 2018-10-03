package manifold.api.fs.jar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;

public class JarEntryFileImpl extends JarEntryResourceImpl implements IFile
{

  public JarEntryFileImpl( IFileSystem fs, String name, IJarFileDirectory parent, JarFileDirectoryImpl jarFile )
  {
    super( fs, name, parent, jarFile );
  }

  @Override
  public InputStream openInputStream() throws IOException
  {
    if( _entry == null )
    {
      throw new IOException();
    }
    return _jarFile.getInputStream( _entry );
  }

  @Override
  public OutputStream openOutputStream()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public OutputStream openOutputStreamForAppend()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getExtension()
  {
    int lastDot = _name.lastIndexOf( "." );
    if( lastDot != -1 )
    {
      return _name.substring( lastDot + 1 );
    }
    else
    {
      return "";
    }
  }

  @Override
  public String getBaseName()
  {
    int lastDot = _name.lastIndexOf( "." );
    if( lastDot != -1 )
    {
      return _name.substring( 0, lastDot );
    }
    else
    {
      return _name;
    }
  }

  @Override
  public boolean isInJar()
  {
    return true;
  }

  @Override
  public boolean create()
  {
    throw new RuntimeException( "Not supported" );
  }
}
