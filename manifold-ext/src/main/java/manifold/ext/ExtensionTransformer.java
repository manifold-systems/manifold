package manifold.ext;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
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
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.lang.model.type.NoType;
import javax.tools.Diagnostic;
import manifold.ExtIssueMsg;
import manifold.ext.api.Extension;
import manifold.ext.api.ICallHandler;
import manifold.ext.api.This;
import manifold.internal.host.ManifoldHost;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.JavaParser;
import manifold.internal.javac.TypeProcessor;
import manifold.util.Pair;
import manifold.util.concurrent.ConcurrentHashSet;
import manifold.util.concurrent.ConcurrentWeakHashMap;

/**
 */
public class ExtensionTransformer extends TreeTranslator
{
  private static final String STRUCTURAL_PROXY = "_structuralproxy_";
  private static Map<Class, Map<Class, Constructor>> PROXY_CACHE = new ConcurrentHashMap<>();
  private static final Map<Object, Set<Class>> ID_MAP = new ConcurrentWeakHashMap<>();

  private final ExtensionManifold _sp;
  private final TypeProcessor _tp;
  private boolean _bridgeMethod;

  ExtensionTransformer( ExtensionManifold sp, TypeProcessor typeProcessor )
  {
    _sp = sp;
    _tp = typeProcessor;
  }

  public TypeProcessor getTypeProcessor()
  {
    return _tp;
  }

