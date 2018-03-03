package manifold.api.type;

import java.util.Collection;
import java.util.List;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.host.IModuleComponent;

/**
 * A {@link ITypeManifold} is a fundamental component of the Manifold API. Implementors of this interface
 * work together to dynamically provide the Java compiler with type information as it requests it.
 * <p/>
 * Its primary duties include:
 * <lu>
 *   <li>Define a domain of types via {@link #getTypeNames(String)}</li>
 *   <li>Resolve types in that domain via {@link #isType(String)}, {@link #isTopLevelType(String)}, and {@link #isPackage(String)}</li>
 *   <li>Contribute source toward a given type projection via {@link #contribute(String, String, DiagnosticListener)}</li>
 * </lu>
 * <p/>
 * Separate instances of a given implementation of this interface exist per {@link manifold.api.host.IModule}.
 */
public interface ITypeManifold extends IFileConnected
{
  /**
   *  System property to aid in debugging generated source.
   */
  String ARG_DUMP_SOURCE = "manifold.dump.source";

  /**
   * Initialize this type manifold.  Avoid defining types in the scope of this method.
   *
   * @param tl The module to which this type manifold belongs
   */
  void init( IModuleComponent tl );

  /**
   * The TypeLoader to which this producer is scoped
   */
  IModuleComponent getTypeLoader();

  /**
   * What kind of source is produced?  Java or Gosu?
   */
  ISourceKind getSourceKind();

  /**
   * How does this producer contribute toward the source file produced
   */
  ContributorKind getContributorKind();

  /**
   * Does this producer supply source for the specified fqn?
   */
  boolean isType( String fqn );

  boolean isTopLevelType( String fqn );

  /**
   * Verifies whether or not the specified package may be provided by this source producer
   */
  boolean isPackage( String pkg );

  /**
   * What kind of type corresponds with fqn?
   */
  ClassType getClassType( String fqn );

  /**
   * What is the package name for the specified fqn?
   */
  String getPackage( String fqn );

  /**
   * Contribute source corresponding with the fqn.
   */
  String contribute( String fqn, String existing, DiagnosticListener<JavaFileObject> errorHandler );

  Collection<String> getAllTypeNames();

  Collection<TypeName> getTypeNames( String namespace );

  List<IFile> findFilesForType( String fqn );

  /**
   * Clear all cached data
   */
  void clear();
}
