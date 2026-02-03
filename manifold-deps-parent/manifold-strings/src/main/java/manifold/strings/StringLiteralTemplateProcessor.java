/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.strings;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.IntPredicate;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import manifold.api.type.ICompilerComponent;
import manifold.internal.javac.StringTemplateDiagnosticHandler;
import manifold.rt.api.util.ServiceUtil;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.TypeProcessor;
import manifold.rt.api.DisableStringLiteralTemplates;
import manifold.rt.api.util.Stack;
import manifold.strings.api.ITemplateProcessorGate;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.LocklessLazyVar;

public class StringLiteralTemplateProcessor extends TreeTranslator implements ICompilerComponent, TaskListener
{
  public static final String SIMPLE_EXPR_DISABLED = "manifold.strings.simple.disabled";

  private TypeProcessor _tp;
  private BasicJavacTask _javacTask;
  private Stack<Boolean> _disabled;
  private StringTemplateDiagnosticHandler _manDiagnosticHandler;
  private SortedSet<ITemplateProcessorGate> _processorGates;
  private LocklessLazyVar<Boolean> _isSimpleExprDisabled;

  @Override
  public void init( BasicJavacTask javacTask, TypeProcessor typeProcessor )
  {
    _tp = typeProcessor;
    _javacTask = javacTask;
    _disabled = new Stack<>();
    _disabled.push( false );
    _isSimpleExprDisabled = LocklessLazyVar.make( () -> isSimpleExprDisabled() );

    javacTask.addTaskListener( this );

    loadTemplateProcessorGates();
  }

  private boolean isSimpleExprDisabled()
  {
    //noinspection resource
    JavacProcessingEnvironment jpe = JavacProcessingEnvironment.instance( _javacTask.getContext() );
    String value = jpe.getOptions().get( SIMPLE_EXPR_DISABLED );
    return Boolean.parseBoolean( value );
  }

  private void loadTemplateProcessorGates()
  {
    _processorGates = new TreeSet<>( Comparator.comparing( c -> c.getClass().getTypeName() ) );
    ServiceUtil.loadRegisteredServices( _processorGates, ITemplateProcessorGate.class, getClass().getClassLoader() );
  }

  @Override
  public void started( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.PARSE )
    {
      return;
    }

