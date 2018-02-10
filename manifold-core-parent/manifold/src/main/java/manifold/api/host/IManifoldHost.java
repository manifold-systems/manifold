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
import manifold.util.NecessaryEvilUtil;

/**
 * Implement this interface to host and drive Manifold in custom way.  For instance
 * another JVM language can implement this to expose Manifold types directly to its
 * type system.  Other implementations include IDE plugins for Manifold e.g., the
 * IntelliJ plugin implements this to use Intellij's file system.
 */
public interface IManifoldHost extends IService
{
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
