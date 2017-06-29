package manifold.ext;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.AttrContextEnv;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.tools.Diagnostic;
import manifold.ext.api.Extension;
import manifold.ext.api.Structural;
import manifold.ext.api.This;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.TypeProcessor;

/**
 */
public class ExtensionTransformer extends TreeTranslator
{
  private static final String STRUCTURAL_PROXY = "_structuralproxy_";
  private static Map<Class, Map<Class, Constructor>> PROXY_CACHE = new ConcurrentHashMap<>();

  private final ExtSourceProducer _sp;
  private final TypeProcessor _tp;

  ExtensionTransformer( ExtSourceProducer sp, TypeProcessor typeProcessor )
  {
    _sp = sp;
    _tp = typeProcessor;
  }

  /**
   * Erase all structural interface type literals to Object
   */
  @Override
  public void visitIdent( JCTree.JCIdent tree )
  {
    super.visitIdent( tree );
    if( isStructuralInterface( tree.sym ) )
    {
      Symtab symbols = Symtab.instance( _tp.getContext() );
      Symbol.ClassSymbol objectSym = (Symbol.ClassSymbol)symbols.objectType.tsym;
      tree.sym = objectSym;
      tree.type = objectSym.type;
    }
    result = tree;
  }

  /**
   * Erase all structural interface type literals to Object
   */
  @Override
  public void visitSelect( JCTree.JCFieldAccess tree )
  {
    super.visitSelect( tree );
    if( isStructuralInterface( tree.sym ) )
    {
      Symtab symbols = Symtab.instance( _tp.getContext() );
      Symbol.ClassSymbol objectSym = (Symbol.ClassSymbol)symbols.objectType.tsym;
      tree.sym = objectSym;
      tree.type = objectSym.type;
    }
    result = tree;
  }

  @Override
  public void visitTypeCast( JCTypeCast tree )
  {
    super.visitTypeCast( tree );
    if( isStructuralInterface( tree.type.tsym ) )
    {
      Symtab symbols = Symtab.instance( _tp.getContext() );
      Symbol.ClassSymbol objectSym = (Symbol.ClassSymbol)symbols.objectType.tsym;

      //## todo:
      // implement a structural assignability test and
      // conditionally erase the type based on whether or not
      // it is assignable to the structural interface
      tree.type = objectSym.type;
    }
    result = tree;
  }

  /**
   * Replace all extension method call-sites with static calls to extension methods
   */
  @Override
  public void visitApply( JCTree.JCMethodInvocation tree )
  {
    super.visitApply( tree );
    Symbol.MethodSymbol method = findExtMethod( tree );
    if( method != null )
    {
      // The structural interface method is implemented as an extension method,
      // replace with extension method call
      replaceExtCall( tree, method );
      result = tree;
    }
    else if( isStructuralMethod( tree ) )
    {
      // The structural interface method is implemented directly in the type or supertype hierarchy,
      // replace with proxy call
      result = replaceStructuralCall( tree );
    }
    else
    {
      result = tree;
    }
  }

  /**
   * Issue errors/warnings if an extension method violates extension method grammar or conflicts with an existing method
   */
  @Override
  public void visitMethodDef( JCTree.JCMethodDecl tree )
  {
    super.visitMethodDef( tree );
    verifyExtensionMethod( tree );
    result = tree;
  }

  private void verifyExtensionMethod( JCTree.JCMethodDecl tree )
  {
    if( !isFromExtensionClass( tree ) )
    {
      return;
    }

    List<JCTree.JCVariableDecl> parameters = tree.getParameters();
    if( parameters.length() == 0 )
    {
      return;
    }

    String extendedClassName = _tp.getCompilationUnit().getPackageName().toString();
    if( !extendedClassName.startsWith( ExtSourceProducer.EXTENSIONS_PACKAGE + '.' ) )
    {
      return;
    }

    extendedClassName = extendedClassName.substring( ExtSourceProducer.EXTENSIONS_PACKAGE.length() + 1 );

    for( int i = 0; i < parameters.size(); i++ )
    {
      JCTree.JCVariableDecl param = parameters.get( i );
      long methodModifiers = tree.getModifiers().flags;
      if( hasAnnotation( param.getModifiers().getAnnotations(), This.class ) )
      {
        if( i != 0 )
        {
          _tp.report( param, Diagnostic.Kind.ERROR, "@This must target only the first parameter of an extension method" );
        }
        else if( !Modifier.isStatic( (int)methodModifiers ) )
        {
          _tp.report( tree, Diagnostic.Kind.ERROR, "Extension method " + tree.getName() + " must be declared 'static'" );
        }

        if( Modifier.isPrivate( (int)methodModifiers ) )
        {
          _tp.report( tree, Diagnostic.Kind.ERROR, "Extension method " + tree.getName() + " must not be declared 'private'" );
        }

        if( !((Symbol.ClassSymbol)param.type.tsym).className().equals( extendedClassName ) )
        {
          _tp.report( param, Diagnostic.Kind.ERROR, "Expecting type '" + extendedClassName + "' for @This parameter" );
        }
      }
      else if( i == 0 &&
               Modifier.isStatic( (int)methodModifiers ) &&
               !Modifier.isPrivate( (int)methodModifiers ) &&
               param.type.toString().equals( extendedClassName ) )
      {
        _tp.report( param, Diagnostic.Kind.WARNING, "Maybe missing @This to declare an extension method?" );
      }
    }
  }

