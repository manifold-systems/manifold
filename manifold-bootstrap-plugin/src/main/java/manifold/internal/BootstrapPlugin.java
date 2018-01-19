package manifold.internal;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

/**
 * Opens jdk.compiler module programmatically. For use with Java 9+, does nothing when used with Java 8.
 */
public class BootstrapPlugin implements Plugin, TaskListener
{
  private Context _ctx;
  private boolean _done;

  @Override
  public String getName()
  {
    return "BootstrapPlugin";
  }

  @Override
  public void init( JavacTask task, String... args )
  {
    _ctx = ((BasicJavacTask)task).getContext();
    task.addTaskListener( this );
  }

  @Override
  public void started( TaskEvent te )
  {
    if( JreUtil.isJava8() )
    {
      return;
    }

    CompilationUnitTree compilationUnit = te.getCompilationUnit();
    if( !(compilationUnit instanceof JCTree.JCCompilationUnit) )
    {
      return;
    }

    if( _done )
    {
      return;
    }

    _done = true;

    openModule( _ctx, "jdk.compiler" );
  }

  @Override
  public void finished( TaskEvent e )
  {

  }

  public static void openModule( Context context, String moduleName )
  {
    try
    {
      Symbol moduleToOpen = (Symbol)ReflectUtil.method( Symtab.instance( context ), "getModule", Name.class )
        .invoke( Names.instance( context ).fromString( moduleName ) );

      if( moduleToOpen == null )
      {
        // not modular java 9
        return;
      }

      moduleToOpen.complete();
      
      Set<Symbol> rootModules = (Set<Symbol>)ReflectUtil.field(
        ReflectUtil.method( ReflectUtil.type( "com.sun.tools.javac.comp.Modules" ), "instance", Context.class ).invokeStatic( context ), "allModules" ).get();

      for( Symbol rootModule: rootModules )
      {
        rootModule.complete();

        List<Object> requires = (List<Object>)ReflectUtil.field( rootModule, "requires" ).get();
        List<Object> newRequires = new ArrayList( requires );
        Object addedRequires = ReflectUtil.constructor( "com.sun.tools.javac.code.Directive$RequiresDirective", ReflectUtil.type( "com.sun.tools.javac.code.Symbol$ModuleSymbol" ) ).newInstance( moduleToOpen );
        newRequires.add( addedRequires );
        requires = com.sun.tools.javac.util.List.from( newRequires );
        ReflectUtil.field( rootModule, "requires" ).set( requires );

        List<Object> exports = new ArrayList<>( (Collection)ReflectUtil.field( moduleToOpen, "exports" ).get() );
        for( Symbol pkg : (Iterable<Symbol>)ReflectUtil.field( moduleToOpen, "enclosedPackages" ).get() )
        {
          if( pkg instanceof Symbol.PackageSymbol )
          {
            //System.err.println( "PACKAGE: " + pkg );
            Object exp = ReflectUtil.constructor( "com.sun.tools.javac.code.Directive$ExportsDirective", Symbol.PackageSymbol.class, com.sun.tools.javac.util.List.class ).newInstance( pkg,
              com.sun.tools.javac.util.List.of( rootModule ) );
            exports.add( exp );

            ((Map)ReflectUtil.field( rootModule, "visiblePackages" ).get()).put( ((Symbol.PackageSymbol)pkg).fullname, pkg );
          }
        }
        ReflectUtil.field( moduleToOpen, "exports" ).set( com.sun.tools.javac.util.List.from( exports ) );

        Set readModules = (Set)ReflectUtil.field( moduleToOpen, "readModules" ).get();
        readModules.add( rootModule );
        ReflectUtil.field( moduleToOpen, "readModules" ).set( readModules );
      }

    }
    catch( Throwable e )
    {
      System.err.println( "Failed to reflectively add-exports " + moduleName + "/* to root module[s], you must add the following argument to jave.exe:\n" +
                          "  --add-exports=" + moduleName + "/*=<root-module>\n" );
      throw new RuntimeException( e );
    }
  }
}
