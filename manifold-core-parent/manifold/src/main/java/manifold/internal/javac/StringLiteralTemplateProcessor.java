package manifold.internal.javac;

import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
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

  public static void register( JavacTask task )
  {
    task.addTaskListener( new StringLiteralTemplateProcessor( task ) );
  }

  private StringLiteralTemplateProcessor( JavacTask task )
  {
    _javacTask = (BasicJavacTask)task;
    _maker = TreeMaker.instance( _javacTask.getContext() );
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
    List<JCTree.JCExpression> exprs = new TemplateParser( stringValue ).parse();
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

  class TemplateParser
  {
    private String _stringValue;
    private int _index;
    private StringBuilder _contentExpr;

    TemplateParser( String stringValue )
    {
      _stringValue = stringValue;
    }

    public List<JCTree.JCExpression> parse()
    {
      List<Expr> comps = split();
      if( comps.isEmpty() )
      {
        return Collections.emptyList();
      }

      List<JCTree.JCExpression> exprs = new ArrayList<>();
      Expr prev = null;
      for( Expr comp: comps )
      {
        JCTree.JCExpression expr;
        if( comp._literal )
        {
          expr = _maker.Literal( comp._expr );
        }
        else
        {
          if( prev != null && !prev._literal )
          {
            // force concatenation
            exprs.add( _maker.Literal( "" ) );
          }

          DiagnosticCollector<JavaFileObject> errorHandler = new DiagnosticCollector<>();
          expr = JavaParser.instance().parseExpr( comp._expr, errorHandler );
          if( expr == null || errorHandler.getDiagnostics().stream().anyMatch( e -> e.getKind() == Diagnostic.Kind.ERROR ) )
          {
            //## todo: add errors reported in the expr as warnings in the source
            return Collections.emptyList();
          }
          replaceNames( expr );
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

    private void replaceNames( JCTree.JCExpression expr )
    {
      expr.accept( new NameReplacer( _javacTask ) );
    }

    private List<Expr> split()
    {
      List<Expr> comps = new ArrayList<>();
      _contentExpr = new StringBuilder();
      int length = _stringValue.length();
      for( _index = 0; _index < length; _index++ )
      {
        char c = _stringValue.charAt( _index );
        if( c == '$' )
        {
          String expr = parseExpr();
          if( expr != null && !expr.isEmpty() )
          {
            if( _contentExpr.length() > 0 )
            {
              // add
              comps.add( new Expr( _contentExpr.toString(), true ) );
              _contentExpr = new StringBuilder();
            }
            comps.add( new Expr( expr, false ) );
            continue;
          }
        }
        _contentExpr.append( c );
      }

      if( !comps.isEmpty() && _contentExpr.length() > 0 )
      {
        comps.add( new Expr( _contentExpr.toString(), true ) );
      }

      return comps;
    }

    private String parseExpr()
    {
      if( _index + 1 == _stringValue.length() )
      {
        return null;
      }

      return _stringValue.charAt( _index + 1 ) == '{'
             ? parseBraceExpr()
             : parseSimpleExpr();
    }

    private String parseBraceExpr()
    {
      int length = _stringValue.length();
      StringBuilder expr = new StringBuilder();
      for( int index = _index+2; index < length; index++ )
      {
        char c = _stringValue.charAt( index );
        if( c != '}' )
        {
          expr.append( c );
        }
        else
        {
          _index = index;
          return expr.length() > 0 ? expr.toString() : null;
        }
      }
      return null;
    }

    private String parseSimpleExpr()
    {
      int length = _stringValue.length();
      StringBuilder expr = new StringBuilder();
      int index;
      for( index = _index+1; index < length; index++ )
      {
        char c = _stringValue.charAt( index );
        if( expr.length() == 0 )
        {
          if( c != '$' && Character.isJavaIdentifierStart( c ) )
          {
            expr.append( c );
          }
          else
          {
            return null;
          }
        }
        else if( c != '$' && Character.isJavaIdentifierPart( c ) )
        {
          expr.append( c );
        }
        else
        {
          break;
        }
        _index = index;
      }
      return expr.length() > 0 ? expr.toString() : null;
    }

    class Expr
    {
      String _expr;
      boolean _literal;

      Expr( String expr, boolean literal )
      {
        _expr = expr;
        _literal = literal;
      }
    }
  }
}
