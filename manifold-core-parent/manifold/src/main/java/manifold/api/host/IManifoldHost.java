package manifold.api.host;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.annotation.processing.ProcessingEnvironment;
import javax.script.Bindings;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFileSystem;
import manifold.api.service.IService;
import manifold.api.type.TypeName;
import manifold.internal.javac.JavacPlugin;
import manifold.util.NecessaryEvilUtil;

/**
 * Implement this interface to host and drive Manifold in custom way.  For instance
 * another JVM language can implement this to expose Manifold types directly to its
 * type system.  Other implementations include IDE plugins for Manifold e.g., the
 * IntelliJ plugin implements this to use Intellij's file system.
 */
public interface IManifoldHost extends IService
{
  /** should this host be used considering the environment e.g., the JavacPlugin context etc.? */
  default boolean accept()
  {
    // Avoid using a host while incrementally compiling the host's own module...

    try
    {
      JavacPlugin javacPlugin = JavacPlugin.instance();
      if( javacPlugin != null )
      {
        List<String> outputPath = javacPlugin.deriveOutputPath();
        for( String path : outputPath )
        {
          String fqn = getClass().getName();
          fqn = fqn.replace( '.', File.separatorChar ) + ".class";
          File classFile = new File( path, fqn );
          if( classFile.isFile() )
          {
            return false;
          }
        }
      }
    }
    catch( IllegalAccessError ignore )
    {
      // this can happend in Java 9 at runtime, we don't care, accept() applies to compilation
    }
    
    return true;
  }

  ClassLoader getActualClassLoader();

  boolean isBootstrapped();
  default void preBootstrap()
  {
    NecessaryEvilUtil.bypassJava9Security();
  }
  void bootstrap( List<File> sourcepath, List<File> classpath );

  IModule getGlobalModule();

  void resetLanguageLevel();

  boolean isPathIgnored( String path );

  String[] getAllReservedWords();

  Bindings createBindings();

  void addTypeLoaderListenerAsWeakRef( Object ctx, ITypeLoaderListener listener );

  JavaFileObject produceFile( String fqn, IModule module, DiagnosticListener<JavaFileObject> errorHandler );

  void maybeAssignManifoldType( ClassLoader loader, String strType, URL url, BiConsumer<String, Supplier<byte[]>> assigner );

  void performLockedOperation( ClassLoader loader, Runnable operation );

  void initializeAndCompileNonJavaFiles( ProcessingEnvironment procEnv, JavaFileManager fileManager, List<String> files, Supplier<Set<String>> sourcePath, Supplier<List<String>> classpath, Supplier<List<String>> outputPath );

  Set<TypeName> getChildrenOfNamespace( String packageName );

  IModule getCurrentModule();

  IFileSystem getFileSystem();
}
