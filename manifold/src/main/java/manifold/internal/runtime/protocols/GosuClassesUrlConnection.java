/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.internal.runtime.protocols;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import manifold.internal.host.ManifoldHost;
import manifold.internal.javac.InMemoryClassJavaFileObject;
import manifold.internal.javac.JavaCompileIssuesException;
import manifold.internal.javac.JavaParser;
import manifold.internal.javac.StringJavaFileObject;
import manifold.util.Pair;

/**
 */
public class GosuClassesUrlConnection extends URLConnection
{
  private static final String[] JAVA_NAMESPACES_TO_IGNORE = {
    "java/", "javax/", "sun/"
  };
  private static final String META_INF_MANIFEST_MF = "META-INF/MANIFEST.MF";
  private static final ThreadLocal<Map<String, Supplier<String>>> _proxySupplierByFqn = ThreadLocal.withInitial( HashMap::new );

  public static Supplier<String> getProxySupplier( String fqnProxy )
  {
    return _proxySupplierByFqn.get().get( fqnProxy );
  }

  public static void putProxySupplier( String fqnProxy, Supplier<String> supplier )
  {
    _proxySupplierByFqn.get().put( fqnProxy, supplier );
  }

  public static void removeProxySupplier( String fqnProxy )
  {
    _proxySupplierByFqn.get().remove( fqnProxy );
  }

  private Supplier<byte[]> _bytecodeSupplier;
  private JavaFileObject _javaSrcFile;
  private Supplier<String> _proxySupplier;
  private String _javaFqn;

  private ClassLoader _loader;
  private boolean _bDirectory;
  private boolean _bInvalid;

  GosuClassesUrlConnection( URL url )
  {
    super( url );
  }

  @Override
  public void connect() throws IOException
  {
    if( _bInvalid )
    {
      throw new IOException();
    }
    connectImpl();
    if( _bInvalid )
    {
      throw new IOException();
    }
  }

  private boolean connectImpl()
  {
    if( _bInvalid )
    {
      return false;
    }
    if( _bytecodeSupplier == null && _javaSrcFile == null && _proxySupplier == null && !_bDirectory )
    {
      //noinspection deprecation
      String strPath = URLDecoder.decode( getURL().getPath() );
      String strClass = strPath.substring( 1 );
      if( isManifest( strClass ) )
      {
        // Some tools (Equinox) expect to find a jar manifest file in the path entry, so we fake an empty one here
        return true;
      }
      if( !ignoreJavaClass( strClass ) )
      {
        String strType = strClass.replace( '/', '.' );
        int iIndexClass = strType.lastIndexOf( ".class" );
        if( iIndexClass > 0 )
        {
          strType = strType.substring( 0, iIndexClass ).replace( '$', '.' );
          maybeAssignGosuType( findClassLoader( getURL().getHost() ), strType );
        }
        else if( strPath.endsWith( "/" ) )
        {
          _bDirectory = true;
        }
      }
      _bInvalid = _bytecodeSupplier == null && _javaSrcFile == null && _proxySupplier == null && !_bDirectory;
    }
    return !_bInvalid;
  }

  private boolean isManifest( String strClass )
  {
    return strClass.equalsIgnoreCase( META_INF_MANIFEST_MF );
  }

  private ClassLoader findClassLoader( String host )
  {
    int identityHash = Integer.parseInt( host );
    ClassLoader loader = ManifoldHost.getActualClassLoader();
    while( loader != null )
    {
      if( System.identityHashCode( loader ) == identityHash )
      {
        return loader;
      }
      loader = loader.getParent();
    }
    throw new IllegalStateException( "Can't find ClassLoader with identity hash: " + identityHash );
  }

  private void maybeAssignGosuType( ClassLoader loader, String strType )
  {
    Supplier<String> proxySupplier = getProxySupplier( strType );
    if( proxySupplier != null )
    {
      removeProxySupplier( strType );
      _proxySupplier = proxySupplier;
      _javaFqn = strType;
      _loader = loader;
      return;
    }

    ManifoldHost.maybeAssignGosuType( loader, strType, getURL(), ( fqn, type ) ->
    {
      if( fqn != null )
      {
        // If there were a class file for the Java type on disk, it would have loaded by now (the gosuclass protocol is last).
        // Therefore we compile and load the java class from the Java source file, eventually a JavaType based on the resulting class
        // may load, if a source-based one hasn't already loaded.
        try
        {
          Pair<JavaFileObject, String> pair = JavaParser.instance().findJavaSource( fqn, null );
          if( pair != null )
          {
            _javaSrcFile = pair.getFirst();
            _javaFqn = fqn;
            _loader = loader;
          }
        }
        catch( NoClassDefFoundError e )
        {
          // tools.jar likely not in the path...
          System.out.println( "\n!!! Unable to dynamically compile Java from source.  tools.jar is likely missing from classpath.\n" );
        }
      }
      else if( type != null )
      {
        _bytecodeSupplier = type;
        _loader = loader;
      }
    } );
  }

