package manifold.internal.javac;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.function.Supplier;
import javax.tools.SimpleJavaFileObject;
import manifold.util.concurrent.LocklessLazyVar;

import static manifold.api.sourceprod.ISourceProducer.ARG_DUMP_SOURCE;

/**
 */
public class GeneratedJavaStubFileObject extends SimpleJavaFileObject
{
  private String _name;
  private long _timestamp;
  private Supplier<String> _sourceSupplier;
  private LocklessLazyVar<String> _src = LocklessLazyVar.make( () -> _sourceSupplier.get() );

  public GeneratedJavaStubFileObject( String name, Supplier<String> sourceSupplier )
  {
    super( URI.create( "genstub:///" + name.replace( '.', '/' ) + Kind.SOURCE.extension ), Kind.SOURCE );
    _name = name.replace( '.', '/' ) + Kind.SOURCE.extension;
    _timestamp = System.currentTimeMillis();
    _sourceSupplier = sourceSupplier;
  }

  @Override
  public URI toUri()
  {
    return URI.create( "genstub:///" + getName() );
  }

  @Override
  public String getName()
  {
    return _name;
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
  public CharSequence getCharContent( boolean ignoreEncodingErrors ) throws IOException
  {
    String source = _src.get();
    maybeDumpSource( source );
    return source;
  }

  private void maybeDumpSource( String source )
  {
    if( !shouldDumpSource() )
    {
      System.out.println( "\n================\n" );
      System.out.println( getName() );
      System.out.println( "\n================\n" );
      System.out.println( source );
    }
  }

  private static Boolean _dumpSource;
  private boolean shouldDumpSource()
  {
    return _dumpSource == null
           ? System.getProperty( ARG_DUMP_SOURCE, "" ).equalsIgnoreCase( "false" )
           : _dumpSource;
  }

  @Override
  public Writer openWriter() throws IOException
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLastModified()
  {
    return _timestamp;
  }

  @Override
  public boolean delete()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals( Object o )
  {
    if( !(o instanceof GeneratedJavaStubFileObject) )
    {
      return false;
    }
    return _name.equals( ((GeneratedJavaStubFileObject)o).getName() );
  }

  @Override
  public int hashCode()
  {
    return _name.hashCode();
  }

  @Override
  public Kind getKind()
  {
    return Kind.SOURCE;
  }

  @Override
  public boolean isNameCompatible( String simpleName, Kind kind )
  {
    return true;
  }
}
