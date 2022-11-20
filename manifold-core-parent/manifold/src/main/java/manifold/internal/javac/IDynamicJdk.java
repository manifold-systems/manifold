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
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import javax.tools.Diagnostic;
import manifold.util.JreUtil;
import manifold.api.util.PerfLogUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.util.function.Predicate;

/**
 * This interface facilitates JDK API version independence via dynamically compiled Dark Java implementations
 */
public interface IDynamicJdk
{
  <T> void report( Log issueLogger, Diagnostic<? extends T> diagnostic );

  default Iterable<Symbol> getMembers( Symbol.ClassSymbol classSym )
  {
    return getMembers( classSym, true );
  }
  Iterable<Symbol> getMembers( Symbol.ClassSymbol members, boolean completeFirst );

  default Iterable<Symbol> getMembers( Symbol.ClassSymbol classSym, Predicate<Symbol> predicate )
  {
    return getMembers( classSym, predicate, true );
  }
  Iterable<Symbol> getMembers( Symbol.ClassSymbol classSym, Predicate<Symbol> predicate, boolean completeFirst );

  default Iterable<Symbol> getMembersByName( Symbol.ClassSymbol classSym, Name name )
  {
    return getMembersByName( classSym, name, true );
  }
  Iterable<Symbol> getMembersByName( Symbol.ClassSymbol classSym, Name call, boolean completeFirst );

  Symbol.ClassSymbol getTypeElement( Context ctx, Object moduleCtx, String fqn );

  Symbol.ClassSymbol getLoadedClass( Context ctx, String fqn );

  void setOperatorSymbol( Context ctx, JCTree.JCBinary expr, JCTree.Tag tag, String op, Symbol operandType );

  List<Type> getTargets( JCTree.JCLambda tree );
  void setTargets( JCTree.JCLambda tree, List<Type> targets );

  Symbol getOperator( JCTree.JCExpression tree );
  void setOperator( JCTree.JCExpression tree, Symbol.OperatorSymbol operator );

  void logError( Log logger, JCDiagnostic.DiagnosticPosition pos, String key, Object... message );
  void logWarning( Log logger, JCDiagnostic.DiagnosticPosition pos, String key, Object... message );

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
          if( JreUtil.isJava8() ) // Java 8
          {
            fqnIssueReporter = "manifold.internal.javac.JavaDynamicJdk_8";
          }
          else if( JreUtil.isJava11() ) // Java 11
          {
            fqnIssueReporter = "manifold.internal.javac.JavaDynamicJdk_11";
          }
          else if( JreUtil.isJava17orLater() )// Java 17+
          {
            fqnIssueReporter = "manifold.internal.javac.JavaDynamicJdk_17";
          }
          else
          {
            throw new RuntimeException( "Unsupported JDK version " + JreUtil.JAVA_VERSION +
              ". Only LTS versions starting with Java 8 and the latest release are supported." );
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
