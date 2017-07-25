package manifold.api.host;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import javax.script.Bindings;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.service.IService;
import manifold.api.sourceprod.TypeName;

/**
 */
public interface IManifoldHost extends IService
{
  ClassLoader getActualClassLoader();

  boolean isBootstrapped();
  void bootstrap( List<File> sourcepath, List<File> classpath );

  IModule getGlobalModule();

  void resetLanguageLevel();

  boolean isPathIgnored( String path );

  ITypeLoader getLoader( IFile file, IModule module );

  String[] getAllReservedWords();

  Bindings createBindings();

  void addTypeLoaderListenerAsWeakRef( Object ctx, ITypeLoaderListener listener );

  JavaFileObject produceFile( String fqn, IModule module, DiagnosticListener<JavaFileObject> errorHandler );

  void maybeAssignManifoldType( ClassLoader loader, String strType, URL url, BiConsumer<String, Supplier<byte[]>> assigner );

  void performLockedOperation( ClassLoader loader, Runnable operation );

  void initializeAndCompileNonJavaFiles( JavacProcessingEnvironment jpe, JavaFileManager fileManager, List<String> files, Supplier<Set<String>> sourcePath, Supplier<List<String>> classpath, Supplier<String> outputPath );

  Set<TypeName> getChildrenOfNamespace( String packageName );

  IModule getCurrentModule();

  IFileSystem getFileSystem();
}
