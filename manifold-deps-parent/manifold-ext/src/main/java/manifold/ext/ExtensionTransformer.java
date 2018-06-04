package manifold.ext;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.BasicJavacTask;
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
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import manifold.ExtIssueMsg;
import manifold.api.fs.IFile;
import manifold.api.type.ITypeManifold;
import manifold.api.type.IncrementalCompile;
import manifold.api.type.Precompile;
import manifold.ext.api.Extension;
import manifold.ext.api.Structural;
import manifold.ext.api.This;
import manifold.internal.host.ManifoldHost;
import manifold.internal.javac.ClassSymbols;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavacPlugin;
import manifold.internal.javac.TypeProcessor;
import manifold.util.ReflectUtil;

/**
 */
public class ExtensionTransformer extends TreeTranslator
{
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
      JCTree.JCIdent objIdent = _tp.getTreeMaker().Ident( objectSym );
      if( parent instanceof JCTree.JCVariableDecl )
      {
        ((JCTree.JCVariableDecl)parent).type = objectSym.type;

        long parameterModifier = 8589934592L; // Flag.Flag.PARAMETER.value
        if( (((JCTree.JCVariableDecl)parent).mods.flags & parameterModifier) != 0 )
        {
          objIdent.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).sym.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).vartype = objIdent;
        }
      }
      else if( parent instanceof JCTree.JCWildcard )
      {
        JCTree.JCWildcard wildcard = (JCTree.JCWildcard)parent;
        wildcard.type = new Type.WildcardType( objectSym.type, wildcard.kind.kind, wildcard.type.tsym );
      }
      tree = objIdent;
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
      Tree parent = _tp.getParent( tree );
      if( parent instanceof JCTree.JCVariableDecl )
      {
        ((JCTree.JCVariableDecl)parent).type = objectSym.type;
        long parameterModifier = 8589934592L; // Flag.Flag.PARAMETER.value
        if( (((JCTree.JCVariableDecl)parent).mods.flags & parameterModifier) != 0 )
        {
          objIdent.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).sym.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).vartype = objIdent;
        }
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
      eraseCompilerGeneratedCast( tree );

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

  private void eraseCompilerGeneratedCast( JCTypeCast tree )
  {
    // the javac compiler generates casts e.g., for a generic call such as List#get()

    if( TypeUtil.isStructuralInterface( _tp, tree.type.tsym ) && !isConstructProxyCall( tree.getExpression() ) )
    {
      tree.type = getObjectClass().type;
      TreeMaker make = _tp.getTreeMaker();
      tree.clazz = make.Type( getObjectClass().type );
    }
  }

  private boolean isConstructProxyCall( JCExpression expression )
  {
    if( expression instanceof JCTree.JCMethodInvocation )
    {
      // don't erase cast if we generated it here e.g.., for structural call cast on constructProxy

      JCExpression meth = ((JCTree.JCMethodInvocation)expression).meth;
      return meth instanceof JCTree.JCFieldAccess && ((JCTree.JCFieldAccess)meth).getIdentifier().toString().equals( "constructProxy" );
    }
    return expression instanceof JCTypeCast && isConstructProxyCall( ((JCTypeCast)expression).getExpression() );
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

  @Override
  public void visitClassDef( JCTree.JCClassDecl tree )
  {
    super.visitClassDef( tree );

    verifyExtensionInterfaces( tree );

    checkExtensionClassError( tree );

    precompileClasses( tree );

    incrementalCompileClasses( tree );
  }

  private void precompileClasses( JCTree.JCClassDecl tree )
  {
    Map<String, Set<String>> typeNames = new HashMap<>();
    for( JCTree.JCAnnotation anno : tree.getModifiers().getAnnotations() )
    {
      if( anno.getAnnotationType().type.toString().equals( Precompile.class.getCanonicalName() ) )
      {
        getTypesToCompile( anno, typeNames );
      }
    }

    precompile( typeNames );
  }

  private void getTypesToCompile( JCTree.JCAnnotation precompileAnno, Map<String, Set<String>> typeNames )
  {
    Attribute.Compound attribute = precompileAnno.attribute;
    if( attribute == null )
    {
      return;
    }

    String typeManifoldClassName = null;
    String regex = ".*";
    for( com.sun.tools.javac.util.Pair<Symbol.MethodSymbol, Attribute> pair: attribute.values )
    {
      Name argName = pair.fst.getSimpleName();
      if( argName.toString().equals( "typeManifold" ) )
      {
        typeManifoldClassName = pair.snd.getValue().toString();
      }
      else if( argName.toString().equals( "typeNames" ) )
      {
        regex = pair.snd.getValue().toString();
      }
    }

    Set<String> regexes = typeNames.computeIfAbsent( typeManifoldClassName, tm -> new HashSet<>() );
    regexes.add( regex );
  }

  private void precompile( Map<String, Set<String>> typeNames )
  {
    for( ITypeManifold tm: ManifoldHost.instance().getCurrentModule().getTypeManifolds() )
    {
      for( Map.Entry<String, Set<String>> entry: typeNames.entrySet() )
      {
        String typeManifoldClassName = entry.getKey();
        if( tm.getClass().getName().equals( typeManifoldClassName ) )
        {
          Collection<String> namesToPrecompile = computeNamesToPrecompile( tm.getAllTypeNames(), entry.getValue() );
          for( String fqn : namesToPrecompile )
          {
            JavacElements elementUtils = JavacElements.instance( _tp.getContext() );
            // This call surfaces the type in the compiler.  If compiling in "static" mode, this means
            // the type will be compiled to disk.
            elementUtils.getTypeElement( fqn );
          }
        }
      }
    }
  }

  private Collection<String> computeNamesToPrecompile( Collection<String> allTypeNames, Set<String> regexes )
  {
    Set<String> matchingTypes = new HashSet<>();
    for( String fqn: allTypeNames )
    {
      if( regexes.stream().anyMatch( fqn::matches ) )
      {
        matchingTypes.add( fqn );
      }
    }
    return matchingTypes;
  }

  private void incrementalCompileClasses( JCTree.JCClassDecl tree )
  {
    Set<Object> drivers = new HashSet<>();
    for( JCTree.JCAnnotation anno : tree.getModifiers().getAnnotations() )
    {
      if( anno.getAnnotationType().type.toString().equals( IncrementalCompile.class.getCanonicalName() ) )
      {
        getIncrementalCompileDrivers( anno, drivers );
      }
    }

    incrementalCompile( drivers );
  }

  private void getIncrementalCompileDrivers( JCTree.JCAnnotation anno, Set<Object> drivers )
  {
    Attribute.Compound attribute = anno.attribute;
    if( attribute == null )
    {
      return;
    }

    for( com.sun.tools.javac.util.Pair<Symbol.MethodSymbol, Attribute> pair: attribute.values )
    {
      Name argName = pair.fst.getSimpleName();
      if( argName.toString().equals( "driverClass" ) )
      {
        String fqnDriver = (String)pair.snd.getValue();
        try
        {
          //noinspection unchecked
          Class<?> cls;
          try
          {
            cls = Class.forName( fqnDriver );
          }
          catch( Exception e )
          {
            cls = Class.forName( fqnDriver, false, Thread.currentThread().getContextClassLoader() );
          }
          Object driver = cls.getConstructor().newInstance();
          drivers.add( driver );
        }
        catch( Exception e )
        {
          _tp.report( anno, Diagnostic.Kind.ERROR, e.getClass().getTypeName() + " : " + e.getMessage() );
        }
      }
    }
  }

  private void incrementalCompile( Set<Object> drivers )
  {
    JavacElements elementUtils = JavacElements.instance( _tp.getContext() );
    for( Object driver: drivers )
    {
      //noinspection unchecked
      Set<IFile> files = ((Collection<File>)ReflectUtil.method( driver, "getResourceFiles" ).invoke() ).stream().map( (File f) -> ManifoldHost.getFileSystem().getIFile( f ) )
        .collect( Collectors.toSet() );
      for( ITypeManifold tm : ManifoldHost.instance().getCurrentModule().getTypeManifolds() )
      {
        for( IFile file: files )
        {
          Set<String> types = Arrays.stream( tm.getTypesForFile( file ) ).collect( Collectors.toSet() );
          if( types.size() > 0 )
          {
            ReflectUtil.method( driver, "mapTypesToFile", Set.class, File.class ).invoke( types, file.toJavaFile() );
            for( String fqn : types )
            {
              // This call surfaces the type in the compiler.  If compiling in "static" mode, this means
              // the type will be compiled to disk.
              elementUtils.getTypeElement( fqn );
            }
          }
        }
      }
    }
  }

  private void verifyExtensionInterfaces( JCTree.JCClassDecl tree )
  {
    if( !hasAnnotation( tree.getModifiers().getAnnotations(), Extension.class ) )
    {
      return;
    }

    outer:
    for( JCExpression iface: tree.getImplementsClause() )
    {
      final Symbol.TypeSymbol ifaceSym = iface.type.tsym;
      if( ifaceSym == _tp.getSymtab().objectType.tsym )
      {
        continue;
      }

      for( Attribute.Compound anno: ifaceSym.getAnnotationMirrors() )
      {
        if( anno.type.toString().equals( Structural.class.getName() ) )
        {
          continue outer;
        }
      }
      // extension interfaces must be structural
      _tp.report( iface, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_ONLY_STRUCTURAL_INTERFACE_ALLOWED_HERE.get( iface.toString() ) );
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

  private void checkExtensionClassError( JCTree.JCClassDecl typeDecl )
  {
    JavacPlugin javacPlugin = JavacPlugin.instance();
    if( javacPlugin == null )
    {
      return;
    }

    if( !isExtensionClass( typeDecl ) )
    {
      return;
    }

    String extendedFqn = getExtendedClassName();
    if( javacPlugin.getJavaInputFiles().stream().anyMatch( pair -> pair.getFirst().equals( extendedFqn ) ) )
    {
      _tp.report( typeDecl, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_CANNOT_EXTEND_SOURCE_FILE.get( extendedFqn ) );
    }
  }

  private void verifyExtensionMethod( JCTree.JCMethodDecl tree )
  {
//    if( JavacPlugin.instance() == null )
//    {
//      // don't perform verification at runtime, slow
//      return;
//    }

    if( !isExtensionClass( _tp.getParent( tree ) ) )
    {
      return;
    }

    String extendedClassName = getExtendedClassName();
    if( extendedClassName == null )
    {
      return;
    }

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
          Symbol.ClassSymbol extendClassSym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), (JCTree.JCCompilationUnit)_tp.getCompilationUnit(), extendedClassName );
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

  private String getExtendedClassName()
  {
    String extendedClassName = _tp.getCompilationUnit().getPackageName().toString();
    int iExt = extendedClassName.indexOf( ExtensionManifold.EXTENSIONS_PACKAGE + '.' );
    if( iExt < 0 )
    {
      return null;
    }

    extendedClassName = extendedClassName.substring( iExt + ExtensionManifold.EXTENSIONS_PACKAGE.length() + 1 );
    return extendedClassName;
  }

  private boolean isExtensionClass( Tree parent )
  {
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
      Symbol.ClassSymbol reflectMethodClassSym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), (JCTree.JCCompilationUnit)_tp.getCompilationUnit(), RuntimeMethods.class.getName() );
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

      JCTree.JCMethodInvocation makeProxyCall = make.Apply( List.nil(), memberAccess( make, javacElems, RuntimeMethods.class.getName() + ".constructProxy" ), List.from( newArgs ) );
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
    Symbol.ClassSymbol reflectMethodClassSym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), (JCTree.JCCompilationUnit)_tp.getCompilationUnit(), RuntimeMethods.class.getName() );

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

    JCTree.JCMethodInvocation makeProxyCall = make.Apply( List.nil(), memberAccess( make, javacElems, RuntimeMethods.class.getName() + ".assignStructuralIdentity" ), List.from( newArgs ) );
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
      BasicJavacTask javacTask = ClassSymbols.instance( _sp.getTypeLoader().getModule() ).getJavacTask();
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
      if( meth.sym == null || !meth.sym.hasAnnotations() )
      {
        return null;
      }
      for( Attribute.Compound annotation : meth.sym.getAnnotationMirrors() )
      {
        if( annotation.type.toString().equals( ExtensionMethod.class.getName() ) )
        {
          String extensionClass = (String)annotation.values.get( 0 ).snd.getValue();
          boolean isStatic = (boolean)annotation.values.get( 1 ).snd.getValue();
          BasicJavacTask javacTask = (BasicJavacTask)_tp.getJavacTask(); //JavacHook.instance() != null ? (JavacTaskImpl)JavacHook.instance().getJavacTask() : ClassSymbols.instance( _sp.getTypeLoader().getModule() ).getJavacTask();
          Symbol.ClassSymbol extClassSym = ClassSymbols.instance( _sp.getTypeLoader().getModule() ).getClassSymbol( javacTask, extensionClass ).getFirst();
          if( extClassSym == null )
          {
            // This can happen during bootstrapping with Dark Java classes from Manifold itself
            // e.g., ManResolve is a darkj class Manifold uses, ManResolve uses String, which may have an extension class which needs ManResole...
            // So we short-circuit that here (ManResolve or any other darkj class used during bootstrapping doesn't really need to use extensions)
            return null;
          }
          Types types = Types.instance( javacTask.getContext() );
          outer:
          for( Symbol elem : IDynamicJdk.instance().getMembers( extClassSym ) )
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
      if( m.sym != null && !m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC ) )
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

  private Symbol.MethodSymbol resolveMethod( JCDiagnostic.DiagnosticPosition pos, Name name, Type qual, List<Type> args )
  {
    return resolveMethod( pos, _tp.getContext(), (JCTree.JCCompilationUnit)_tp.getCompilationUnit(), name, qual, args );
  }

  private static Symbol.MethodSymbol resolveMethod( JCDiagnostic.DiagnosticPosition pos, Context ctx, JCTree.JCCompilationUnit compUnit, Name name, Type qual, List<Type> args )
  {
    Resolve rs = Resolve.instance( ctx );
    AttrContext attrContext = new AttrContext();
    Env<AttrContext> env = new AttrContextEnv( pos.getTree(), attrContext );
    env.toplevel = compUnit;
    return rs.resolveInternalMethod( pos, env, qual, name, args, null );
  }
}
