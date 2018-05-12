package manifold.internal.javac;

import com.sun.source.util.JavacTask;
//import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.api.BasicJavacTask;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.lang.model.element.TypeElement;
//import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import manifold.api.type.ICompilerComponent;
import manifold.api.type.ITypeManifold;
import manifold.api.type.ITypeProcessor;
import manifold.internal.host.ManifoldHost;
import manifold.internal.javac.templ.StringLiteralTemplateProcessor;
import manifold.util.ServiceUtil;

/**
 */
public class TypeProcessor extends CompiledTypeProcessor
{
  TypeProcessor( JavacTask javacTask )
  {
    super( javacTask );
    loadCompilerComponents( (BasicJavacTask)javacTask );
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

  private void loadCompilerComponents( BasicJavacTask javacTask )
  {
    SortedSet<ICompilerComponent> compilerComponents = new TreeSet<>( Comparator.comparing( c -> c.getClass().getTypeName() ) );
    loadBuiltin( compilerComponents );
    ServiceUtil.loadRegisteredServices( compilerComponents, ICompilerComponent.class, getClass().getClassLoader() );
    compilerComponents.forEach( cc -> cc.init( javacTask ) );
  }

  private void loadBuiltin( SortedSet<ICompilerComponent> compilerComponents )
  {
    if( JavacPlugin.instance() == null || JavacPlugin.instance().isStringTemplatesEnabled() )
    {
      // string templates are Disabled by default, enable feature with "-Xplugin:Manifold strings"
      // note, string templates are Enabled by default if compiling dynamically
      compilerComponents.add( new StringLiteralTemplateProcessor() );
    }
  }

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