  /**
   * Erase all structural interface type literals to Object
   */
  @Override
  public void visitIdent( JCTree.JCIdent tree )
  {
    super.visitIdent( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( tree.sym != null && TypeUtil.isStructuralInterface( _tp, tree.sym ) && !isReceiver( tree ) )
    {
      Symbol.ClassSymbol objectSym = getObjectClass();
      Tree parent = _tp.getParent( tree );
      if( parent instanceof JCTree.JCVariableDecl )
      {
        ((JCTree.JCVariableDecl)parent).type = objectSym.type;
      }
      else if( parent instanceof JCTree.JCWildcard )
      {
        JCTree.JCWildcard wildcard = (JCTree.JCWildcard)parent;
        wildcard.type = new Type.WildcardType( objectSym.type, wildcard.kind.kind, wildcard.type.tsym );
      }
      tree = _tp.getTreeMaker().Ident( objectSym );
      tree.type = objectSym.type;
    }
    result = tree;
  }

  @Override
  public void visitLambda( JCTree.JCLambda tree )
  {
    super.visitLambda( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    tree.type = eraseStructureType( tree.type );
    ArrayList<Type> types = new ArrayList<>();
    for( Type target : tree.targets )
    {
      types.add( eraseStructureType( target ) );
    }
    tree.targets = List.from( types );
  }

  /**
   * Erase all structural interface type literals to Object
   */
  @Override
  public void visitSelect( JCTree.JCFieldAccess tree )
  {
    super.visitSelect( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( TypeUtil.isStructuralInterface( _tp, tree.sym ) && !isReceiver( tree ) )
    {
      Symbol.ClassSymbol objectSym = getObjectClass();
      JCTree.JCIdent objIdent = _tp.getTreeMaker().Ident( objectSym );
      objIdent.type = objectSym.type;
      Tree parent = _tp.getParent( tree );
      if( parent instanceof JCTree.JCVariableDecl )
      {
        ((JCTree.JCVariableDecl)parent).type = objectSym.type;
        ((JCTree.JCVariableDecl)parent).sym.type = objectSym.type;
        ((JCTree.JCVariableDecl)parent).vartype = objIdent;
      }
      else if( parent instanceof JCTree.JCWildcard )
      {
        JCTree.JCWildcard wildcard = (JCTree.JCWildcard)parent;
        wildcard.type = new Type.WildcardType( objectSym.type, wildcard.kind.kind, wildcard.type.tsym );
      }
      result = objIdent;
    }
    else
    {
      result = tree;
    }
  }

  @Override
  public void visitTypeCast( JCTypeCast tree )
  {
    super.visitTypeCast( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( TypeUtil.isStructuralInterface( _tp, tree.type.tsym ) )
    {
      tree.expr = replaceCastExpression( tree.getExpression(), tree.type );
      tree.type = getObjectClass().type;
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

    eraseGenericStructuralVarargs( tree );

    if( _tp.isGenerate() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( method != null )
    {
      // Replace with extension method call
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

  private boolean shouldProcessForGeneration()
  {
    return _bridgeMethod;
  }

  private boolean isBridgeMethod( JCTree.JCMethodDecl tree )
  {
    long modifiers = tree.getModifiers().flags;
    return (Flags.BRIDGE & modifiers) != 0;
  }

  private Type eraseStructureType( Type type )
  {
    return new StructuralTypeEraser( this ).visit( type );
  }

  private boolean isReceiver( JCTree tree )
  {
    Tree parent = _tp.getParent( tree );
    if( parent instanceof JCTree.JCFieldAccess )
    {
      return ((JCTree.JCFieldAccess)parent).getExpression() == tree;
    }
    return false;
  }

  Symbol.ClassSymbol getObjectClass()
  {
    Symtab symbols = Symtab.instance( _tp.getContext() );
    return (Symbol.ClassSymbol)symbols.objectType.tsym;
  }

  private void eraseGenericStructuralVarargs( JCTree.JCMethodInvocation tree )
  {
    if( tree.varargsElement instanceof Type.ClassType && TypeUtil.isStructuralInterface( _tp, tree.varargsElement.tsym ) )
    {
      tree.varargsElement = _tp.getSymtab().objectType;
    }
  }

  /**
   * Issue errors/warnings if an extension method violates extension method grammar or conflicts with an existing method
   */
  @Override
  public void visitMethodDef( JCTree.JCMethodDecl tree )
  {
    if( isBridgeMethod( tree ) )
    {
      // we process bridge methods during Generation, since they don't exist prior to Generation
      _bridgeMethod = true;
    }
    try
    {
      super.visitMethodDef( tree );
    }
    finally
    {
      _bridgeMethod = false;
    }

    if( _tp.isGenerate() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( tree.sym.owner.isAnonymous() )
    {
      // Keep track of anonymous classes so we can process any bridge methods added to them
      JCTree.JCClassDecl anonymousClassDef = (JCTree.JCClassDecl)_tp.getTreeUtil().getTree( tree.sym.owner );
      _tp.preserveInnerClassForGenerationPhase( anonymousClassDef );
    }

    verifyExtensionMethod( tree );
    result = tree;
  }

  private void verifyExtensionMethod( JCTree.JCMethodDecl tree )
  {
    if( !isFromExtensionClass( tree ) )
    {
      return;
    }

    String extendedClassName = _tp.getCompilationUnit().getPackageName().toString();
    if( !extendedClassName.startsWith( ExtensionManifold.EXTENSIONS_PACKAGE + '.' ) )
    {
      return;
    }

    extendedClassName = extendedClassName.substring( ExtensionManifold.EXTENSIONS_PACKAGE.length() + 1 );

    boolean thisAnnoFound = false;
    List<JCTree.JCVariableDecl> parameters = tree.getParameters();
    for( int i = 0; i < parameters.size(); i++ )
    {
      JCTree.JCVariableDecl param = parameters.get( i );
      long methodModifiers = tree.getModifiers().flags;
      if( hasAnnotation( param.getModifiers().getAnnotations(), This.class ) )
      {
        thisAnnoFound = true;

        if( i != 0 )
        {
          _tp.report( param, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_THIS_FIRST.get() );
        }

        if( !(param.type.tsym instanceof Symbol.ClassSymbol) || !((Symbol.ClassSymbol)param.type.tsym).className().equals( extendedClassName ) )
        {
          Symbol.ClassSymbol extendClassSym = JavacElements.instance( _tp.getContext() ).getTypeElement( extendedClassName );
          if( extendClassSym != null && !TypeUtil.isStructuralInterface( _tp, extendClassSym ) ) // an extended class could be made a structural interface which results in Object as @This param, ignore this
          {
            _tp.report( param, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_EXPECTING_TYPE_FOR_THIS.get( extendedClassName ) );
          }
        }
      }
      else if( i == 0 &&
               Modifier.isStatic( (int)methodModifiers ) &&
               Modifier.isPublic( (int)methodModifiers ) &&
               param.type.toString().equals( extendedClassName ) )
      {
        _tp.report( param, Diagnostic.Kind.WARNING, ExtIssueMsg.MSG_MAYBE_MISSING_THIS.get() );
      }
    }

    if( thisAnnoFound || hasAnnotation( tree.getModifiers().getAnnotations(), Extension.class ) )
    {
      long methodModifiers = tree.getModifiers().flags;
      if( !Modifier.isStatic( (int)methodModifiers ) )
      {
        _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_MUST_BE_STATIC.get( tree.getName() ) );
      }

      if( Modifier.isPrivate( (int)methodModifiers ) )
      {
        _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_MUST_NOT_BE_PRIVATE.get( tree.getName() ) );
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
    for( JCTree.JCAnnotation anno : annotations )
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
      Symtab symbols = _tp.getSymtab();
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
      makeProxyCall.setPos( theCall.pos );
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

  private JCExpression replaceCastExpression( JCExpression expression, Type type )
  {
    TreeMaker make = _tp.getTreeMaker();
    Symtab symbols = _tp.getSymtab();
    Names names = Names.instance( _tp.getContext() );
    JavacElements elementUtils = JavacElements.instance( _tp.getContext() );
    Symbol.ClassSymbol reflectMethodClassSym = elementUtils.getTypeElement( getClass().getName() );

    Symbol.MethodSymbol makeInterfaceProxyMethod = resolveMethod( expression.pos(), names.fromString( "assignStructuralIdentity" ), reflectMethodClassSym.type,
                                                                  List.from( new Type[]{symbols.objectType, symbols.classType} ) );

    JavacElements javacElems = _tp.getElementUtil();
    ArrayList<JCExpression> newArgs = new ArrayList<>();
    newArgs.add( expression );
    JCTree.JCFieldAccess ifaceClassExpr = (JCTree.JCFieldAccess)memberAccess( make, javacElems, type.tsym.getQualifiedName().toString() + ".class" );
    ifaceClassExpr.type = symbols.classType;
    ifaceClassExpr.sym = symbols.classType.tsym;
    assignTypes( ifaceClassExpr.selected, type.tsym );
    newArgs.add( ifaceClassExpr );

    JCTree.JCMethodInvocation makeProxyCall = make.Apply( List.nil(), memberAccess( make, javacElems, ExtensionTransformer.class.getName() + ".assignStructuralIdentity" ), List.from( newArgs ) );
    makeProxyCall.type = symbols.objectType;
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)makeProxyCall.getMethodSelect();
    newMethodSelect.sym = makeInterfaceProxyMethod;
    newMethodSelect.type = makeInterfaceProxyMethod.type;
    assignTypes( newMethodSelect.selected, reflectMethodClassSym );

    JCTypeCast castCall = make.TypeCast( symbols.objectType, makeProxyCall );
    castCall.type = symbols.objectType;

    return castCall;

  }

  private void replaceExtCall( JCTree.JCMethodInvocation tree, Symbol.MethodSymbol method )
  {
    JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      boolean isStatic = m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
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

      if( !isStatic )
      {
        ArrayList<JCExpression> newArgs = new ArrayList<>( tree.args );
        newArgs.add( 0, thisArg );
        tree.args = List.from( newArgs );
      }
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
          boolean isStatic = (boolean)annotation.values.get( 1 ).snd.getValue();
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
              int thisOffset = isStatic ? 0 : 1;
              if( extParams.size() - thisOffset != calledParams.size() )
              {
                continue;
              }
              for( int i = thisOffset; i < extParams.size(); i++ )
              {
                Symbol.VarSymbol extParam = extParams.get( i );
                Symbol.VarSymbol calledParam = calledParams.get( i - thisOffset );
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
      if( !m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC ) )
      {
        JCExpression thisArg = m.selected;
        if( TypeUtil.isStructuralInterface( _tp, thisArg.type.tsym ) )
        {
          return true;
        }
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

  @SuppressWarnings("UnusedDeclaration")
  public static Object constructProxy( Object root, Class iface )
  {
    // return findCachedProxy( root, iface ); // this is only beneficial when structural invocation happens in a loop, otherwise too costly
    return createNewProxy( root, iface );
  }

  @SuppressWarnings("UnusedDeclaration")
  public static Object assignStructuralIdentity( Object obj, Class iface )
  {
    //## note: we'd like to avoid the operation if the obj not a ICallHandler,
    // but that is an expensive structural check, more expensive than this call...
    //  if( obj is a ICallHandler )
    //  {
    Set<Class> ifaces = ID_MAP.computeIfAbsent( obj, k -> new ConcurrentHashSet<>() );
    ifaces.add( iface );
   //   }
    return obj;
  }

  private static Object createNewProxy( Object root, Class<?> iface )
  {
    Class rootClass = root.getClass();
    if( iface.isAssignableFrom( rootClass ) )
    {
      return root;
    }

    Map<Class, Constructor> proxyByClass = PROXY_CACHE.get( iface );
    if( proxyByClass == null )
    {
      PROXY_CACHE.put( iface, proxyByClass = new ConcurrentHashMap<>() );
    }
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
    String relativeProxyName = rootClass.getCanonicalName().replace( '.', '_' ) + STRUCTURAL_PROXY + iface.getCanonicalName().replace( '.', '_' );
    if( hasCallHandlerMethod( rootClass ) )
    {
      return DynamicTypeProxyGenerator.makeProxy( iface, rootClass, relativeProxyName );
    }
    return StructuralTypeProxyGenerator.makeProxy( iface, rootClass, relativeProxyName );
  }

  private static boolean hasCallHandlerMethod( Class rootClass )
  {
    String fqn = rootClass.getCanonicalName();
    JavacTaskImpl javacTask = JavaParser.instance().getJavacTask();
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> classSymbol = ClassSymbols.instance( ManifoldHost.getGlobalModule() ).getClassSymbol( javacTask, fqn );
    Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> callHandlerSymbol = ClassSymbols.instance( ManifoldHost.getGlobalModule() ).getClassSymbol( javacTask, ICallHandler.class.getCanonicalName() );
    if( Types.instance( javacTask.getContext() ).isAssignable( callHandlerSymbol.getFirst().asType(), classSymbol.getFirst().asType() ) )
    {
      // Nominally implements ICallHandler
      return true;
    }

    return hasCallMethod( javacTask, classSymbol.getFirst() );
  }

  private static boolean hasCallMethod( JavacTaskImpl javacTask, Symbol.ClassSymbol classSymbol )
  {
    Name call = Names.instance( javacTask.getContext() ).fromString( "call" );
    Iterable<Symbol> elems = classSymbol.members().getElementsByName( call );
    for( Symbol s : elems )
    {
      if( s instanceof Symbol.MethodSymbol )
      {
        List<Symbol.VarSymbol> parameters = ((Symbol.MethodSymbol)s).getParameters();
        if( parameters.size() != 5 )
        {
          return false;
        }

        Symtab symbols = Symtab.instance( javacTask.getContext() );
        Types types = Types.instance( javacTask.getContext() );
        return types.erasure( parameters.get( 0 ).asType() ).equals( types.erasure( symbols.classType ) ) &&
               parameters.get( 1 ).asType().equals( symbols.stringType ) &&
               types.erasure( parameters.get( 2 ).asType() ).equals( types.erasure( symbols.classType ) ) &&
               parameters.get( 3 ).asType() instanceof Type.ArrayType && types.erasure( ((Type.ArrayType)parameters.get( 3 ).asType()).getComponentType() ).equals( types.erasure( symbols.classType ) ) &&
               parameters.get( 4 ).asType() instanceof Type.ArrayType && ((Type.ArrayType)parameters.get( 4 ).asType()).getComponentType().equals( symbols.objectType );
      }
    }
    Type superclass = classSymbol.getSuperclass();
    if( !(superclass instanceof NoType) )
    {
      if( hasCallMethod( javacTask, (Symbol.ClassSymbol)superclass.tsym ) )
      {
        return true;
      }
    }
    for( Type iface : classSymbol.getInterfaces() )
    {
      if( hasCallMethod( javacTask, (Symbol.ClassSymbol)iface.tsym ) )
      {
        return true;
      }
    }
    return false;
  }

  private Symbol.MethodSymbol resolveMethod( JCDiagnostic.DiagnosticPosition pos, Name name, Type qual, List<Type> args )
  {
    return resolveMethod( pos, _tp.getContext(), _tp.getCompilationUnit(), name, qual, args );
  }

  private static Symbol.MethodSymbol resolveMethod( JCDiagnostic.DiagnosticPosition pos, Context ctx, JCTree.JCCompilationUnit compUnit, Name name, Type qual, List<Type> args )
  {
    Resolve rs = Resolve.instance( ctx );
    AttrContext attrContext = new AttrContext();
    Env<AttrContext> env = new AttrContextEnv( pos.getTree(), attrContext );
    env.toplevel = compUnit;
    return rs.resolveInternalMethod( pos, env, qual, name, args, null );
  }

  /**
   * Facilitates ICallHandler where the receiver of the method call structurally implements a method,
   * but the association of the structural interface with the receiver is lost.  For example:
   * <pre>
   *   Person person = Person.create(); // Person is a JsonTypeManifold interface; the runtime type of person here is really just a Map (or Binding)
   *   IMyStructureThing thing = (IMyStructureThing)person; // Extension method[s] satisfying IMyStructureThing on Person make this work e.g., via MyPerosnExt extension methods class
   *   thing.foo(); // foo() is an extension method on Person e.g., defined in MyPersonExt, however the runtime type of thing is just a Map (or Binding) thus the Person type identity is lost
   * </pre>
   */
  //## todo: this is inefficient, we should consider caching the methods by signature along with the interfaces
  public static Object invokeUnhandled( Object thiz, Class proxiedIface, String name, Class returnType, Class[] paramTypes, Object[] args )
  {
    Set<Class> ifaces = ID_MAP.get( thiz );
    if( ifaces != null )
    {
      for( Class iface : ifaces )
      {
        if( iface == proxiedIface )
        {
          continue;
        }

        Method m = findMethod( iface, name, paramTypes );
        if( m != null )
        {
          try
          {
            Object result = m.invoke( constructProxy( thiz, iface ), args );
            //## todo: maybe coerce result if return types are not directly assignable?  e.g., Integer vs. Double
            return result;
          }
          catch( Exception e )
          {
            throw new RuntimeException( e );
          }
        }
      }
    }
    return ICallHandler.UNHANDLED;
  }

  private static Method findMethod( Class<?> iface, String name, Class[] paramTypes )
  {
    try
    {
      Method m = iface.getDeclaredMethod( name, paramTypes );
      if( m == null )
      {
        for( Class superIface : iface.getInterfaces() )
        {
          m = findMethod( superIface, name, paramTypes );
          if( m != null )
          {
            break;
          }
        }
      }
      if( m != null )
      {
        return m;
      }
    }
    catch( Exception e )
    {
      return null;
    }
    return null;
  }
}
