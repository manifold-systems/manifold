package manifold.api.fs.def;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileUtil;

public class JavaFileImpl extends JavaResourceImpl implements IFile
{

  public JavaFileImpl( File file )
  {
    super( file );
  }

  @Override
  public InputStream openInputStream() throws IOException
  {
    return new FileInputStream( _file );
  }

  @Override
  public OutputStream openOutputStream() throws IOException
  {
    return new FileOutputStream( _file );
  }

  @Override
  public OutputStream openOutputStreamForAppend() throws IOException
  {
    return new FileOutputStream( _file, true );
  }

  @Override
  public String getExtension()
  {
    return IFileUtil.getExtension( this );
  }

  @Override
  public String getBaseName()
  {
    return IFileUtil.getBaseName( this );
  }

  @Override
  public boolean create()
  {
    try
    {
      return _file.createNewFile();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  @Override
  public boolean exists()
  {
    return _file.isFile();
  }
}