  private boolean isFromExtensionClass( JCTree.JCMethodDecl tree )
  {
    Tree parent = _tp.getParent( tree );
    if( parent instanceof JCTree.JCClassDecl )
    {
      if( hasAnnotation( ((JCTree.JCClassDecl)parent).getModifiers().getAnnotations(), Extension.class ) )
      {
        return true;
      }
    }
    return false;
  }

  private boolean hasAnnotation( List<JCTree.JCAnnotation> annotations, Class<? extends Annotation> annoClass )
  {
    for( JCTree.JCAnnotation anno: annotations )
    {
      if( anno.getAnnotationType().type.toString().equals( annoClass.getCanonicalName() ) )
      {
        return true;
      }
    }
    return false;
  }

  private JCTree replaceStructuralCall( JCTree.JCMethodInvocation theCall )
  {
    JCExpression methodSelect = theCall.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      Symtab symbols = Symtab.instance( _tp.getContext() );
      Names names = Names.instance( _tp.getContext() );
      JavacElements elementUtils = JavacElements.instance( _tp.getContext() );
      Symbol.ClassSymbol reflectMethodClassSym = elementUtils.getTypeElement( getClass().getName() );
      Symbol.MethodSymbol makeInterfaceProxyMethod = resolveMethod( theCall.pos(), names.fromString( "constructProxy" ), reflectMethodClassSym.type,
                                                                    List.from( new Type[]{symbols.objectType, symbols.classType} ) );

      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      TreeMaker make = _tp.getTreeMaker();
      JavacElements javacElems = _tp.getElementUtil();
      JCExpression thisArg = m.selected;

      ArrayList<JCExpression> newArgs = new ArrayList<>();
      newArgs.add( thisArg );
      JCTree.JCFieldAccess ifaceClassExpr = (JCTree.JCFieldAccess)memberAccess( make, javacElems, thisArg.type.tsym.getQualifiedName().toString() + ".class" );
      ifaceClassExpr.type = symbols.classType;
      ifaceClassExpr.sym = symbols.classType.tsym;
      assignTypes( ifaceClassExpr.selected, thisArg.type.tsym );
      newArgs.add( ifaceClassExpr );

      JCTree.JCMethodInvocation makeProxyCall = make.Apply( List.nil(), memberAccess( make, javacElems, ExtensionTransformer.class.getName() + ".constructProxy" ), List.from( newArgs ) );
      makeProxyCall.type = thisArg.type;
      JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)makeProxyCall.getMethodSelect();
      newMethodSelect.sym = makeInterfaceProxyMethod;
      newMethodSelect.type = makeInterfaceProxyMethod.type;
      assignTypes( newMethodSelect.selected, reflectMethodClassSym );

      JCTypeCast cast = make.TypeCast( thisArg.type, makeProxyCall );
      cast.type = thisArg.type;

