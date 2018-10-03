package manifold.api.host;

import java.io.File;
import java.util.List;
import javax.script.Bindings;
import manifold.util.NecessaryEvilUtil;

/**
 * A Manifold host exclusive to the runtime environment.  Responsible for
 * dynamic loading of Manifold types via ClassLoader integration.
 */
public interface IRuntimeManifoldHost extends IManifoldHost
{
  /**
   * Is Manifold bootstrapped?
   */
  boolean isBootstrapped();

  /**
   * Measures to be taken before {@link #bootstrap(List, List)} is invoked.
   */
  default void preBootstrap()
  {
    // reflectively make modules accessible such as java.base and jdk.compiler
    NecessaryEvilUtil.bypassJava9Security();
  }

  /**
   * Bootstrap Manifold before application code executes
   */
  void bootstrap( List<File> sourcepath, List<File> classpath );

  /**
   * Creates a {@link Bindings} implementation for use with scripting language
   * Type Manifolds such as the Javascript Type Manifold.  Default returns
   * a {@link javax.script.SimpleBindings}
   */
  Bindings createBindings();
}
