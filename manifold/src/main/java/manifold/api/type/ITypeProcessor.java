package manifold.api.type;

import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import manifold.internal.javac.IssueReporter;
import manifold.internal.javac.TypeProcessor;

/**
 */
public interface ITypeProcessor
{
  void process( TypeElement fqn, TypeProcessor typeProcessor, IssueReporter<JavaFileObject> issueReporter );
  //boolean filterError( TypeProcessor typeProcessor, Diagnostic diagnostic );
}
