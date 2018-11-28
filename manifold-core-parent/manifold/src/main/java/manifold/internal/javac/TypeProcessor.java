package manifold.internal.javac;

import com.sun.tools.javac.api.BasicJavacTask;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import manifold.api.host.IManifoldHost;
import manifold.api.type.ICompilerComponent;
import manifold.api.type.ITypeManifold;
import manifold.api.type.ITypeProcessor;
import manifold.internal.javac.templ.StringLiteralTemplateProcessor;
import manifold.util.ServiceUtil;
import manifold.util.concurrent.ConcurrentHashSet;

/**
 */
public class TypeProcessor extends CompiledTypeProcessor
{
  private Map<File, Set<String>> _typesCompiledByFile;
  private Set<Object> _drivers;

  TypeProcessor( IManifoldHost host, BasicJavacTask javacTask )
  {
    super( host, javacTask );
    _typesCompiledByFile = new ConcurrentHashMap<>();
    _drivers = new ConcurrentHashSet<>();
    loadCompilerComponents( javacTask );
  }

//  @Override
//  public boolean filterError( Diagnostic diagnostic )
//  {
//    for( ITypeManifold sp: RuntimeManifoldHost.get().getSingleModule().getTypeManifolds() )
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

  public Map<File, Set<String>> getTypesCompiledByFile()
  {
    return _typesCompiledByFile;
  }

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
      compilerComponents.add( new StringLiteralTemplateProcessor( this ) );
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

    for( ITypeManifold sp: getHost().getSingleModule().getTypeManifolds() )
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

  public void addDrivers( Set<Object> drivers )
  {
    _drivers.addAll( drivers );
  }
  public Set<Object> getDrivers()
  {
    return _drivers;
  }
}
