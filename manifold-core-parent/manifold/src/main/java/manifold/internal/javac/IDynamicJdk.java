package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Filter;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import javax.tools.Diagnostic;
import manifold.util.PerfLogUtil;
import manifold.util.concurrent.LocklessLazyVar;

/**
 * This interface facilitates JDK API version independence via dynamically compiled Dark Java implementations
 */
public interface IDynamicJdk
{
  <T> void report( Log issueLogger, Diagnostic<? extends T> diagnostic );

  Iterable<Symbol> getMembers( Symbol.ClassSymbol members );

  Iterable<Symbol> getMembers( Symbol.ClassSymbol classSym, Filter<Symbol> filter );

  Iterable<Symbol> getMembersByName( Symbol.ClassSymbol classSym, Name call );

  Symbol.ClassSymbol getTypeElement( Context ctx, Object moduleCtx, String fqn );

  Symbol.ClassSymbol getLoadedClass( Context ctx, String fqn );


  class Instance
  {
    private static boolean INITIALIZING = false;

    private static LocklessLazyVar<IDynamicJdk> INSTANCE = LocklessLazyVar.make( () -> {
      INITIALIZING = true;
      try
      {
        long before = System.nanoTime();
        try
        {
          String fqnIssueReporter;
          if( JavacPlugin.IS_JAVA_8 )
          {
            fqnIssueReporter = "manifold.internal.javac.Java8DynamicJdk";
          }
          else
          {
            fqnIssueReporter = "manifold.internal.javac.Java9DynamicJdk";
          }
          return (IDynamicJdk)Class.forName( fqnIssueReporter ).newInstance();
        }
        finally
        {
          PerfLogUtil.log( "Dynamic JDK Time", before );
        }
      }
      catch( Exception e )
      {
        throw new RuntimeException( e );
      }
      finally
      {
        INITIALIZING = false;
      }
    } );
  }

  static IDynamicJdk instance()
  {
    return Instance.INSTANCE.get();
  }

  static boolean isInitializing()
  {
    return Instance.INITIALIZING;
  }
}
