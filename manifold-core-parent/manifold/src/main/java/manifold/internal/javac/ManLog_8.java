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

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.DeferredAttr;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Flow;
import com.sun.tools.javac.comp.Infer;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.jvm.ClassWriter;
import com.sun.tools.javac.jvm.Gen;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.MandatoryWarningHandler;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import manifold.util.ReflectUtil;
import manifold.util.Stack;
import manifold.util.concurrent.LocklessLazyVar;

public class ManLog_8 extends Log
{
  private Map<DiagnosticHandler, LinkedHashMap<JCTree, Stack<Stack<JCDiagnostic>>>> _suspendedIssues;
  private LocklessLazyVar<Class<?>> _extensionTransformerClass;

  public static Log instance( Context ctx )
  {
    Log log = ctx.get( logKey );
    if( !(log instanceof ManLog_8) )
    {
      ctx.put( logKey, (Log)null );
      log = new ManLog_8( ctx,
        (DiagnosticHandler)ReflectUtil.field( log, "diagnosticHandler" ).get(),
        log.currentSource(),
        (PrintWriter)ReflectUtil.field( log, "errWriter" ).get(),
        (PrintWriter)ReflectUtil.field( log, "warnWriter" ).get(),
        (PrintWriter)ReflectUtil.field( log, "noticeWriter" ).get() );
    }

    return log;
  }

  private ManLog_8( Context ctx, DiagnosticHandler diagnosticHandler, DiagnosticSource source,
                    PrintWriter errWriter, PrintWriter warnWriter, PrintWriter noticeWriter )
  {
    super( ctx, errWriter, warnWriter, noticeWriter );
    ReflectUtil.field( this, "diagnosticHandler" ).set( diagnosticHandler );
    try
    {
      ReflectUtil.field( diagnosticHandler, "this$0" ).set( this );
    }
    catch( Exception ignore )
    {
    }
    ReflectUtil.field( this, "source" ).set( source );
    _suspendedIssues = new HashMap<>();
    _extensionTransformerClass = LocklessLazyVar.make(
      () -> ReflectUtil.type( "manifold.ext.ExtensionTransformer" ) );
    reassignLog( ctx );
  }

  private void reassignLog( Context ctx )
  {
    Object[] earlyAttrHolders = {
      Annotate.instance( ctx ),
      Check.instance( ctx ),
      ClassReader.instance( ctx ),
      ClassWriter.instance( ctx ),
      DeferredAttr.instance( ctx ),
      Enter.instance( ctx ),
      Flow.instance( ctx ),
      Gen.instance( ctx ),
      Infer.instance( ctx ),
      JavaCompiler.instance( ctx ),
      JavacProcessingEnvironment.instance( ctx ),
      JavacTrees.instance( ctx ),
      MemberEnter.instance( ctx ),
      Resolve.instance( ctx ),
//      LambdaToMethod.instance( ctx ),
//      Lower.instance( ctx ),
//      MemberEnter.instance( ctx ),
//      TransTypes.instance( ctx ),
//      TypeAnnotations.instance( ctx ),
    };
    for( Object instance: earlyAttrHolders )
    {
      ReflectUtil.LiveFieldRef l = ReflectUtil.WithNull.field( instance, "log" );
      if( l != null )
      {
        l.set( this );
      }
    }

    // Reassign Log fields
    // Note this is only relevant when compiling with annotation processors

    // Also reassign the 'log' fields in Check's various MandatoryWarningHandlers...
    for( Field f: Check.class.getDeclaredFields() )
    {
      if( MandatoryWarningHandler.class.isAssignableFrom( f.getType() ) )
      {
        f.setAccessible( true );
        try
        {
          Object mwh = f.get( Check.instance( ctx ) );
          ReflectUtil.field( mwh, "log" ).set( this );
        }
        catch( IllegalAccessException e )
        {
          throw new RuntimeException( e );
        }
      }
    }
  }

  @Override
  public void popDiagnosticHandler( DiagnosticHandler handler )
  {
    super.popDiagnosticHandler( handler );
    _suspendedIssues.remove( handler );
  }

  public void error( JCDiagnostic.DiagnosticPosition pos, String key, Object... args )
  {
    //noinspection StatementWithEmptyBody
    if( pos instanceof JCTree.JCFieldAccess &&
        ("cant.assign.val.to.final.var".equals( key ) ||
         "var.might.already.be.assigned".equals( key )) &&
        isJailbreakSelect( (JCTree.JCFieldAccess)pos ) )
    {
      // For @Jailbreak assignments, change error to warning re final var assignment
      //## todo: the error message can't be converted to a warning, make up a custom warning
      // report( diags.warning( source, pos, key, args ) );
    }
    else if( !isSuppressedCheckedExceptionError( key ) )
    {
      super.error( pos, key, args );
    }
  }

