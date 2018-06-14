package manifold.api.host;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.cache.PathCache;
import manifold.api.type.ITypeManifold;

/**
 * Java projects are typically organized according to a hierarchy of modules where each module defines
 * packages of Java classes and resources and other modules on which it depends. {@link IModule} abstracts
 * this concept as a set of paths defining source, class, and output locations, a list of dependencies on
 * other modules, and the set of {@link ITypeManifold}s managed by the module.
 * <p/>
 * The use of modules for the purposes of the Manifold API is mostly applicable to IDE integration such
 * as with the <a href="http://manifold.systems/docs.html#working-with-intellij">IntelliJ plugin</a>.
 * Otherwise, because compilation is not intermodular and because runtime is flattened, modules consist
 * of a single "default" module.
 */
public interface IModule
{
  String getName();

  /**
   * The path[s] having source files that should be exposed to this module.
   */
  List<IDirectory> getSourcePath();

  List<IDirectory> getJavaClassPath();

  List<IDirectory> getOutputPath();

  IDirectory[] getExcludedPath();

  List<IDirectory> getCollectiveSourcePath();

  List<IDirectory> getCollectiveJavaClassPath();

  IFileSystem getFileSystem();

  /**
   * @return A list of dependency modules.
   * The dependency graph must not have cycles.
   */
  List<Dependency> getDependencies();

  PathCache getPathCache();

  Set<ITypeManifold> getTypeManifolds();

  @SuppressWarnings("unchecked")
  Set<ITypeManifold> findTypeManifoldsFor( String fqn, Predicate<ITypeManifold>... prediates );

  Set<ITypeManifold> findTypeManifoldsFor( IFile file );

  JavaFileObject produceFile( String fqn, DiagnosticListener<JavaFileObject> errorHandler );
}
