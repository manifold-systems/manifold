package manifold.internal.javac;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

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
  protected final M fileManager;

  /**
   * Create a JavacFileManager using a given context, optionally registering
   * it as the JavaFileManager for that context.
   */
  public JavacFileManagerBridge( M fileManager, Context context )
  {
    super( null, false, Charset.defaultCharset() );
    this.fileManager = fileManager;
    log = Log.instance( context );
  }

  @Override
  public void setContext( Context context )
  {
    if( context != null )
    {
      super.setContext( context );
      log = Log.instance( context );
    }
  }

  /**
   * @throws SecurityException {@inheritDoc}
   * @throws IllegalStateException {@inheritDoc}
   */
  public ClassLoader getClassLoader(Location location) {
    return fileManager.getClassLoader(location);
  }

  /**
   * @throws IOException {@inheritDoc}
   * @throws IllegalStateException {@inheritDoc}
   */
  public Iterable<JavaFileObject> list( Location location,
                                        String packageName,
                                        Set<JavaFileObject.Kind> kinds,
                                        boolean recurse)
    throws IOException
  {
    return fileManager.list(location, packageName, kinds, recurse);
  }

  /**
   * @throws IllegalStateException {@inheritDoc}
   */
  public String inferBinaryName(Location location, JavaFileObject file) {
    return fileManager.inferBinaryName(location, file);
  }

  /**
   * @throws IllegalArgumentException {@inheritDoc}
   */
  public boolean isSameFile( FileObject a, FileObject b) {
    return fileManager.isSameFile(a, b);
  }

  /**
   * @throws IllegalArgumentException {@inheritDoc}
   * @throws IllegalStateException {@inheritDoc}
   */
  public boolean handleOption(String current, Iterator<String> remaining) {
    return fileManager.handleOption(current, remaining);
  }

  public boolean hasLocation(Location location) {
    return fileManager.hasLocation(location);
  }

  public int isSupportedOption(String option) {
    return fileManager.isSupportedOption(option);
  }

  /**
   * @throws IllegalArgumentException {@inheritDoc}
   * @throws IllegalStateException {@inheritDoc}
   */
  public JavaFileObject getJavaFileForInput(Location location,
                                            String className,
                                            JavaFileObject.Kind kind)
    throws IOException
  {
    return fileManager.getJavaFileForInput(location, className, kind);
  }

  /**
   * @throws IllegalArgumentException {@inheritDoc}
   * @throws IllegalStateException {@inheritDoc}
   */
  public JavaFileObject getJavaFileForOutput(Location location,
                                             String className,
                                             JavaFileObject.Kind kind,
                                             FileObject sibling)
    throws IOException
  {
    return fileManager.getJavaFileForOutput(location, className, kind, sibling);
  }

  /**
   * @throws IllegalArgumentException {@inheritDoc}
   * @throws IllegalStateException {@inheritDoc}
   */
  public FileObject getFileForInput(Location location,
                                    String packageName,
                                    String relativeName)
    throws IOException
  {
    return fileManager.getFileForInput(location, packageName, relativeName);
  }

  /**
   * @throws IllegalArgumentException {@inheritDoc}
   * @throws IllegalStateException {@inheritDoc}
   */
  public FileObject getFileForOutput(Location location,
                                     String packageName,
                                     String relativeName,
                                     FileObject sibling)
    throws IOException
  {
    return fileManager.getFileForOutput(location, packageName, relativeName, sibling);
  }

  public void flush() {
    try
    {
      fileManager.flush();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public void close(){
    try
    {
      fileManager.close();
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

}
