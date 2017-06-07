package manifold.internal.javac;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import manifold.api.sourceprod.ISourceProducer;
import manifold.api.sourceprod.ITypeProcessor;
import manifold.internal.host.ManifoldHost;

/**
 */
public class TypeProcessor extends CompiledTypeProcessor
{
  TypeProcessor( JavacTask javacTask )
  {
    super( javacTask );
  }

  @Override
  public void process( TypeElement element, TreePath tree, IssueReporter<JavaFileObject> issueReporter )
  {
    String fqn = element.getQualifiedName().toString();
    for( ISourceProducer sp: ManifoldHost.getCurrentModule().getSourceProducers() )
    {
      if( sp instanceof ITypeProcessor )
      {
        JavacProcessingEnvironment.instance( getContext() ).getMessager().printMessage( Diagnostic.Kind.NOTE, "Processing: " + element.getQualifiedName() );

        ((ITypeProcessor)sp).process( fqn, this, issueReporter );
      }
    }
  }
}
