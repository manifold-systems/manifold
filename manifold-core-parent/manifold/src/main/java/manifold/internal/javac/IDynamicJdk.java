/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Filter;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import javax.tools.Diagnostic;
import manifold.util.JreUtil;
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

  List<Type> getTargets( JCTree.JCLambda tree );
  void setTargets( JCTree.JCLambda tree, List<Type> targets );

  void logError( Log logger, JCDiagnostic.DiagnosticPosition pos, String key, Object... message );

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
            fqnIssueReporter = "manifold.internal.javac.JavaDynamicJdk_8";
          }
          else if( JreUtil.JAVA_VERSION < 11 ) // Java 9 & 10
          {
            fqnIssueReporter = "manifold.internal.javac.JavaDynamicJdk_9";
          }
          else // Java 11 or later
          {
            fqnIssueReporter = "manifold.internal.javac.JavaDynamicJdk_11";
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
