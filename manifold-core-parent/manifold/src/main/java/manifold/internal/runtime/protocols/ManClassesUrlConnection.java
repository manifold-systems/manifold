/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.internal.runtime.protocols;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import manifold.api.type.ISelfCompiledFile;
import manifold.api.type.ITypeManifold;
import manifold.internal.host.RuntimeManifoldHost;
import manifold.internal.javac.InMemoryClassJavaFileObject;
import manifold.internal.javac.JavaCompileIssuesException;
import manifold.internal.javac.StringJavaFileObject;
import manifold.api.util.Pair;
import manifold.api.util.PerfLogUtil;

/**
 */
public class ManClassesUrlConnection extends URLConnection
{
  private static final boolean DUMP_CLASSFILES = false;

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

  private JavaFileObject _javaSrcFile;
  private Supplier<String> _proxySupplier;
  private String _javaFqn;

  private boolean _bDirectory;
  private boolean _bInvalid;

  ManClassesUrlConnection( URL url )
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
    if( _javaSrcFile == null && _proxySupplier == null && !_bDirectory )
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
          maybeAssignType( findClassLoader( getURL().getHost() ), strType );
        }
        else if( strPath.endsWith( "/" ) )
        {
          _bDirectory = true;
        }
      }
      _bInvalid = _javaSrcFile == null && _proxySupplier == null && !_bDirectory;
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
    ClassLoader loader = RuntimeManifoldHost.get().getActualClassLoader();
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

  private void maybeAssignType( ClassLoader loader, String strType )
  {
    Supplier<String> proxySupplier = getProxySupplier( strType );
    if( proxySupplier != null )
    {
      removeProxySupplier( strType );
      _proxySupplier = proxySupplier;
      _javaFqn = strType;
      return;
    }

    Set<ITypeManifold> sps = RuntimeManifoldHost.get().getSingleModule().findTypeManifoldsFor( strType );
    if( !sps.isEmpty() )
    {
      if( strType != null )
      {
        // If there were a class file for the Java type on disk, it would have loaded by now (the manifoldclass protocol is last).
        // Therefore we compile and load the java class from the Java source file, eventually a JavaType based on the resulting class
        // may load, if a source-based one hasn't already loaded.
        try
        {
          Pair<JavaFileObject, String> pair = RuntimeManifoldHost.get().getJavaParser().findJavaSource( strType, new DiagnosticCollector<>() );
          if( pair != null )
          {
            _javaSrcFile = pair.getFirst();
            _javaFqn = strType;
          }
        }
        catch( NoClassDefFoundError e )
        {
          // tools.jar likely not in the path...
          System.out.println( "\n!!! Unable to dynamically compile Java from source.  tools.jar is likely missing from classpath.\n" );
        }
      }
    }
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
    if( _javaSrcFile != null || _proxySupplier != null )
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
    throw new IOException( "Invalid or missing Manifold class for: " + url.toString() );
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
        //System.out.println( "Compiling: " + _type.getName() );
//        if( _bytecodeSupplier != null )
//        {
//          _buf = _bytecodeSupplier.get();
//        }
        if( _javaSrcFile != null )
        {
          _buf = compileJavaClass();
        }
        else if( _proxySupplier != null )
        {
          _buf = compileProxyClass( _proxySupplier.get() );
        }
        else
        {
          throw new IllegalStateException();
        }
        _pos = 0;
        _count = _buf.length;
        writeClassFile_Debug();
      }
    }

    private void writeClassFile_Debug()
    {
      if( !DUMP_CLASSFILES )
      {
        return;
      }

      File tmpDir = new File( System.getProperty( "java.io.tmpdir" ) );
      File classesDir = new File( tmpDir, "manifold" + File.separator + "classes" );
      //noinspection ResultOfMethodCallIgnored
      classesDir.mkdirs();
      int iLastDot = _javaFqn.lastIndexOf( '.' );
      String fileName;

      if( iLastDot > 0 )
      {
        classesDir = new File( classesDir, _javaFqn.substring( 0, iLastDot ).replace( '.', File.separatorChar ) );
        //noinspection ResultOfMethodCallIgnored
        classesDir.mkdirs();
        fileName = _javaFqn.substring( iLastDot + 1 ) + ".class";
      }
      else
      {
        fileName = _javaFqn + ".class";
      }
      File classFile = new File( classesDir, fileName );
      try
      {
        //noinspection ResultOfMethodCallIgnored
        classFile.createNewFile();
        FileOutputStream os = new FileOutputStream( classFile );
        os.write( _buf );
        os.close();
      }
      catch( IOException e )
      {
        throw new RuntimeException( e );
      }
    }

    private byte[] compileJavaClass()
    {
      long before = System.nanoTime();
      try
      {
        if( _javaSrcFile instanceof ISelfCompiledFile && ((ISelfCompiledFile)_javaSrcFile).isSelfCompile( _javaFqn ) )
        {
          return ((ISelfCompiledFile)_javaSrcFile).compile( _javaFqn );
        }
        else
        {
          DiagnosticCollector<JavaFileObject> errorHandler = new DiagnosticCollector<>();
          InMemoryClassJavaFileObject cls = RuntimeManifoldHost.get().getJavaParser().compile( _javaFqn,
            Arrays.asList( "-source", "8", "-g", "-nowarn", "-Xlint:none", "-proc:none", "-parameters" ), errorHandler );
          if( cls != null )
          {
            return cls.getBytes();
          }
          throw new JavaCompileIssuesException( _javaFqn, errorHandler );
        }
      }
      catch( Throwable t )
      {
        t.printStackTrace();
        throw t;
      }
      finally
      {
        PerfLogUtil.log( "compileJavaClass() " + _javaFqn, before );
      }
    }

    private byte[] compileProxyClass( String source )
    {
      long before = System.nanoTime();
      try
      {
        DiagnosticCollector<JavaFileObject> errorHandler = new DiagnosticCollector<>();
        StringJavaFileObject fileObj = new StringJavaFileObject( _javaFqn, source );
        InMemoryClassJavaFileObject cls = RuntimeManifoldHost.get().getJavaParser().compile( fileObj, _javaFqn,
          Arrays.asList( "-source", "8", "-g", "-nowarn", "-Xlint:none", "-proc:none", "-parameters" ), errorHandler );
        if( cls != null )
        {
          return cls.getBytes();
        }
        throw new JavaCompileIssuesException( _javaFqn, errorHandler );
      }
      finally
      {
        PerfLogUtil.log( "compileProxyClass() " + _javaFqn, before );
      }
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

    public void close()
    {
    }
  }
}
