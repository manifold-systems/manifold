package manifold.api.type;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import manifold.internal.javac.IssueReporter;
import manifold.internal.javac.TypeProcessor;

/**
 * Implementors of this interface can examine and rewrite the Java AST during compilation.
 */
public interface ITypeProcessor
{
  /**
   * A typical implementation creates a {@link com.sun.tools.javac.tree.TreeTranslator} and
   * visits the tree in context e.g.,
   * <pre>
   *   TreeTranslator visitor = new ExtensionTransformer( this, typeProcessor );
   *   typeProcessor.getTree().accept( visitor );
   * </pre>
   */
  void process( TypeElement fqn, TypeProcessor typeProcessor, IssueReporter<JavaFileObject> issueReporter );

  //boolean filterError( TypeProcessor typeProcessor, Diagnostic diagnostic );
}