    // Install the handler so we can filter the 'illegal escape character' errors for \$
    _manDiagnosticHandler = JreUtil.isJava25orLater()
                            ? (StringTemplateDiagnosticHandler)ReflectUtil.method(
                                ReflectUtil.constructor( "manifold.internal.javac.ManDiagnosticHandler_25" ).newInstance(), "make", Context.class )
                                 .invoke( _javacTask.getContext() )
                            : (StringTemplateDiagnosticHandler)ReflectUtil.constructor( "manifold.internal.javac.ManDiagnosticHandler_8", Context.class )
                                .newInstance( _javacTask.getContext() );
  }

  @Override
  public void finished( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.PARSE )
    {
      return;
    }

    try
    {
      // Uninstall the handler after the file parses (we create new handler for each file)
      Log.instance( _javacTask.getContext() ).popDiagnosticHandler( (Log.DiagnosticHandler)_manDiagnosticHandler );
    }
    catch( Throwable ignore ) {}

    for( Tree tree : e.getCompilationUnit().getTypeDecls() )
    {
      if( !(tree instanceof JCTree.JCClassDecl) )
      {
        continue;
      }
      
      JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl)tree;
      if( !isTypeExcluded( classDecl, e.getCompilationUnit() ) )
      {
        classDecl.accept( this );
      }
    }
  }

  @Override
  public void visitClassDef( JCTree.JCClassDecl classDef )
  {
    process( classDef.getModifiers(), () -> super.visitClassDef( classDef ) );
  }

  @Override
  public void visitMethodDef( JCTree.JCMethodDecl methodDecl )
  {
    process( methodDecl.getModifiers(), () -> super.visitMethodDef( methodDecl ) );
  }

  @Override
  public void visitVarDef( JCTree.JCVariableDecl varDecl )
  {
    process( varDecl.getModifiers(), () -> super.visitVarDef( varDecl ) );
  }

  private void process( JCTree.JCModifiers modifiers, Runnable processor )
  {
    Boolean disable = getDisableAnnotationValue( modifiers );
    if( disable != null )
    {
      pushDisabled( disable );
    }

    try
    {
      // processor
      processor.run();
    }
    finally
    {
      if( disable != null )
      {
        popDisabled( disable );
      }
    }
  }

  private boolean isTypeExcluded( JCTree.JCClassDecl classDef, CompilationUnitTree compilationUnit )
  {
    if( _processorGates.isEmpty() )
    {
      return false;
    }

    ExpressionTree pkgName = compilationUnit.getPackageName();
    if( pkgName == null )
    {
      return false;
    }

    String simpleName = classDef.name.toString();
    String fqn = pkgName.toString() + '.' + simpleName;
    return _processorGates.stream().anyMatch( gate -> gate.exclude( fqn ) );
  }

  private Boolean getDisableAnnotationValue( JCTree.JCModifiers modifiers )
  {
    Boolean disable = null;
    for( JCTree.JCAnnotation anno: modifiers.getAnnotations() )
    {
      if( anno.annotationType.toString().contains( DisableStringLiteralTemplates.class.getSimpleName() ) )
      {
        try
        {
          com.sun.tools.javac.util.List<JCTree.JCExpression> args = anno.getArguments();
          if( args.isEmpty() )
          {
            disable = true;
          }
          else
          {
            JCTree.JCExpression argExpr = args.get( 0 );
            Object value;
            if( argExpr instanceof JCTree.JCLiteral &&
                (value = ((JCTree.JCLiteral)argExpr).getValue()) instanceof Boolean )
            {
              disable = (boolean)value;
            }
            else
            {
              IDynamicJdk.instance().logError( Log.instance( _javacTask.getContext() ), argExpr.pos(),
                "proc.messager", "Only boolean literal values 'true' and 'false' allowed here" );
              disable = true;
            }
          }
        }
        catch( Exception e )
        {
          disable = true;
        }
      }
    }
    return disable;
  }

  private boolean isDisabled()
  {
    return _disabled.peek();
  }
  private void pushDisabled( boolean disabled )
  {
    _disabled.push( disabled );
  }
  private void popDisabled( boolean disabled )
  {
    if( disabled != _disabled.pop() )
    {
      throw new IllegalStateException();
    }
  }

  @Override
  public void visitAnnotation( JCTree.JCAnnotation jcAnno )
  {
    // Disable string templates inside annotations. Reasons:
    // 1. spring framework has its own $ processing that interferes
    // 2. annotation processors in IDEs won't work with it
    // 3. there's not enough benefit to justify the cost of fixing the above problems
    // See https://github.com/manifold-systems/manifold/issues/102

    pushDisabled( true );
    try
    {
      super.visitAnnotation( jcAnno );
    }
    finally
    {
      popDisabled( true );
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

    if( isDisabled() )
    {
      return;
    }

    TreeMaker maker = TreeMaker.instance( _javacTask.getContext() );
    String stringValue = (String)value;
    List<JCTree.JCExpression> exprs = parse( stringValue, jcLiteral.getPreferredPosition() );
    JCTree.JCBinary concat = null;
    while( !exprs.isEmpty() )
    {
      if( concat == null )
      {
        concat = maker.Binary( JCTree.Tag.PLUS, exprs.remove( 0 ), exprs.remove( 0 ) );
      }
      else
      {
        concat = maker.Binary( JCTree.Tag.PLUS, concat, exprs.remove( 0 ) );
      }
    }

    result = concat == null ? result : maker.Parens( concat );
  }

  public List<JCTree.JCExpression> parse( String stringValue, int literalOffset )
  {
    List<StringLiteralTemplateParser.Expr> comps = StringLiteralTemplateParser.parse(
      new EscapeMatcher( _manDiagnosticHandler, literalOffset+1 ), _isSimpleExprDisabled.get(), stringValue );
    if( comps.isEmpty() )
    {
      return Collections.emptyList();
    }

    TreeMaker maker = TreeMaker.instance( _javacTask.getContext() );
    Names names = Names.instance( _javacTask.getContext() );

    List<JCTree.JCExpression> exprs = new ArrayList<>();
    StringLiteralTemplateParser.Expr prev = null;
    for( StringLiteralTemplateParser.Expr comp : comps )
    {
      JCTree.JCExpression expr;
      if( comp.isVerbatim() )
      {
        expr = maker.Literal( comp.getExpr() );
      }
      else
      {
        if( prev != null && !prev.isVerbatim() )
        {
          // enforce concatenation
          exprs.add( maker.Literal( "" ) );
        }

        int exprPos = literalOffset + 1 + comp.getOffset();

        if( comp.isIdentifier() )
        {
          JCTree.JCIdent ident = maker.Ident( names.fromString( comp.getExpr() ) );
          ident.pos = exprPos;
          expr = ident;
        }
        else
        {
          DiagnosticCollector<JavaFileObject> errorHandler = new DiagnosticCollector<>();
          expr = _tp.getHost().getJavaParser().parseExpr( comp.getExpr(), errorHandler );
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
      exprs.add( 0, maker.Literal( "" ) );
    }

    return exprs;
  }

  private boolean transferParseErrors( int literalOffset, StringLiteralTemplateParser.Expr comp, JCTree.JCExpression expr, DiagnosticCollector<JavaFileObject> errorHandler )
  {
    if( expr == null || errorHandler.getDiagnostics().stream().anyMatch( e -> e.getKind() == Diagnostic.Kind.ERROR ) )
    {
      for( Diagnostic<? extends JavaFileObject> diag : errorHandler.getDiagnostics() )
      {
        if( diag.getKind() == Diagnostic.Kind.ERROR )
        {
          JCDiagnostic jcDiag = ((ClientCodeWrapper.DiagnosticSourceUnwrapper)diag).d;
          String code = debaseMsgCode( diag );
          IDynamicJdk.instance().logError(
            Log.instance( _javacTask.getContext() ), new JCDiagnostic.SimpleDiagnosticPosition( literalOffset + 1 + comp.getOffset() ), code, jcDiag.getArgs() );
        }
      }
      return true;
    }
    return false;
  }

  private String debaseMsgCode( Diagnostic<? extends JavaFileObject> diag )
  {
    // Log#error() will prepend "compiler.err", so we must remove it to avoid double-basing the message
    String code = diag.getCode();
    if( code != null && code.startsWith( "compiler.err" ) )
    {
      code = code.substring( "compiler.err".length() + 1 );
    }
    return code;
  }

  private void replaceNames( JCTree.JCExpression expr, int offset )
  {
    expr.accept( new NameReplacer( _javacTask, offset ) );
  }

  @Override
  public boolean isSuppressed( JCDiagnostic.DiagnosticPosition pos, String issueKey, Object[] args )
  {
    if( issueKey.contains( "unmatched.processor.options" ) && args != null && args.length == 1 )
    {
      // filter the warning for unmatched processor option
      return args[0].toString().contains( SIMPLE_EXPR_DISABLED );
    }
    return ICompilerComponent.super.isSuppressed( pos, issueKey, args );
  }

  private static class EscapeMatcher implements IntPredicate
  {
    private final StringTemplateDiagnosticHandler _manDiagnosticHandler;
    private final int _offsetOfLiteral;
    private int _escapedCount;

    private EscapeMatcher( StringTemplateDiagnosticHandler manDiagnosticHandler, int offsetOfLiteral )
    {
      _manDiagnosticHandler = manDiagnosticHandler;
      _offsetOfLiteral = offsetOfLiteral;
    }

    @Override
    public boolean test( int index )
    {
      if( _manDiagnosticHandler.isEscapedPos( _offsetOfLiteral + index + (_escapedCount+1) ) )
      {
        _escapedCount++;
        return true;
      }
      return false;
    }
  }
}
