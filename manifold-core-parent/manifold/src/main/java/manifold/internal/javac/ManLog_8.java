package manifold.internal.javac;

import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
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
      Object diagnosticHandler = ReflectUtil.field( log, "diagnosticHandler" ).get();
      log = new ManLog_8( ctx );
      ReflectUtil.field( log, "diagnosticHandler" ).set( diagnosticHandler );
    }

    return log;
  }

  private ManLog_8( Context ctx )
  {
    super( ctx );
    _suspendedIssues = new HashMap<>();
    _extensionTransformerClass = LocklessLazyVar.make(
      () -> ReflectUtil.type( "manifold.ext.ExtensionTransformer" ) );
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
    else
    {
      super.error( pos, key, args );
    }
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