      ((JCTree.JCFieldAccess)theCall.meth).selected = cast;
      return theCall;
    }
    return null;
  }

  private void replaceExtCall( JCTree.JCMethodInvocation tree, Symbol.MethodSymbol method )
  {
    JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      TreeMaker make = _tp.getTreeMaker();
      JavacElements javacElems = _tp.getElementUtil();
      JCExpression thisArg = m.selected;
      String extensionFqn = method.getEnclosingElement().asType().tsym.toString();
      m.selected = memberAccess( make, javacElems, extensionFqn );
      JavacTaskImpl javacTask = ClassSymbols.instance( _sp.getTypeLoader().getModule() ).getJavacTask();
      Symbol.ClassSymbol extensionClassSym = ClassSymbols.instance( _sp.getTypeLoader().getModule() ).getClassSymbol( javacTask, extensionFqn ).getFirst();
      assignTypes( m.selected, extensionClassSym );
      m.sym = method;
      m.type = method.type;

      ArrayList<JCExpression> newArgs = new ArrayList<>( tree.args );
      newArgs.add( 0, thisArg );

      tree.args = List.from( newArgs );
    }
  }

  private void assignTypes( JCExpression m, Symbol symbol )
  {
    if( m instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess)m;
      fieldAccess.sym = symbol;
      fieldAccess.type = symbol.type;
      assignTypes( fieldAccess.selected, symbol.owner );
    }
    else if( m instanceof JCTree.JCIdent )
    {
      JCTree.JCIdent fieldAccess = (JCTree.JCIdent)m;
      fieldAccess.sym = symbol;
      fieldAccess.type = symbol.type;
    }
  }

  private Symbol.MethodSymbol findExtMethod( JCTree.JCMethodInvocation tree )
  {
    JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof MemberSelectTree )
    {
      JCTree.JCFieldAccess meth = (JCTree.JCFieldAccess)tree.meth;
      if( !meth.sym.hasAnnotations() )
      {
        return null;
      }
      for( Attribute.Compound annotation : meth.sym.getAnnotationMirrors() )
      {
        if( annotation.type.toString().equals( ExtensionMethod.class.getName() ) )
        {
          String extensionClass = (String)annotation.values.get( 0 ).snd.getValue();
          JavacTaskImpl javacTask = (JavacTaskImpl)_tp.getJavacTask(); //JavacHook.instance() != null ? (JavacTaskImpl)JavacHook.instance().getJavacTask() : ClassSymbols.instance( _sp.getTypeLoader().getModule() ).getJavacTask();
          Symbol.ClassSymbol extClassSym = ClassSymbols.instance( _sp.getTypeLoader().getModule() ).getClassSymbol( javacTask, extensionClass ).getFirst();
          Types types = Types.instance( javacTask.getContext() );
          outer:
          for( Symbol elem : extClassSym.members().getElements() )
          {
            if( elem instanceof Symbol.MethodSymbol && elem.flatName().toString().equals( meth.sym.name.toString() ) )
            {
              Symbol.MethodSymbol extMethodSym = (Symbol.MethodSymbol)elem;
              List<Symbol.VarSymbol> extParams = extMethodSym.getParameters();
              List<Symbol.VarSymbol> calledParams = ((Symbol.MethodSymbol)meth.sym).getParameters();
              if( extParams.size() - 1 != calledParams.size() )
              {
                continue;
              }
              for( int i = 1; i < extParams.size(); i++ )
              {
                Symbol.VarSymbol extParam = extParams.get( i );
                Symbol.VarSymbol calledParam = calledParams.get( i - 1 );
                if( !types.isSameType( types.erasure( extParam.type ), types.erasure( calledParam.type ) ) )
                {
                  continue outer;
                }
              }
              return extMethodSym;
            }
          }
        }
      }
    }
    return null;
  }

  private boolean isStructuralMethod( JCTree.JCMethodInvocation tree )
  {
    JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      JCExpression thisArg = m.selected;
      if( isStructuralInterface( thisArg.type.tsym ) )
      {
        return true;
      }
    }
    return false;
  }

  private JCExpression memberAccess( TreeMaker make, JavacElements javacElems, String path )
  {
    return memberAccess( make, javacElems, path.split( "\\." ) );
  }

  private JCExpression memberAccess( TreeMaker make, JavacElements node, String... components )
  {
    JCExpression expr = make.Ident( node.getName( components[0] ) );
    for( int i = 1; i < components.length; i++ )
    {
      expr = make.Select( expr, node.getName( components[i] ) );
    }
    return expr;
  }

  private boolean isStructuralInterface( Symbol sym )
  {
    if( !sym.isInterface() || !sym.hasAnnotations() )
    {
      return false;
    }
    for( Attribute.Compound annotation : sym.getAnnotationMirrors() )
    {
      if( annotation.type.toString().equals( Structural.class.getName() ) )
      {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("UnusedDeclaration")
  public static Object constructProxy( Object root, Class iface )
  {
    // return findCachedProxy( root, iface ); // this is only beneficial when structural invocation happens in a loop, otherwise too costly
    return createNewProxy( root, iface );
  }

  private static Object createNewProxy( Object root, Class<?> iface )
  {
    if( iface.isAssignableFrom( root.getClass() ) )
    {
      return root;
    }

    Map<Class, Constructor> proxyByClass = PROXY_CACHE.get( iface );
    if( proxyByClass == null )
    {
      PROXY_CACHE.put( iface, proxyByClass = new ConcurrentHashMap<>() );
    }
    Class rootClass = root.getClass();
    Constructor proxyClassCtor = proxyByClass.get( rootClass );
    if( proxyClassCtor == null )
    {
      Class proxyClass = createProxy( iface, rootClass );
      proxyByClass.put( rootClass, proxyClassCtor = proxyClass.getConstructors()[0] );
    }
    try
    {
      return proxyClassCtor.newInstance( root );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  private static Class createProxy( Class iface, Class rootClass )
  {
    String relativeProxyName = rootClass.getSimpleName() + STRUCTURAL_PROXY + iface.getCanonicalName().replace( '.', '_' );
    return StructuralTypeProxyGenerator.makeProxy( iface, rootClass, relativeProxyName );
  }

  private Symbol.MethodSymbol resolveMethod( JCDiagnostic.DiagnosticPosition pos, Name name, Type qual, List<Type> args )
  {
    Resolve rs = Resolve.instance( _tp.getContext() );
    AttrContext attrContext = new AttrContext();
    Env<AttrContext> env = new AttrContextEnv( pos.getTree(), attrContext );
    env.toplevel = _tp.getCompilationUnit();
    return rs.resolveInternalMethod( pos, env, qual, name, args, null );
  }
}
