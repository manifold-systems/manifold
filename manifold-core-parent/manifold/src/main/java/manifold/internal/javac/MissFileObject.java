package manifold.internal.javac;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

public class MissFileObject implements JavaFileObject
{
  @Override
  public URI toUri()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getName()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public InputStream openInputStream() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public OutputStream openOutputStream() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Reader openReader( boolean ignoreEncodingErrors ) throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public CharSequence getCharContent( boolean ignoreEncodingErrors ) throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Writer openWriter() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLastModified()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean delete()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Kind getKind()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNameCompatible( String simpleName, Kind kind )
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public NestingKind getNestingKind()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Modifier getAccessLevel()
  {
    throw new UnsupportedOperationException();
  }
}
