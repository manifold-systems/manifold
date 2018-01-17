package manifold.internal.javac;

import com.sun.source.util.JavacTask;
//import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.lang.model.element.TypeElement;
//import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import manifold.api.type.ITypeManifold;
import manifold.api.type.ITypeProcessor;
import manifold.internal.host.ManifoldHost;

/**
 */
public class TypeProcessor extends CompiledTypeProcessor
{
  TypeProcessor( JavacTask javacTask )
  {
    super( javacTask );
  }

//  @Override
//  public boolean filterError( Diagnostic diagnostic )
//  {
//    for( ITypeManifold sp: ManifoldHost.getCurrentModule().getTypeManifolds() )
//    {
//      if( sp instanceof ITypeProcessor )
//      {
//        if( ((ITypeProcessor)sp).filterError( this, diagnostic ) )
//        {
//          return true;
//        }
//      }
//    }
//    return false;
//  }

  @Override
  public void process( TypeElement element, IssueReporter<JavaFileObject> issueReporter )
  {
    if( IDynamicJdk.isInitializing() )
    {
      // avoid re-entry of dynamic jdk construction
      return;
    }

    for( ITypeManifold sp : ManifoldHost.getCurrentModule().getTypeManifolds() )
    {
      if( sp instanceof ITypeProcessor )
      {
        //JavacProcessingEnvironment.instance( getContext() ).getMessager().printMessage( Diagnostic.Kind.NOTE, "Processing: " + element.getQualifiedName() );

        try  
        {
          ((ITypeProcessor)sp).process( element, this, issueReporter );
        }
        catch( Throwable e )
        {
          StringWriter stackTrace = new StringWriter();
          e.printStackTrace( new PrintWriter( stackTrace ) );
          issueReporter.reportError( "Fatal error processing with Manifold type processor: " + sp.getClass().getName() +
                                     "\non type: " + element.getQualifiedName() +
                                     "\nPlease report the error with the accompanying stack trace.\n" + stackTrace );
          throw e;
        }
      }
    }
  }
}
