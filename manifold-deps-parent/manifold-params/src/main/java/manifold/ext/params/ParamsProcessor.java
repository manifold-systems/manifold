/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.ext.params;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import manifold.api.type.ICompilerComponent;
import manifold.api.util.JavacDiagnostic;
import manifold.ext.params.rt.params;
import manifold.ext.params.rt.param_default;
import manifold.internal.javac.*;
import manifold.rt.api.Null;
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.Stack;
import manifold.util.ReflectUtil;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.sun.source.util.TaskEvent.Kind.*;
import static manifold.ext.params.ParamsIssueMsg.*;

public class ParamsProcessor implements ICompilerComponent, TaskListener
{
  private static final long RECORD = 1L << 61; // from Flags in newer JDKs

  private static final Map<String, String> paramsByName = new HashMap<>();

  private BasicJavacTask _javacTask;
  private Context _context;
  private TaskEvent _taskEvent;
  private ParentMap _parents;
  private Map<JCClassDecl, ArrayList<JCMethodDecl>> _recordCtors;

  @Override
  public void init( BasicJavacTask javacTask, TypeProcessor typeProcessor )
  {
    _javacTask = javacTask;
    _context = _javacTask.getContext();
    _parents = new ParentMap( () -> getCompilationUnit() );
    _recordCtors = new HashMap<>();

    if( JavacPlugin.instance() == null )
    {
      // does not function at runtime
      return;
    }

    // Ensure TypeProcessor follows this in the listener list e.g., so that params integrates with structural
    // typing and extension methods.
    typeProcessor.addTaskListener( this );
  }
  
  BasicJavacTask getJavacTask()
  {
    return _javacTask;
  }

  Context getContext()
  {
    return _context;
  }

  Tree getParent( Tree child )
  {
    return _parents.getParent( child );
  }

  public Types getTypes()
  {
    return Types.instance( getContext() );
  }

  public Names getNames()
  {
    return Names.instance( getContext() );
  }

  public TreeMaker getTreeMaker()
  {
    return TreeMaker.instance( getContext() );
  }

  public Symtab getSymtab()
  {
    return Symtab.instance( getContext() );
  }

  @Override
  public void tailorCompiler()
  {
    _context = _javacTask.getContext();
  }

  private CompilationUnitTree getCompilationUnit()
  {
    if( _taskEvent != null )
    {
      CompilationUnitTree compUnit = _taskEvent.getCompilationUnit();
      if( compUnit != null )
      {
        return compUnit;
      }
    }
    return JavacPlugin.instance() != null
      ? JavacPlugin.instance().getTypeProcessor().getCompilationUnit()
      : null;
  }

  @Override
  public boolean isSuppressed( JCDiagnostic.DiagnosticPosition pos, String issueKey, Object[] args )
  {
    if( issueKey.endsWith( "does.not.override.superclass" ) )
    {
      if( pos.getTree() instanceof JCAnnotation )
      {
        JCMethodDecl methodDecl = findEnclosing( pos.getTree(), JCMethodDecl.class );
        if( methodDecl == null )
        {
          return false;
        }
        return checkIndirectlyOverrides( methodDecl );
      }
    }
    return false;
  }

  private <C extends JCTree> C findEnclosing( Tree tree, Class<C> cls )
  {
    if( tree == null )
    {
      return null;
    }

    if( cls.isInstance( tree ) )
    {
      return (C)tree;
    }

    tree = JavacPlugin.instance().getTypeProcessor().getParent( tree, ((ManAttr)Attr.instance( getContext() )).getEnv().toplevel );
    return findEnclosing( tree, cls );
  }