  private boolean isSuppressedCheckedExceptionError( String key )
  {
    return JavacPlugin.instance() != null &&
           JavacPlugin.instance().isCheckedExceptionsOff() &&
           key != null &&
           (key.contains( "unreported.exception." ) ||
            key.contains( "incompatible.thrown.types" ));
  }

  private DiagnosticHandler getDiagnosticHandler()
  {
    return (DiagnosticHandler)ReflectUtil.field( this, "diagnosticHandler" ).get();
  }

  @Override
  public void report( JCDiagnostic issue )
  {
    LinkedHashMap<JCTree, Stack<Stack<JCDiagnostic>>> suspendedIssues =
      _suspendedIssues.get( getDiagnosticHandler() );
    if( suspendedIssues == null || suspendedIssues.isEmpty() )
    {
      super.report( issue );
    }
    else
    {
      JCTree last = null;
      for( JCTree key: suspendedIssues.keySet() )
      {
        last = key;
      }
      suspendedIssues.get( last ).peek().push( issue );
    }
  }

  boolean isJailbreakSelect( JCTree.JCFieldAccess pos )
  {
    if( _extensionTransformerClass.get() == null )
    {
      return false;
    }

    //noinspection ConstantConditions
    return (boolean)ReflectUtil.method( _extensionTransformerClass.get(), "isJailbreakReceiver",
      JCTree.JCFieldAccess.class ).invokeStatic( pos );
  }

  void pushSuspendIssues( JCTree tree )
  {
    LinkedHashMap<JCTree, Stack<Stack<JCDiagnostic>>> suspendedIssues =
      _suspendedIssues.computeIfAbsent( getDiagnosticHandler(), k -> new LinkedHashMap<>() );
    Stack<Stack<JCDiagnostic>> issues = suspendedIssues.get( tree );
    if( issues == null )
    {
      suspendedIssues.put( tree, issues = new Stack<>() );
    }
    issues.push( new Stack<>() );
  }

  void popSuspendIssues( JCTree tree )
  {
    LinkedHashMap<JCTree, Stack<Stack<JCDiagnostic>>> suspendedIssues =
      _suspendedIssues.get( getDiagnosticHandler() );

    if( suspendedIssues.isEmpty() )
    {
      // found method in superclass, already recorded any issues from that attempt
      return;
    }

    Stack<Stack<JCDiagnostic>> issueFrames = suspendedIssues.get( tree );
    if( issueFrames.size() == 1 )
    {
      if( isRootFrame( tree ) )
      {
        recordRecentSuspendedIssuesAndRemoveOthers( tree );
      }
    }
    else
    {
      issueFrames.pop();
    }
  }

  void recordRecentSuspendedIssuesAndRemoveOthers( JCTree tree )
  {
    LinkedHashMap<JCTree, Stack<Stack<JCDiagnostic>>> suspendedIssues =
      _suspendedIssues.get( getDiagnosticHandler() );

    Stack<Stack<JCDiagnostic>> issues = suspendedIssues.get( tree );
    Stack<JCDiagnostic> currentIssues = issues.pop();
    issues.clear();
    issues.push( currentIssues );
    if( isRootFrame( tree ) )
    {
      recordSuspendedIssues();
      suspendedIssues.clear();
    }
  }

  private void recordSuspendedIssues()
  {
    LinkedHashMap<JCTree, Stack<Stack<JCDiagnostic>>> suspendedIssues =
      _suspendedIssues.get( getDiagnosticHandler() );

    for( Map.Entry<JCTree, Stack<Stack<JCDiagnostic>>> entry: suspendedIssues.entrySet() )
    {
      Stack<Stack<JCDiagnostic>> issueFrames = entry.getValue();
      Stack<JCDiagnostic> issueFrame = issueFrames.pop();
      if( !issueFrames.isEmpty() )
      {
        throw new IllegalStateException( "Invalid issue frames, should be only one frame" );
      }
      for( JCDiagnostic d: issueFrame )
      {
        super.report( d );
      }
    }
  }

  private boolean isRootFrame( JCTree tree )
  {
    LinkedHashMap<JCTree, Stack<Stack<JCDiagnostic>>> suspendedIssues =
      _suspendedIssues.get( getDiagnosticHandler() );
    return suspendedIssues.keySet().iterator().next() == tree;
  }
}