  private boolean ignoreJavaClass( String strClass )
  {
    for( String namespace : JAVA_NAMESPACES_TO_IGNORE )
    {
      if( strClass.startsWith( namespace ) )
      {
        return true;
      }
    }
    return false;
  }

  @Override
  public InputStream getInputStream() throws IOException
  {
    if( _bytecodeSupplier != null || _javaSrcFile != null || _proxySupplier != null )
    {
      // Avoid compiling until the bytes are actually requested;
      // sun.misc.URLClassPath grabs the inputstream twice, the first time is for practice :)
      return new LazyByteArrayInputStream();
    }
    else if( _bDirectory )
    {
      return new ByteArrayInputStream( new byte[0] );
    }
    else if( getURL().getPath().toUpperCase().endsWith( META_INF_MANIFEST_MF ) )
    {
      return new ByteArrayInputStream( new byte[0] );
    }
    throw new IOException( "Invalid or missing Gosu class for: " + url.toString() );
  }

  public boolean isValid()
  {
    return connectImpl();
  }

  class LazyByteArrayInputStream extends InputStream
  {
    byte _buf[];
    int _pos;
    int _mark;
    int _count;

    private void init()
    {
      if( _buf == null )
      {
        ManifoldHost.performLockedOperation( _loader, () ->
        {
          //System.out.println( "Compiling: " + _type.getName() );
          if( _bytecodeSupplier != null )
          {
            _buf = _bytecodeSupplier.get();
          }
          else if( _javaSrcFile != null )
          {
            _buf = compileJavaClass();
          }
          else if( _proxySupplier != null )
          {
            _buf = compileProxyClass( _proxySupplier.get() );
          }
          _pos = 0;
          _count = _buf.length;
        } );
      }
    }

//    private void logExceptionForFailedCompilation( Throwable e )
//    {
//      // Log the exception, it tends to get swallowed esp. if the class doesn't parse.
//      //
//      // Note this is sometimes OK because the failure is recoverable. For example,
//      // a Gosu class references a Java class which in turn extends the Gosu class.
//      // Due the the circular reference at the header level, the Java compiler will
//      // fail to compile the Gosu class via this Url loader (because the Gosu class
//      // needs the Java class, which is compiling). In this case the DefaultTypeLoader
//      // catches the exception and generates a Java stub for the Gosu class and returns
//      // that as the definitive JavaClassInfo.  Thus, we don't really want to log
//      // a nasty message here or print the stack trace, if it's recoverable.
//
//      //System.out.println( "!! Failed to compile: " + _type.getName() + " (don't worry, these are mostly recoverable, mostly)" );
//      //e.printStackTrace();
//    }

    private byte[] compileJavaClass()
    {
      DiagnosticCollector<JavaFileObject> errorHandler = new DiagnosticCollector<>();
      InMemoryClassJavaFileObject cls = JavaParser.instance().compile( _javaFqn, Arrays.asList( "-g", "-nowarn", "-Xlint:none", "-proc:none", "-parameters" ), errorHandler );
      if( cls != null )
      {
        return cls.getBytes();
      }
      throw new JavaCompileIssuesException( _javaFqn, errorHandler );
    }

    private byte[] compileProxyClass( String source )
    {
      DiagnosticCollector<JavaFileObject> errorHandler = new DiagnosticCollector<>();
      StringJavaFileObject fileObj = new StringJavaFileObject( _javaFqn, source );
      InMemoryClassJavaFileObject cls = JavaParser.instance().compile( fileObj, _javaFqn, Arrays.asList( "-g", "-nowarn", "-Xlint:none", "-proc:none", "-parameters" ), errorHandler );
      if( cls != null )
      {
        return cls.getBytes();
      }
      throw new JavaCompileIssuesException( _javaFqn, errorHandler );
    }

    public int read()
    {
      init();
      return (_pos < _count) ? (_buf[_pos++] & 0xff) : -1;
    }

    @Override
    public int read( byte[] b ) throws IOException
    {
      init();
      return super.read( b );
    }

    public int read( byte b[], int off, int len )
    {
      init();
      if( b == null )
      {
        throw new NullPointerException();
      }
      else if( off < 0 || len < 0 || len > b.length - off )
      {
        throw new IndexOutOfBoundsException();
      }
      if( _pos >= _count )
      {
        return -1;
      }
      if( _pos + len > _count )
      {
        len = _count - _pos;
      }
      if( len <= 0 )
      {
        return 0;
      }
      System.arraycopy( _buf, _pos, b, off, len );
      _pos += len;
      return len;
    }

    public long skip( long n )
    {
      if( _pos + n > _count )
      {
        n = _count - _pos;
      }
      if( n < 0 )
      {
        return 0;
      }
      _pos += n;
      return n;
    }

    public int available()
    {
      init();
      return _count - _pos;
    }

    public boolean markSupported()
    {
      return true;
    }

    public void mark( int readAheadLimit )
    {
      _mark = _pos;
    }

    public void reset()
    {
      _pos = _mark;
    }

    public void close() throws IOException
    {
    }
  }
}