  private boolean checkIndirectlyOverrides( JCMethodDecl optParamsMethod )
  {
    if( optParamsMethod.sym.isConstructor() || optParamsMethod.sym.isPrivate() || optParamsMethod.params.isEmpty() )
    {
      return false;
    }

    JCClassDecl classDecl = findEnclosing( optParamsMethod, JCClassDecl.class );
    Iterable<Symbol> methodOverloads = IDynamicJdk.instance().getMembers( classDecl.sym,
      m -> m instanceof MethodSymbol && m.name.equals( optParamsMethod.name ) );
    Set<Symbol> overloads = StreamSupport.stream( methodOverloads.spliterator(), false ).collect( Collectors.toSet() );
    for( Symbol potentialTelescopingMethod : methodOverloads )
    {
      MethodSymbol targetMethod = findTargetMethod( (MethodSymbol)potentialTelescopingMethod, (Set)overloads );
      if( targetMethod == optParamsMethod.sym  )
      {
        Check check = Check.instance( getContext() );
        if( (boolean)ReflectUtil.method( check, "isOverrider", Symbol.class ).invoke( potentialTelescopingMethod ) )
        {
          // at least one telescoping method overrides a super method
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void started( TaskEvent e )
  {
    if( e.getKind() != ENTER )
    {
      return;
    }

    _taskEvent = e;
    try
    {
      ensureInitialized( _taskEvent );

      for( Tree tree : e.getCompilationUnit().getTypeDecls() )
      {
        if( tree instanceof JCClassDecl )
        {
          JCClassDecl classDecl = (JCClassDecl)tree;
          if( e.getKind() == ENTER )
          {
            classDecl.accept( new Enter_Start() );
          }
        }
      }
    }
    finally
    {
      _taskEvent = null;
    }
  }

  @Override
  public void finished( TaskEvent e )
  {
    if( e.getKind() != ENTER && e.getKind() != ANALYZE )
    {
      return;
    }

    _taskEvent = e;
    try
    {
      ensureInitialized( _taskEvent );

      for( Tree tree : e.getCompilationUnit().getTypeDecls() )
      {
        if( tree instanceof JCClassDecl )
        {
          JCClassDecl classDecl = (JCClassDecl)tree;
          switch( e.getKind() )
          {
            case ENTER:
              classDecl.accept( new Enter_Finish() );
              break;
            case ANALYZE:
              classDecl.accept( new Analyze_Finish() );
              break;
          }
        }
      }
    }
    finally
    {
      _taskEvent = null;
    }
  }

  private class Enter_Start extends TreeTranslator
  {
    public Enter_Start()
    {
    }

    @Override
    public void visitClassDef( JCClassDecl tree )
    {
      handleRecord( tree );
      super.visitClassDef( tree );
    }

    // We temporarily generate the record's ctor so we can later call `processParams( ctor )` during Enter_Finish
    // with the initializers on the ctor. Note, we do not add this ctor to the class' defs, instead we add it to
    // _recordCtors
    private void handleRecord( JCClassDecl tree )
    {
      if( !tree.getKind().name().equals( "RECORD" ) )
      {
        return;
      }

      if( tree.defs == null ||
          tree.defs.stream().noneMatch( def -> isRecordParam( def ) && ((JCVariableDecl)def).init != null ) )
      {
        // not a record having one or more optional parameters
        return;
      }

      TreeMaker make = getTreeMaker();
      make.at( tree.pos );
      TreeCopier<?> copier = new TreeCopier<>( make );

      List<JCVariableDecl> params = List.nil();

      for( JCTree def : tree.defs )
      {
        if( isRecordParam( def ) )
        {
          JCVariableDecl copy = (JCVariableDecl)copier.copy( def );
          copy.mods.flags = copy.mods.flags & ~Modifier.PRIVATE;
          params = params.append( copy );
          ((JCVariableDecl)def).init = null; // records compile parameters directly as fields, gotta remove the init
        }
      }

      if( !params.isEmpty() )
      {
        // generate a ctor that preserves the opt param initializers, so we can process it later in Enter_Finish
        JCMethodDecl ctor = make.MethodDef( make.Modifiers( Flags.PUBLIC ), getNames().init,
                                            make.TypeIdent( TypeTag.VOID ), List.nil(), params, List.nil(),
                                            make.Block( 0L, List.nil() ), null );
        _recordCtors.computeIfAbsent( tree, __ -> new ArrayList<>() )
          .add( ctor );
      }
    }

    private boolean isRecordParam( JCTree def )
    {
      return def instanceof JCVariableDecl && (((JCVariableDecl)def).getModifiers().flags & RECORD) != 0;
    }
  }

  // Generate following methods corresponding with a method having optional parameters:
  // - default parameters forwarding method/constructor
  // - telescoping method/ctor overloads corresponding to opt params
  // - default value methods (only if the opt param method is overridable)
  private class Enter_Finish extends TreeTranslator
  {
    private final Stack<Pair<JCClassDecl, ArrayList<JCTree>>> _newDefs;

    public Enter_Finish()
    {
      _newDefs = new Stack<>();
    }

    private JCClassDecl classDecl()
    {
      return _newDefs.peek().fst;
    }

    @Override
    public void visitClassDef( JCClassDecl tree )
    {
      if( alreadyProcessed( tree ) )
      {
        // already processed for optional params, probably an annotation processor round e.g., for lombok.
        return;
      }

      _newDefs.push( new Pair<>( tree, new ArrayList<>() ) );
      try
      {
        processRecords( tree );
        super.visitClassDef( tree );
        identifyIllegalOverrides( tree );
        removeInitFromParams( tree );

        // add them to defs
        ArrayList<JCTree> addedDefs = _newDefs.peek().snd;
        if( !addedDefs.isEmpty() )
        {
          ArrayList<JCTree> newDefs = new ArrayList<>( tree.defs );
          newDefs.addAll( addedDefs );
          tree.defs = List.from( newDefs );

          // define method symbols and add them to the class symbol's members
          for( JCTree addedDef : addedDefs )
          {
            ReflectUtil.method( MemberEnter.instance( getContext() ), "memberEnter", JCTree.class, Env.class )
              .invoke( addedDef, Enter.instance( getContext() ).getClassEnv( tree.sym ) );
          }
        }
      }
      finally
      {
        _newDefs.pop();
      }
    }

    private void processRecords( JCClassDecl tree )
    {
      // record ctors, we generated these during enter_start
      // caveat: they aren't attributed -- no symbols/types
      ArrayList<JCMethodDecl> ctors = _recordCtors.get( tree );
      if( ctors != null )
      {
        for( Iterator<JCMethodDecl> iterator = ctors.iterator(); iterator.hasNext(); )
        {
          JCMethodDecl ctor = iterator.next();
          processParams( ctor, false );
          iterator.remove();
        }
      }
    }

    /**
     * Enforce rule: multiple *overridable* overloads with optional params are not allowed
     */
    private void identifyIllegalOverrides( JCClassDecl tree )
    {
      Map<Name, ArrayList<JCMethodDecl>> methodsByName = new HashMap<>();
      for( JCTree def : tree.defs )
      {
        if( def instanceof JCMethodDecl )
        {
          methodsByName.computeIfAbsent( ((JCMethodDecl)def).name, __-> new ArrayList<>() )
            .add( (JCMethodDecl)def );
        }
      }
      for( Map.Entry<Name, ArrayList<JCMethodDecl>> entry : methodsByName.entrySet() )
      {
        if( entry.getValue().size() > 1 &&
            // two or more method overloads having optional parameters
            entry.getValue().stream()
              .filter( m -> isOverridable( m.sym ) && m.params.stream().anyMatch( param -> param.init != null ) )
              .count() > 1 )
        {
          for( JCMethodDecl m : entry.getValue() )
          {
            if( m.params.stream().anyMatch( param -> param.init != null ) || findOverloadInSuperTypes( tree, m ) )
            {
              reportError( m, MSG_OPT_PARAM_OVERRIDABLE_METHOD_OVERLOAD_NOT_ALLOWED.get( entry.getKey() ) );
            }
          }
        }
      }
    }

    private void removeInitFromParams( JCClassDecl tree )
    {
      for( JCTree def : tree.defs )
      {
        if( def instanceof JCMethodDecl )
        {
          for( JCVariableDecl param : ((JCMethodDecl)def).params )
          {
            if( param.init != null )
            {
              param.init = null; // remove init just in case javac decides not to like it at some point
            }
          }
        }
      }
    }

    private boolean findOverloadInSuperTypes( JCClassDecl tree, JCMethodDecl method )
    {
      if( method.sym.isConstructor() )
      {
        return false;
      }

      MethodSymbol superMethod = findSuperMethod( method );
      if( superMethod != null )
      {
        // ignore super methods because the super method will be checked against methods in its class
        return false;
      }

      for( Type type : getTypes().closure( tree.sym.type ) )
      {
        if( type.tsym == tree.type.tsym || !(type.tsym instanceof ClassSymbol) )
        {
          // skip inquiring class, and non-Class types such as type vars
          continue;
        }

        Iterable<Symbol> matches = IDynamicJdk.instance().getMembers( (ClassSymbol)type.tsym,
          m -> m instanceof MethodSymbol &&
          !m.isConstructor() &&
          m.name.toString().equals( "$" + method.name ) &&
          isAccessible( (MethodSymbol)m, tree ) );
        if( matches.iterator().hasNext() )
        {
          return true;
        }
      }

      return false;
    }

    // retarded check until Resolve.isAccessible/env is better understood
    private boolean isAccessible( MethodSymbol member, JCClassDecl siteClass )
    {
      if( Modifier.isPublic( (int)member.flags_field ) )
      {
        return true;
      }

      ClassSymbol siteClassSym = siteClass.sym;
      ClassSymbol memberClassSym = member.enclClass();

      if( memberClassSym == siteClassSym ||
          siteClassSym.outermostClass() == memberClassSym.outermostClass() )
      {
        return true;
      }

      if( Modifier.isProtected( (int)member.flags_field ) )
      {
        return siteClassSym.isSubClass( memberClassSym, Types.instance( getContext() ) );
      }

      // package-private
      if( !Modifier.isPrivate( (int)member.flags_field ) )
      {
        return memberClassSym.packge().equals( siteClassSym.packge() );
      }

      return false;
    }

    private boolean alreadyProcessed( JCClassDecl tree )
    {
      for( JCTree def : tree.defs )
      {
        if( def instanceof JCMethodDecl )
        {
          JCMethodDecl methodDecl = (JCMethodDecl)def;
          if( methodDecl.mods.getAnnotations().stream().anyMatch(
            anno -> anno.getAnnotationType().toString().endsWith( params.class.getSimpleName() ) ) )
          {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public void visitMethodDef( JCMethodDecl methodDecl )
    {
      processParams( methodDecl, true );
      super.visitMethodDef( methodDecl );
    }

    private void processParams( JCMethodDecl methodDecl, boolean requireSymbols )
    {
      if( requireSymbols && methodDecl.sym == null )
      {
        return;
      }

      for( JCVariableDecl param : methodDecl.params )
      {
        if( param.init != null )
        {
          handleOptionalParams( methodDecl );
          break;
        }
      }
    }

    private void handleOptionalParams( JCMethodDecl optParamsMeth )
    {
      TreeMaker make = getTreeMaker();
      make.at( optParamsMeth.pos );

      TreeCopier<?> copier = new TreeCopier<>( make );

      ArrayList<JCTree> defs = _newDefs.peek().snd;

      MethodSymbol superMethod = findSuperMethod( optParamsMeth );

      if( isOverridable( optParamsMeth ) )
      {
        // Generate default value methods
        // (must gen these *before* makeParamsMethod() call below bc there we assign param.init where defaults are inherited)
        List<JCMethodDecl> defaultValueMethods = makeDefaultValueMethods( optParamsMeth, make, copier );
        defs.addAll( defaultValueMethods );
      }

      // Generate a method to handle defaults and delegate to the original method
      JCMethodDecl paramsMethod = makeParamsMethod( optParamsMeth, superMethod, make, copier );
      defs.add( paramsMethod );

      // Generate telescoping method overloads because:
      // - binary compatibility
      // - handles call sites having all positional arguments
      // - enables inheritance and indirect overriding of methods having opt params (the only viable way in the JVM)
      ArrayList<JCMethodDecl> telescopeMethods = makeTelescopeMethods( optParamsMeth, make, copier );
      defs.addAll( telescopeMethods );
    }

    private List<JCMethodDecl> makeDefaultValueMethods( JCMethodDecl optParamsMeth, TreeMaker make, TreeCopier<?> copier )
    {
      // generate def value methods only for explicit opt params declared in target method via param.init
      // each method must have preceding parameters passed in

      List<JCMethodDecl> result = List.nil();
      List<JCVariableDecl> priorParams = List.nil();
      for( int i = 0; i < optParamsMeth.params.size(); i++ )
      {
        JCVariableDecl param = optParamsMeth.params.get( i );
        if( param.init != null )
        {
          JCMethodDecl defaultValueMethod = makeDefaultValueMethod( optParamsMeth, make, copier, copier.copy( priorParams ), param );
          result = result.append( defaultValueMethod );
        }
        priorParams = priorParams.append( param );
      }
      return result;

    }

    private JCMethodDecl makeDefaultValueMethod( JCMethodDecl targetMethod, TreeMaker make, TreeCopier<?> copier,
                                                 List<JCVariableDecl> priorParams, JCVariableDecl param )
    {
      JCMethodDecl copy = copier.copy( targetMethod );

      copy.name = getNames().fromString( "$" + copy.name + "_" + param.name );
      long flags = targetMethod.getModifiers().flags;
      if( (flags & Flags.STATIC) == 0 &&
          (classDecl().mods.flags & Flags.INTERFACE) != 0 )
      {
        flags |= Flags.DEFAULT;
      }
      else if( (flags & Flags.ABSTRACT) != 0 )
      {
        flags &= ~Flags.ABSTRACT;
      }
      copy.mods.annotations = List.nil();
      copy.mods.flags = flags;

      // mark with @param_default for IJ use
      JCAnnotation paramsAnno = make.Annotation( memberAccess( make, param_default.class.getTypeName() ), List.nil() );
      copy.mods.annotations = copy.mods.annotations.append( paramsAnno );

      // Params
      copy.params = priorParams;
      copy.params.forEach( e -> e.init = null );

      // Return type
      copy.restype = copier.copy( param.vartype );

      copy.body = make.Block( 0L, List.of( make.Return( copier.copy( param.init ) ) ) );
      return copy;
    }

    private ArrayList<JCMethodDecl> makeTelescopeMethods( JCMethodDecl optParamsMeth, TreeMaker make, TreeCopier<?> copier )
    {
      int optParamsCount = 0;
      for( JCVariableDecl p : optParamsMeth.params )
      {
        if( p.init != null )
        {
          optParamsCount++;
        }
      }

      // start with a method having all the required params and forwarding all default param values,
      // end with having all the optional params but the last one as required params (since the original method has all the params as required)
      ArrayList<JCMethodDecl> result = new ArrayList<>();
      for( int i = 0; i < optParamsCount; i++ )
      {
        JCMethodDecl telescopeMethod = makeTelescopeMethod( optParamsMeth, make, copier, i );
        result.add( telescopeMethod );
      }
      return result;
    }

    private JCMethodDecl makeTelescopeMethod( JCMethodDecl targetMethod, TreeMaker make, TreeCopier<?> copier, int optParamsInSig )
    {
      JCMethodDecl copy = copier.copy( targetMethod );

      ArrayList<JCVariableDecl> reqParams = new ArrayList<>();
      ArrayList<JCVariableDecl> optParams = new ArrayList<>();
      for( JCVariableDecl p : copy.params )
      {
        if( p.init == null )
        {
          reqParams.add( p );
        }
        else
        {
          p.init = null; // don't need this any longer
          optParams.add( p );
        }
      }

      List<Name> targetParams = List.from( copy.params.stream().map( e -> e.name ).collect( Collectors.toList() ) );

      // telescope interface methods have default impls that forward to original
      long flags = targetMethod.getModifiers().flags;
      if( (flags & Flags.STATIC) == 0 &&
          (classDecl().mods.flags & Flags.INTERFACE) != 0 )
      {
        flags |= Flags.DEFAULT;
      }
      else if( (flags & Flags.ABSTRACT) != 0 )
      {
        flags &= ~Flags.ABSTRACT;
      }
      copy.mods.flags = flags;
      // mark with @params for IJ use
      JCAnnotation paramsAnno = make.Annotation(
        memberAccess( make, params.class.getTypeName() ),
        List.of( paramsString( targetMethod.params.stream().map( p -> p.name.toString() ) ) ) );
      copy.mods.annotations = copy.mods.annotations.append( paramsAnno );

      // Params
      ArrayList<JCVariableDecl> params = new ArrayList<>();
      ArrayList<JCExpression> args = new ArrayList<>();
      for( JCVariableDecl reqParam : reqParams )
      {
        params.add( make.VarDef( make.Modifiers( Flags.PARAMETER ), reqParam.name, reqParam.vartype, null ) );
        args.addAll( makeTupleArg( make, reqParam.name ) );
      }
      for( int i = 0; i < optParamsInSig; i++ )
      {
        JCVariableDecl optParam = optParams.get( i );
        int index = targetParams.indexOf( optParam.name );
        params.add( index, optParam );
        args.addAll( index * 2, makeTupleArg( make, optParam.name ) );
      }
      if( args.stream().anyMatch( Objects::isNull ) )
      {
        throw new IllegalStateException( "null " );
      }
      copy.params = List.from( params );

      JCExpression tupleExpr = make.Parens(
        make.Apply( List.nil(), make.Ident( getNames().fromString( "$manifold_tuple" ) ), List.from( args ) ) );

      // body
      JCMethodInvocation forwardCall;
      if( targetMethod.name.equals( getNames().init ) )
      {
        forwardCall = make.Apply( List.nil(), make.Ident( getNames()._this ), List.of( tupleExpr ) );
      }
      else
      {
        forwardCall = make.Apply( List.nil(), make.Ident( targetMethod.name ), List.of( tupleExpr ) );
      }
      JCStatement stmt;
      if( targetMethod.restype == null ||
          (targetMethod.restype instanceof JCPrimitiveTypeTree &&
           ((JCPrimitiveTypeTree)targetMethod.restype).typetag == TypeTag.VOID) )
      {
        stmt = make.Exec( forwardCall );
      }
      else
      {
        stmt = make.Return( forwardCall );
      }
      copy.body = make.Block( 0L, List.of( stmt ) );
      return copy;
    }

    private List<JCExpression> makeTupleArg( TreeMaker make, Name name )
    {
      return List.of( make.Apply( List.nil(), make.Ident( getNames().fromString( "$manifold_label" ) ), List.of( make.Ident( name ) ) ),
                      make.Ident( name ) );
    }

    // Because default value expressions can reference symbols within the declaring class, we make a sibling method
    // the evaluate teh expressions locally.
    // Make the method with a boolean parameters for each optional parameter, indicating if the optional parameter value
    // was supplied at the call site, or if the default value should be used, and forward to the original method:
    //   String foo(String name, int age = 100) {...}
    //   String $foo(String name,  boolean isAge,int age) {
    //     return foo(name, isAge ? age : 100);
    //   }
    // so we can call it using tuples for named args and optional params:
    // foo(name:"Scott");
    private JCMethodDecl makeParamsMethod( JCMethodDecl targetMethod, MethodSymbol superMethod, TreeMaker make, TreeCopier<?> copier )
    {
      Map<String, JCMethodInvocation> superOptParams = makeSuperDefaultValueMap( superMethod, make );

      // Method name & modifiers
      boolean isConstructor = targetMethod.getName().equals( getNames().init );
      Name name = getNames().fromString( (isConstructor ? "" : "$") + targetMethod.getName() );
      long mods = targetMethod.getModifiers().flags;
      if( (mods & Flags.STATIC) == 0 &&
          (classDecl().mods.flags & Flags.INTERFACE) != 0 )
      {
        mods |= Flags.DEFAULT;
      }
      else if( (mods & Flags.ABSTRACT) != 0 )
      {
        mods &= ~Flags.ABSTRACT;
      }
      JCModifiers modifiers = make.Modifiers( mods );

      JCAnnotation paramsAnno = make.Annotation(
        memberAccess( make, params.class.getTypeName() ), List.of( paramsString( paramNamesStream( targetMethod, superOptParams ) ) ) );
      modifiers.annotations = modifiers.annotations.append( paramsAnno );
      if( isOverridable( targetMethod ) )
      {
        // need this because annotations are not processed before we need to check for them
        paramsByName.put( targetMethod.sym.owner.name + "#$" + targetMethod.name,
                          paramNamesStream( targetMethod, superOptParams ).collect( Collectors.joining( "," ) ) );
      }

      // Throws
      List<JCExpression> thrown = copier.copy( targetMethod.thrown );

      // Type params
      List<JCTypeParameter> typeParams = copier.copy( targetMethod.getTypeParameters() );

      // Params
      List<JCVariableDecl> params = List.nil();
      List<JCVariableDecl> targetParams = targetMethod.params;
      List<JCExpression> args = List.nil();
      List<JCExpression> optArgs = List.nil();
      for( JCVariableDecl methParam : targetParams )
      {
        JCMethodInvocation superDefaultValue = superOptParams.get( methParam.name.toString() );
        if( methParam.init != null || superDefaultValue != null )
        {
          // default param "xxx" requires an "isXxx" param indicating whether the "xxx" should assume the default value
          Name isXxx = getNames().fromString( "$is" + ManStringUtil.capitalize( methParam.name.toString() ) );
          JCVariableDecl param = make.VarDef( make.Modifiers( Flags.PARAMETER ), isXxx, make.TypeIdent( TypeTag.BOOLEAN ), null );
          param.pos = make.pos;
          params = params.append( param );

          // build call to the default value method wrapper, which enables inheriting and overriding default values
          JCMethodInvocation defaultValueCall = make.Apply( List.nil(),
                                                            make.Ident( getNames().fromString( "$" + targetMethod.name + "_" + methParam.name ) ), optArgs );

          // pN = isPN ? pN : $foo_param(p1, p2, pN-1)  (assign is necessary to support refs to params in subsequent opt param default values)
          JCAssign assign = make.Assign( make.Ident( methParam.name ),
                                         make.Conditional( make.Ident( isXxx ), make.Ident( methParam.name ),
                                                           copier.copy( methParam.init == null
                                                                        ? (methParam.init = defaultValueCall)
                                                                        : !isOverridable( targetMethod ) ? methParam.init : (methParam.init = defaultValueCall) ) ) );
          // opt param
          args = args.append( assign );
        }
        else
        {
          // param is not optional, always forward it directly
          args = args.append( make.Ident( methParam.name ) );
        }
        optArgs = optArgs.append( make.Ident( methParam.name ) );

        JCExpression type = (JCExpression)copier.copy( methParam.getType() );
        JCVariableDecl param = make.VarDef( make.Modifiers( Flags.PARAMETER ), methParam.name, type, null );
        param.pos = make.pos;
        params = params.append( param );
      }

      // Return type
      final JCExpression resType = (JCExpression)copier.copy( targetMethod.getReturnType() );

      // body
      JCMethodInvocation forwardCall;
      if( targetMethod.name.equals( getNames().init ) )
      {
        // constructor
        forwardCall = make.Apply( List.nil(), make.Ident( getNames()._this ), args );
      }
      else
      {
        // method
        forwardCall = make.Apply( List.nil(), make.Ident( targetMethod.name ), args );
      }

      JCStatement forwardStmt;
      if( targetMethod.restype == null ||
          (targetMethod.restype instanceof JCPrimitiveTypeTree &&
           ((JCPrimitiveTypeTree)targetMethod.restype).typetag == TypeTag.VOID) )
      {
        forwardStmt = make.Exec( forwardCall );
      }
      else
      {
        forwardStmt = make.Return( forwardCall );
      }
      JCBlock body = make.Block( 0L, List.of( forwardStmt ) );

      return make.MethodDef( modifiers, name, resType, typeParams, params, thrown, body, null );
    }

    private Stream<String> paramNamesStream( JCMethodDecl targetMethod, Map<String, JCMethodInvocation> superOptParams )
    {
      return targetMethod.params.stream().map(
        e -> (e.init == null && superOptParams.get( e.name.toString() ) == null ? "" : "opt$") + e.name );
    }

    private Map<String, JCMethodInvocation> makeSuperDefaultValueMap( MethodSymbol superMethod, TreeMaker make )
    {
      if( superMethod == null )
      {
        return Collections.emptyMap();
      }

      String value = getParamAnnoValue( superMethod );
      if( value == null )
      {
        // superMethod is not an opt params method, nor does it override one
        return Collections.emptyMap();
      }

      TreeCopier<?> copier = new TreeCopier<>( make );

      Map<String, JCMethodInvocation> superOptParams = new HashMap<>();
      List<JCExpression> superOptArgs = List.nil();
      for( StringTokenizer tokenizer = new StringTokenizer( value, "," ); tokenizer.hasMoreTokens(); )
      {
        String paramName = tokenizer.nextToken();
        if( paramName.startsWith( "opt$" ) )
        {
          paramName = paramName.substring( "opt$".length() );
          superOptParams.put( paramName, make.Apply( List.nil(),
                                                     make.Ident( getNames().fromString( "$" + superMethod.name + "_" + paramName ) ), copier.copy( superOptArgs ) ) );
        }
        superOptArgs = superOptArgs.append( make.Ident( getNames().fromString( paramName ) ) );
      }
      return superOptParams;
    }

    private String getParamAnnoValue( MethodSymbol superMethod )
    {
      Symbol superClass = superMethod.owner;
      String value = null;
      for( Symbol m: IDynamicJdk.instance().getMembersByName( (ClassSymbol)superClass, getNames().fromString( "$" + superMethod.name ) ) )
      {
        if( m instanceof MethodSymbol )
        {
          params paramsAnno = m.getAnnotation( params.class );
          if( paramsAnno != null )
          {
            value = paramsAnno.value();
          }
          else
          {
            value = paramsByName.get( m.owner.name + "#" + m.name );
          }

          if( value != null && value.contains( "opt$" ) )
          {
            break;
          }
        }
      }
      return value;
    }

    private MethodSymbol findSuperMethod( JCMethodDecl method )
    {
      MethodSymbol superMethod = findSuperMethod_NoParamCheck( method );
      if( superMethod == null )
      {
        return null;
      }

      // check that param names match
      checkParamNames( method, superMethod );
      return superMethod;
    }

    private void checkParamNames( JCMethodDecl method, MethodSymbol superMethod )
    {
      List<Symbol.VarSymbol> superParams = superMethod.params;
      for( int i = 0, paramsSize = superParams.size(); i < paramsSize; i++ )
      {
        Symbol.VarSymbol superParam = superParams.get( i );
        JCVariableDecl param = method.params.get( i );
        if( !superParam.name.equals( param.name ) )
        {
          reportError( param, MSG_OPT_PARAM_NAME_MISMATCH.get( param.name.toString(), superParam.name.toString() ) );
        }
      }
    }

    private MethodSymbol findSuperMethod_NoParamCheck( JCMethodDecl method )
    {
      if( !couldOverride( method ) )
      {
        return null;
      }

      Symbol superMethod = findSuperMethod( classDecl().sym, method.name, method.sym.type );
      if( superMethod != null )
      {
        // directly overrides super method
        return (MethodSymbol)superMethod;
      }

      // check for indirect override where overrider has additional optional parameters

      int lastPositional = -1;
      List<Type> requiredForOverride = List.nil();
      List<Type> remainingOptional = List.nil();
      List<JCVariableDecl> params = method.params;
      for( int i = 0; i < params.size(); i++ )
      {
        JCVariableDecl param = params.get( i );
        if( param.init == null )
        {
          lastPositional = i;
        }
      }
      for( int i = 0; i <= lastPositional; i++ )
      {
        requiredForOverride = requiredForOverride.append( params.get( i ).sym.type );
      }
      for( int i = lastPositional + 1; i < params.size(); i++ )
      {
        remainingOptional = remainingOptional.append( params.get( i ).sym.type );
      }

      for( int i = lastPositional + remainingOptional.size(); i > lastPositional; i-- ) // excludes last param bc we already checked for a direct override up top
      {
        java.util.List<Type> l = params.stream().map( e -> e.sym.type ).collect( Collectors.toList() ).subList( 0, i );
        List<Type> superSig = List.from( l );
        Type methodTypeWithParameters = getTypes().createMethodTypeWithParameters( method.sym.type, superSig );
        superMethod = findSuperMethod( classDecl().sym, method.name, methodTypeWithParameters );
        if( superMethod != null )
        {
          return (MethodSymbol)superMethod;
        }
      }
      return null;
    }

    // search the ancestry for the super method, if the direct super method does not have optional parameters, search for
    // its direct super method, and so on until a super method with optional parameters is found, otherwise return null.
    private Symbol findSuperMethod( Symbol.TypeSymbol owner, Name name, Type candidate )
    {
      for( Type sup : getTypes().closure( owner.type ) )
      {
        if( sup == owner.type || !(sup.tsym instanceof ClassSymbol) )
        {
          // skip inquiring class, and non-Class types such as type vars
          continue;
        }
        Iterable<Symbol> members = IDynamicJdk.instance().getMembers( (ClassSymbol)sup.tsym,
          e -> e instanceof MethodSymbol && isOverridable( (MethodSymbol)e ) && e.name.equals( name ) );
        for( Symbol e : members )
        {
          if( getTypes().isSubSignature( candidate, e.type ) )
          {
            String value = getParamAnnoValue( (MethodSymbol)e );
            if( value == null )
            {
              // the direct superMethod does not define or override any optional parameters, but it could override a
              // method that does, and if so use that as the superMethod here.
              e = findSuperMethod( (Symbol.TypeSymbol)e.owner, name, e.type );
            }
            return e;
          }
        }
      }
      return null;
    }
  }

  private boolean isOverridable( JCMethodDecl method )
  {
    return !method.getName().equals( getNames().init ) && isOverridable( method.sym );
  }
  private boolean isOverridable( MethodSymbol msym )
  {
    return msym != null && !msym.isStatic() && !msym.isPrivate() && !msym.type.isFinal() && !msym.isConstructor();
  }

  private boolean couldOverride( JCMethodDecl targetMethod )
  {
    if( targetMethod.name.equals( getNames().init ) )
    {
      return false;
    }

    MethodSymbol msym = targetMethod.sym;
    return msym != null && !msym.isStatic() && !msym.isPrivate() && !msym.isConstructor();
  }

  private MethodSymbol findParamsMethod( MethodSymbol msym )
  {
    Iterable<Symbol> members = IDynamicJdk.instance().getMembers( (ClassSymbol)msym.owner,
      e -> e instanceof MethodSymbol && !e.isStatic() && msym.name.toString().equals( "$" + e.name ) );
    return findTargetMethod( msym,
      StreamSupport.stream( members.spliterator(), false ).map( e -> (MethodSymbol)e ).collect( Collectors.toSet() ) );
  }

  private JCLiteral paramsString( Stream<String> paramNames )
  {
    return getTreeMaker().Literal( paramNames.collect( Collectors.joining( "," ) ) );
  }

  class Analyze_Finish extends TreeTranslator
  {
    private final Stack<JCClassDecl> _classDecls = new Stack<>();

    private JCClassDecl classDecl()
    {
      return _classDecls.peek();
    }
    
    @Override
    public void visitClassDef( JCClassDecl tree )
    {
      _classDecls.push( tree );
      try
      {
//## todo: handle anonymous classes, see InheritanceTest#MyInterface. Forwarding to Enter_Finish here because otherwise
//     the anon class and its defs are not attributed (no symbols) -- anonymous classes are attributed during Analyze of
//     containing class. Something is wrong though, hence saving this for a later time.
//        if( tree.sym.isAnonymous() )
//        {
//          new Enter_Finish().visitClassDef( tree );
//        }
        super.visitClassDef( tree );
      }
      finally
      {
        _classDecls.pop();
      }
    }

    @Override
    public void visitMethodDef( JCMethodDecl methodDecl )
    {
      super.visitMethodDef( methodDecl );

      if( methodDecl.mods.getAnnotations().stream().anyMatch(
        anno -> anno.getAnnotationType().type != null && anno.getAnnotationType().type.tsym.getQualifiedName().toString().equals( params.class.getTypeName() ) ) )
      {
        checkDuplication( methodDecl );
      }

      addSyntheticModifier( methodDecl );
    }

    private void addSyntheticModifier( JCMethodDecl methodDecl )
    {
      if( methodDecl.sym == null || methodDecl.sym.isConstructor() )
      {
        // adding BRIDGE to constructors makes compiler angry,
        // will have to use the IDE plugin to hide these :/
        return;
      }

      methodDecl.mods.getAnnotations().stream()
        .filter( anno -> {
          String annoTypeName = anno.getAnnotationType().type.tsym.getQualifiedName().toString();
          if( annoTypeName.equals( params.class.getTypeName() ) )
          {
            String paramNames = (String)anno.attribute.getElementValues().values().stream().findFirst().get().getValue();
            return paramNames.contains( "opt$" );
          }
          return annoTypeName.equals( param_default.class.getTypeName() );
        } )
        .findFirst().ifPresent( anno -> {
            methodDecl.sym.flags_field |= Flags.BRIDGE;
        } );
    }

    private void checkDuplication( JCMethodDecl telescopeMethod )
    {
      Set<MethodSymbol> methods = new LinkedHashSet<>();
      ManTypes.getAllMethods( classDecl().type, m -> m != null && m.name.equals( telescopeMethod.name ), methods );

      MethodSymbol targetMethod = findTargetMethod( telescopeMethod.sym, methods );
      if( targetMethod == null )
      {
        // probably a $foo_<param> overload variant
        return;
      }

      for( MethodSymbol msym : new ArrayList<>( methods ) )
      {
        if( msym != telescopeMethod.sym && getTypes().overrideEquivalent( msym.type, telescopeMethod.type ) )
        {
          String paramNames = targetMethod.params.stream()
            .map( p -> p.name.toString() )
            .collect( Collectors.joining( ", " ) );
          JCMethodDecl paramsMethodDecl = getTargetMethod( telescopeMethod.name, paramNames );
          if( msym.owner == classDecl().sym )
          {
            MethodSymbol paramsMethodFromMsym = findTargetMethod( msym, methods );
            if( paramsMethodFromMsym != null )
            {
              String signature = telescopeMethod.params.stream()
                .map( p -> p.type.tsym.getSimpleName() )
                .collect( Collectors.joining( ", " ) );
              reportError( paramsMethodDecl,
                MSG_OPT_PARAM_METHOD_CLASHES_WITH_SUBSIG
                  .get( targetMethod, msym, signature ) );
            }
            else
            {
              reportError( paramsMethodDecl,
                MSG_OPT_PARAM_METHOD_INDIRECTLY_CLASHES.get( targetMethod, msym ) );
            }
          }
          else if( !msym.isConstructor() && !msym.isPrivate() && !msym.isStatic() )
          {
            if( targetMethod.getAnnotation( Override.class ) == null )
            {
              reportWarning( paramsMethodDecl,
                MSG_OPT_PARAM_METHOD_INDIRECTLY_OVERRIDES
                  .get( targetMethod, msym, (msym.owner == null ? "<unknown>" : msym.owner.getSimpleName()) ) );
            }
          }
        }
      }
    }

    JCMethodDecl getTargetMethod( Name methodName, String paramNames )
    {
      for( JCTree def : classDecl().defs )
      {
        if( def instanceof JCMethodDecl && ((JCMethodDecl)def).name.equals( methodName ) )
        {
          JCMethodDecl m = (JCMethodDecl)def;
          if( paramNames.equals( m.params.stream().map( p -> p.name.toString() ).collect( Collectors.joining( ", ") ) ) )
          {
            return m;
          }
        }
      }
      throw new IllegalStateException( "Expected to find target optional params method for: " + methodName + "(" + paramNames + ")" );
    }

    @Override
    public void visitVarDef( JCVariableDecl tree )
    {
      super.visitVarDef( tree );

      if( tree.init != null && isNullType( tree.init.type ) )
      {
        tree.init = makeCast( tree.init, ((JCTree)tree).type == Type.noType ? getSymtab().objectType : ((JCTree)tree).type );
      }
    }

    private JCTypeCast makeCast( JCExpression expression, Type type )
    {
      TreeMaker make = TreeMaker.instance( getContext() );

      JCTypeCast castCall = make.TypeCast( type, expression );
      ((JCTree)castCall).type = type;
      castCall.pos = expression.pos;

      return castCall;
    }

    private boolean isNullType( Type t )
    {
      return t.tsym != null && Null.class.getTypeName().equals( t.tsym.getQualifiedName().toString() );
    }

    @Override
    public void visitNewClass( JCNewClass tree )
    {
      super.visitNewClass( tree );
    }
  }

  private JCAnnotation makeAnnotation( Class<?> anno )
  {
    TreeMaker make = TreeMaker.instance( getContext() );
    JCExpression staticAnno = memberAccess( make, anno.getName() );
    return make.Annotation( staticAnno, List.nil() );
  }

  private void ensureInitialized( TaskEvent e )
  {
    // ensure JavacPlugin is initialized, particularly for Enter since the order of TaskListeners is evidently not
    // maintained by JavaCompiler i.e., this TaskListener is added after JavacPlugin, but is notified earlier
    JavacPlugin javacPlugin = JavacPlugin.instance();
    if( javacPlugin != null )
    {
      javacPlugin.initialize( e );
    }
  }

  private JCExpression memberAccess( TreeMaker make, String path )
  {
    return memberAccess( make, path.split( "\\." ) );
  }

  private JCExpression memberAccess( TreeMaker make, String... components )
  {
    Names names = Names.instance( getContext() );
    JCExpression expr = make.Ident( names.fromString( (components[0]) ) );
    for( int i = 1; i < components.length; i++ )
    {
      expr = make.Select( expr, names.fromString( components[i] ) );
    }
    return expr;
  }

  private void reportWarning( JCTree location, String message )
  {
    report( Diagnostic.Kind.WARNING, location, message );
  }

  private void reportError( JCTree location, String message )
  {
    report( Diagnostic.Kind.ERROR, location, message );
  }

  private void report( Diagnostic.Kind kind, JCTree location, String message )
  {
    report( _taskEvent.getSourceFile(), location, kind, message );
  }
  public void report( JavaFileObject sourcefile, JCTree tree, Diagnostic.Kind kind, String msg )
  {
    IssueReporter<JavaFileObject> reporter = new IssueReporter<>( _javacTask::getContext );
    JavaFileObject file = sourcefile != null ? sourcefile : Util.getFile( tree, child -> getParent( child ) );
    reporter.report( new JavacDiagnostic( file, kind, tree.getStartPosition(), 0, 0, msg ) );
  }

  private MethodSymbol findTargetMethod( MethodSymbol telescopeMethod, Set<MethodSymbol> methods )
  {
    MethodSymbol tm = null;
    params anno = telescopeMethod.getAnnotation( params.class );
    if( anno != null )
    {
      tm = getTargetMethod( anno, methods );
    }
    return tm;
  }

  MethodSymbol getTargetMethod( params anno, Set<MethodSymbol> methods )
  {
    return methods.stream()
      .filter( m ->
        anno.value().replace( "opt$", "" ).equals( m.params.stream().map( p -> p.name.toString() ).collect( Collectors.joining( "," ) ) ) )
      .findFirst().orElse( null );
  }

}
