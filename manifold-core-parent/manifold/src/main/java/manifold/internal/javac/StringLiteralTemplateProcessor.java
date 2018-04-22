package manifold.internal.javac;

import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import manifold.api.type.DisableStringLiteralTemplates;

public class StringLiteralTemplateProcessor extends TreeTranslator implements TaskListener
{
  private final BasicJavacTask _javacTask;
  private final TreeMaker _maker;
  private final Names _names;

  public static void register( JavacTask task )
  {
    task.addTaskListener( new StringLiteralTemplateProcessor( task ) );
  }

  private StringLiteralTemplateProcessor( JavacTask task )
  {
    _javacTask = (BasicJavacTask)task;
    _maker = TreeMaker.instance( _javacTask.getContext() );
    _names = Names.instance( _javacTask.getContext() );
  }

  @Override
  public void started( TaskEvent taskEvent )
  {
    // nothing to do
  }

  @Override
  public void finished( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.PARSE )
    {
      return;
    }

    for( Tree tree : e.getCompilationUnit().getTypeDecls() )
    {
      if( !(tree instanceof JCTree.JCClassDecl) )
      {
        continue;
      }
      
      JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl)tree;
      for( JCTree.JCAnnotation anno: classDecl.getModifiers().getAnnotations() )
      {
        if( anno.annotationType.toString().contains( DisableStringLiteralTemplates.class.getSimpleName() ) )
        {
          return;
        }
      }
      classDecl.accept( this );
    }
  }

  @Override
  public void visitLiteral( JCTree.JCLiteral jcLiteral )
  {
    super.visitLiteral( jcLiteral );

    Object value = jcLiteral.getValue();
    if( !(value instanceof String) )
    {
      return;
    }

    String stringValue = (String)value;
    List<JCTree.JCExpression> exprs = parse( stringValue, jcLiteral.getPreferredPosition() );
    JCTree.JCBinary concat = null;
    while( !exprs.isEmpty() )
    {
      if( concat == null )
      {
        concat = _maker.Binary( JCTree.Tag.PLUS, exprs.remove( 0 ), exprs.remove( 0 ) );
      }
      else
      {
        concat = _maker.Binary( JCTree.Tag.PLUS, concat, exprs.remove( 0 ) );
      }
    }

    result = concat == null ? result : concat;
  }

  public List<JCTree.JCExpression> parse( String stringValue, int literalOffset )
  {
    List<StringLiteralTemplateParser.Expr> comps = StringLiteralTemplateParser.parse( stringValue );
    if( comps.isEmpty() )
    {
      return Collections.emptyList();
    }

    List<JCTree.JCExpression> exprs = new ArrayList<>();
    StringLiteralTemplateParser.Expr prev = null;
    for( StringLiteralTemplateParser.Expr comp : comps )
    {
      JCTree.JCExpression expr;
      if( comp.isVerbatim() )
      {
        expr = _maker.Literal( comp.getExpr() );
      }
      else
      {
        if( prev != null && !prev.isVerbatim() )
        {
          // force concatenation
          exprs.add( _maker.Literal( "" ) );
        }

        int exprPos = literalOffset + 1 + comp.getOffset();

        if( comp.isIdentifier() )
        {
          JCTree.JCIdent ident = _maker.Ident( _names.fromString( comp.getExpr() ) );
          ident.pos = exprPos;
          expr = ident;
        }
        else
        {
          DiagnosticCollector<JavaFileObject> errorHandler = new DiagnosticCollector<>();
          expr = JavaParser.instance().parseExpr( comp.getExpr(), errorHandler );
          if( transferParseErrors( literalOffset, comp, expr, errorHandler ) )
          {
            return Collections.emptyList();
          }
          replaceNames( expr, exprPos );
        }
      }
      prev = comp;
      exprs.add( expr );
    }

    if( exprs.size() == 1 )
    {
      // insert an empty string so concat will make the expr a string
      exprs.add( 0, _maker.Literal( "" ) );
    }

    return exprs;
  }

  private boolean transferParseErrors( int literalOffset, StringLiteralTemplateParser.Expr comp, JCTree.JCExpression expr, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    if( expr == null || errorHandler.getDiagnostics().stream().anyMatch( e -> e.getKind() == Diagnostic.Kind.ERROR ) )
    {
      //## todo: add errors reported in the expr
      //Log.instance( _javacTask.getContext() ).error( new JCDiagnostic.SimpleDiagnosticPosition( literalOffset + 1 + comp._offset ),  );
      for( Diagnostic<? extends JavaFileObject> diag : errorHandler.getDiagnostics() )
      {
        if( diag.getKind() == Diagnostic.Kind.ERROR )
        {
          JCDiagnostic jcDiag = ((ClientCodeWrapper.DiagnosticSourceUnwrapper)diag).d;
//                JCDiagnostic.Factory.instance( _javacTask.getContext() ).error(
//                  Log.instance( _javacTask.getContext() ).currentSource(),
//                  new JCDiagnostic.SimpleDiagnosticPosition( literalOffset + 1 + comp._offset ), diag.getCode(), jcDiag.getArgs() );
          Log.instance( _javacTask.getContext() ).error( new JCDiagnostic.SimpleDiagnosticPosition( literalOffset + 1 + comp.getOffset() ), diag.getCode(), jcDiag.getArgs() );
        }
      }
      return true;
    }
    return false;
  }

  private void replaceNames( JCTree.JCExpression expr, int offset )
  {
    expr.accept( new NameReplacer( _javacTask, offset ) );
  }
}
