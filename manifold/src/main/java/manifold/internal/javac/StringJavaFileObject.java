package manifold.internal.javac;

import java.io.IOException;
import java.net.URI;
import javax.tools.SimpleJavaFileObject;

/**
*/
public class StringJavaFileObject extends SimpleJavaFileObject
{
  private final String _src;

  public StringJavaFileObject( String name, String src )
  {
    super( URI.create( "string:///" + name.replace( '.', '/' ) + Kind.SOURCE.extension ), Kind.SOURCE );
    _src = src;
  }

  @Override
  public CharSequence getCharContent( boolean ignoreEncodingErrors ) throws IOException
  {
    return _src;
  }
}
