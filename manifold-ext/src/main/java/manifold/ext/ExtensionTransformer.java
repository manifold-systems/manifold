package manifold.ext;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTrees;
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
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import manifold.ext.api.ExtensionMethod;
import manifold.ext.api.Structural;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.TypeProcessor;

/**
 */
public class ExtensionTransformer extends TreeTranslator
{
  private static final String STRUCTURAL_PROXY = "_structuralproxy_";

  private final ExtSourceProducer _sp;
  private final TypeProcessor _tp;
  private final Symbol.ClassSymbol _typeElement;

  ExtensionTransformer( ExtSourceProducer sp, TypeProcessor typeProcessor, Symbol.ClassSymbol typeElement )
  {
    _sp = sp;
    _tp = typeProcessor;
    _typeElement = typeElement;
  }

  /**
   * Erase all structural inteface type literals to Object
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

//  @Override
//  public void visitAssign( JCTree.JCAssign tree )
//  {
//    super.visitAssign( tree );
//    if( isStructuralInterface( tree.type.tsym ) )
//    {
//      Symtab symbols = Symtab.instance( _tp.getContext() );
//
//      tree.rhs.type = symbols.objectType;
//      tree.type = symbols.objectType;
//    }
//    result = tree;
//  }
//
//  @Override
//  public void visitVarDef( JCTree.JCVariableDecl tree )
//  {
//    super.visitVarDef( tree );
//    if( isStructuralInterface( tree.type.tsym ) )
//    {
//      Symtab symbols = Symtab.instance( _tp.getContext() );
//      tree.init.type = symbols.objectType;
//    }
//  }


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
      replaceExtCall( tree, method );
      result = tree;
    }
    else if( isStructuralMethod( tree ) )
    {
      result = replaceStructuralCall( tree );
    }
    else
    {
      result = tree;
    }
  }

//  private JCTree replaceStructuralCall( JCTree.JCMethodInvocation oldCall )
//  {
//    JCExpression methodSelect = oldCall.getMethodSelect();
//    if( methodSelect instanceof JCTree.JCFieldAccess )
//    {
//      Types types = Types.instance( _tp.getContext() );
//      Symtab symbols = Symtab.instance( _tp.getContext() );
//      Names names = Names.instance( _tp.getContext() );
//      JavacElements elementUtils = JavacElements.instance( _tp.getContext() );
//      Symbol.ClassSymbol reflectMethodClassSym = elementUtils.getTypeElement( getClass().getName() );
//      Symbol.MethodSymbol method = lookupMethod( oldCall.pos(), names.fromString( "invoke" ), reflectMethodClassSym.type,
//                                                 List.from( new Type[]{symbols.objectType, symbols.stringType, types.makeArrayType( symbols.stringType ), types.makeArrayType( symbols.objectType )} ) );
//
//      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
//      TreeMaker make = _tp.getTreeMaker();
//      JavacElements javacElems = _tp.getElementUtil();
//      JCExpression thisArg = m.selected;
//
//      ArrayList<JCExpression> newArgs = new ArrayList<>();
//      ArrayList<JCExpression> ifaceArgs = new ArrayList<>();
//      for( JCExpression expr : oldCall.args )
//      {
//        ifaceArgs.add( boxIfPrimitive( expr ) );
//      }
//      ArrayList<JCExpression> paramTypes = new ArrayList<>();
//      for( Type paramType: oldCall.type.getParameterTypes() )
//      {
//        JCTree.JCLiteral literal = make.Literal( paramType.tsym.getQualifiedName().toString() );
//        paramTypes.add( literal );
//      }
//      newArgs.add( thisArg );
//      newArgs.add( make.Literal( ((JCTree.JCFieldAccess)oldCall.meth).getIdentifier().toString() ) );
//      newArgs.add( make.NewArray( null, List.nil(), List.from( paramTypes ) ).setType( types.makeArrayType( symbols.stringType ) ) );
//      newArgs.add( make.NewArray( null, List.nil(), List.from( ifaceArgs ) ).setType( types.makeArrayType( symbols.objectType ) ) );
//
//
//      JCTree.JCMethodInvocation newCall = make.Apply( List.nil(), memberAccess( make, javacElems, ExtensionTransformer.class.getName() + ".invoke" ), List.from( newArgs ) );
//      newCall.type = symbols.objectType;
//      JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)newCall.getMethodSelect();
//      newMethodSelect.sym = method;
//      newMethodSelect.type = method.type;
//      assignTypes( newMethodSelect.selected, reflectMethodClassSym );
//
//      Type returnType = oldCall.getMethodSelect().type.getReturnType();
//      JCTree result = unboxIfNecessary( newCall, returnType );
//      if( result == oldCall )
//      {
//        result = make.TypeCast( returnType, oldCall );
//        result.type = returnType;
//      }
//      return result;
//    }
//    return null;
//  }

  private JCTree replaceStructuralCall( JCTree.JCMethodInvocation theCall )
  {
    JCExpression methodSelect = theCall.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      Symtab symbols = Symtab.instance( _tp.getContext() );
      Names names = Names.instance( _tp.getContext() );
      JavacElements elementUtils = JavacElements.instance( _tp.getContext() );
      Symbol.ClassSymbol reflectMethodClassSym = elementUtils.getTypeElement( getClass().getName() );
      Symbol.MethodSymbol makeInterfaceProxyMethod = lookupMethod( theCall.pos(), names.fromString( "constructProxy" ), reflectMethodClassSym.type,
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

//  private static Map<String, Map<Object, Object>> PROXY_INSTANCE_CACHE = new ConcurrentHashMap<String, Map<Object, Object>>();
//  private static Object findCachedProxy( Object root, String iface ) {
//    Map<Object, Object> proxyInstanceByInstance = PROXY_INSTANCE_CACHE.get( iface );
//    if( proxyInstanceByInstance == null ) {
//      PROXY_INSTANCE_CACHE.put( iface, proxyInstanceByInstance = Collections.synchronizedMap( new WeakHashMap<Object, Object>() ) );
//    }
//    Object proxyInstance = proxyInstanceByInstance.get( root );
//    if( proxyInstance == null ) {
//      proxyInstanceByInstance.put( root, proxyInstance = createNewProxy( root, iface ) );
//    }
//    return proxyInstance;
//  }

  private static Map<Class, Map<Class, Constructor>> PROXY_CACHE = new ConcurrentHashMap<>();

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

//  public static Object invoke( Object receiver, String name, String[] paramTypes, Object[] args )
//  {
//    Class[] types = new Class[paramTypes.length];
//    for( int i = 0; i < types.length; i++ )
//    {
//      try
//      {
//        types[i] = Class.forName( paramTypes[i], false, receiver.getClass().getClassLoader() );
//      }
//      catch( ClassNotFoundException e )
//      {
//        throw new RuntimeException( e );
//      }
//    }
//    Method m = getDeclaredMethod( receiver.getClass(), name, types );
//    if( m == null )
//    {
//      m = getExtensionMethod( receiver.getClass(), name, types );
//    }
//    m.setAccessible( true );
//    try
//    {
//      return m.invoke( receiver, args );
//    }
//    catch( Exception e )
//    {
//      throw new RuntimeException( e );
//    }
//  }
//
//  private static Method getExtensionMethod( Class<?> aClass, String name, Class[] types )
//  {
//    return null;
//  }
//
//  private static Method getDeclaredMethod( Class<?> cls, String name, Class[] paramTypes )
//  {
//    try
//    {
//      for( Method m : cls.getDeclaredMethods() )
//      {
//        if( m.getName().equals( name ) && Arrays.equals( m.getParameters(), paramTypes ) )
//        {
//          return m;
//        }
//      }
//      Class<?> superclass = cls.getSuperclass();
//      if( superclass != null )
//      {
//        Method m = getDeclaredMethod( superclass, name, paramTypes );
//        if( m != null )
//        {
//          return m;
//        }
//      }
//      for( Class iface : cls.getInterfaces() )
//      {
//        Method m = getDeclaredMethod( iface, name, paramTypes );
//        if( m != null )
//        {
//          return m;
//        }
//      }
//      return null;
//    }
//    catch( Exception e )
//    {
//      throw new RuntimeException( e );
//    }
//  }
//
//  @SuppressWarnings("unchecked")
//  private <T extends JCTree> T boxIfPrimitive( T tree )
//  {
//    return tree.type.isPrimitive() ? (T)boxPrimitive( (JCExpression)tree ) : tree;
//  }
//
//  private JCExpression boxPrimitive( JCExpression tree )
//  {
//    Types types = Types.instance( _tp.getContext() );
//    return boxPrimitive( tree, types.boxedClass( tree.type ).type );
//  }
//
//  private JCExpression boxPrimitive( JCExpression tree, Type box )
//  {
//    Names names = Names.instance( _tp.getContext() );
//    Symbol valueOfSym = lookupMethod( tree.pos(),
//                                      names.valueOf,
//                                      box,
//                                      List.<Type>nil().prepend( tree.type ) );
//    TreeMaker make = make_at( tree.pos() );
//    return make.App( make.QualIdent( valueOfSym ), List.of( tree ) );
//  }
//
//  JCExpression unboxIfNecessary( JCExpression tree, Type primitive )
//  {
//    if( !primitive.isPrimitive() )
//    {
//      return tree;
//    }
//
//    Types types = Types.instance( _tp.getContext() );
//    TreeMaker make = _tp.getTreeMaker();
//    Type unboxedType = types.unboxedType( tree.type );
//    if( unboxedType.getTag() == TypeTag.NONE )
//    {
//      unboxedType = primitive;
//      make_at( tree.pos() );
//      tree = make.TypeCast( types.boxedClass( unboxedType ).type, tree );
//    }
//    else
//    {
//      // There must be a conversion from unboxedType to primitive.
//      if( !types.isSubtype( unboxedType, primitive ) )
//      {
//        throw new AssertionError( tree );
//      }
//    }
//    make_at( tree.pos() );
//    Names names = Names.instance( _tp.getContext() );
//    Symbol valueSym = lookupMethod( tree.pos(),
//                                    unboxedType.tsym.name.append( names.Value ), // x.intValue()
//                                    tree.type,
//                                    List.nil() );
//    return make.App( make.Select( tree, valueSym ) );
//  }

  private Symbol.MethodSymbol lookupMethod( JCDiagnostic.DiagnosticPosition pos, Name name, Type qual, List<Type> args )
  {
    Resolve rs = Resolve.instance( _tp.getContext() );
    AttrContext attrContext = new AttrContext();
    Env<AttrContext> env = new AttrContextEnv( pos.getTree(), attrContext );
    JavacTrees trees = JavacTrees.instance( _tp.getContext() );
    TreePath path = trees.getPath( _typeElement );
    env.toplevel = (JCTree.JCCompilationUnit)path.getCompilationUnit();
    return rs.resolveInternalMethod( pos, env, qual, name, args, null );
  }

//  private TreeMaker make_at( JCDiagnostic.DiagnosticPosition pos )
//  {
//    TreeMaker make = _tp.getTreeMaker();
//    return make.at( pos );
//  }
}
