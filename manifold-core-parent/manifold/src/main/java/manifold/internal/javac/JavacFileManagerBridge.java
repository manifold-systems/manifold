package manifold.internal.javac;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.util.ReflectUtil;

/**
 * The purpose of this class is to make our ManifoldJavaFileManager a JavacFileManager, which is necessary for
 * straight usage of javac.exe on the command line; other javac usage such as via Maven, Gradle, and more generally
 * via the Java Compilar API do not require our file manager to extend JavacFileManager.  Otherwise, we'd extend
 * ForwardingJavaFileManager.
 */
public class JavacFileManagerBridge<M extends JavaFileManager> extends JavacFileManager
{
  /**
   * The file manager which all methods are delegated to.
   */
  private final M fileManager;

  /**
   * Create a JavacFileManager using a given context, optionally registering
   * it as the JavaFileManager for that context.
   */
  JavacFileManagerBridge( M fileManager, Context context )
  {
    super( context, false, Charset.defaultCharset() );
    this.fileManager = fileManager;
  }

  /**
   */
  public ClassLoader getClassLoader( Location location )
  {
    return fileManager.getClassLoader( location );
  }

  /**
   */
  public Iterable<JavaFileObject> list( Location location,
                                        String packageName,
                                        Set<JavaFileObject.Kind> kinds,
                                        boolean recurse )
    throws IOException
  {
    return fileManager.list( location, packageName, kinds, recurse );
  }

  /**
   */
  public String inferBinaryName( Location location, JavaFileObject file )
  {
    return fileManager.inferBinaryName( location, file );
  }

  /**
   */
  public boolean isSameFile( FileObject a, FileObject b )
  {
    return fileManager.isSameFile( a, b );
  }

  /**
   */
  public boolean handleOption( String current, Iterator<String> remaining )
  {
    return fileManager.handleOption( current, remaining );
  }

  public boolean hasLocation( Location location )
  {
    if( JavacPlugin.IS_JAVA_8 )
    {
      return fileManager.hasLocation( location );
    }

    // Java 9 introduces JavacFileManager#getLocationAsPaths() for validation, but there is a bug in their code
    // where it does not check for empty iterable, which is what we do here
    boolean hasLocation = fileManager.hasLocation( location );
    if( hasLocation )
    {
      Iterable iter = getLocationAsPaths( location );
      if( iter == null || !iter.iterator().hasNext() )
      {
        hasLocation = false;
      }
    }
    return hasLocation;
  }

  // exclusive to Java 9
  public Iterable<? extends Path> getLocationAsPaths( Location location )
  {
    try
    {
      //noinspection unchecked
      return (Iterable)ReflectUtil.method( fileManager, "getLocationAsPaths", Location.class ).invoke( location );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  public int isSupportedOption( String option )
  {
    return fileManager.isSupportedOption( option );
  }

  /**
   */
  public JavaFileObject getJavaFileForInput( Location location,
                                             String className,
                                             JavaFileObject.Kind kind )
    throws IOException
  {
    return fileManager.getJavaFileForInput( location, className, kind );
  }

  /**
   */
  public JavaFileObject getJavaFileForOutput( Location location,
                                              String className,
                                              JavaFileObject.Kind kind,
                                              FileObject sibling )
    throws IOException
  {
    return fileManager.getJavaFileForOutput( location, className, kind, sibling );
  }

  /**
   */
  public FileObject getFileForInput( Location location,
                                     String packageName,
                                     String relativeName )
    throws IOException
  {
    return fileManager.getFileForInput( location, packageName, relativeName );
  }

  /**
   */
  public FileObject getFileForOutput( Location location,
                                      String packageName,
                                      String relativeName,
                                      FileObject sibling )
    throws IOException
  {
    return fileManager.getFileForOutput( location, packageName, relativeName, sibling );
  }

  public void flush()
  {
    try
    {
      fileManager.flush();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public void close()
  {
    try
    {
      fileManager.close();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @since 9
   */
  public Location getLocationForModule( Location location, String moduleName ) throws IOException
  {
    //return fileManager.getLocationForModule(location, moduleName);
    try
    {
      Method getLocationForModule = JavaFileManager.class.getDeclaredMethod( "getLocationForModule", Location.class, String.class );
      return (Location)getLocationForModule.invoke( fileManager, location, moduleName );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @since 9
   */
  public Location getLocationForModule( Location location, JavaFileObject fo ) throws IOException
  {
    //return fileManager.getLocationForModule(location, fo);
    try
    {
      Method getLocationForModule = JavaFileManager.class.getDeclaredMethod( "getLocationForModule", Location.class, JavaFileObject.class );
      return (Location)getLocationForModule.invoke( fileManager, location, fo );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @since 9
   */
  public <S> ServiceLoader<S> getServiceLoader( Location location, Class<S> service ) throws IOException
  {
    //return fileManager.getServiceLoader(location, service);
    try
    {
      Method getServiceLoader = JavaFileManager.class.getDeclaredMethod( "getServiceLoader", Location.class, Class.class );
      //noinspection unchecked
      return (ServiceLoader)getServiceLoader.invoke( fileManager, location, service );
    }
    catch( Exception e )
    {
      throw new IOException( e );
    }
  }

  /**
   * @since 9
   */
  public String inferModuleName( Location location )
  {
    //return fileManager.inferModuleName( location );
    try
    {
      Method inferModuleName = JavaFileManager.class.getDeclaredMethod( "inferModuleName", Location.class );
      return (String)inferModuleName.invoke( fileManager, location );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @since 9
   */
  public Iterable<Set<Location>> listLocationsForModules( Location location ) throws IOException
  {
    //return fileManager.listLocationsForModules( location );
    try
    {
      Method listLocationsForModules = JavaFileManager.class.getDeclaredMethod( "listLocationsForModules", Location.class );
      //noinspection unchecked
      return (Iterable)listLocationsForModules.invoke( fileManager, location );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * @since 9
   */
  public boolean contains( Location location, FileObject fo ) throws IOException
  {
    //return fileManager.contains( location, fo );
    try
    {
      Method contains = JavaFileManager.class.getDeclaredMethod( "contains", Location.class, FileObject.class );
      //noinspection unchecked
      return (boolean)contains.invoke( fileManager, location, fo );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
