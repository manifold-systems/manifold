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
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
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
import manifold.ext.params.rt.bool;
import manifold.ext.params.rt.param_overload;
import manifold.ext.params.rt.params;
import manifold.ext.params.rt.param_default;
import manifold.internal.javac.*;
import manifold.rt.api.Null;
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.Stack;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.sun.source.util.TaskEvent.Kind.*;
import static manifold.api.util.JCTreeUtil.makeEmptyValue;
import static manifold.api.util.JCTreeUtil.memberAccess;
import static manifold.ext.ExtensionTransformer.tempify;
import static manifold.ext.params.ParamsIssueMsg.*;

public class ParamsProcessor implements ICompilerComponent, TaskListener
{
  private static final long RECORD = 1L << 61; // from Flags in newer JDKs

  private BasicJavacTask _javacTask;
  private Context _context;
  private TaskEvent _taskEvent;
  private ParentMap _parents;
  private Map<JCClassDecl, ArrayList<JCMethodDecl>> _recordCtors;
  private Map<String, String> _paramsByName;
  private Set<String> _processed;

  @Override
  public void init( BasicJavacTask javacTask, TypeProcessor typeProcessor )
  {
    _javacTask = javacTask;
    _context = _javacTask.getContext();
    _parents = new ParentMap( () -> getCompilationUnit() );
    _recordCtors = new HashMap<>();
    _paramsByName = new HashMap<>();
    _processed = new HashSet<>();

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
      //noinspection unchecked
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
      //noinspection unchecked,rawtypes
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
          switch( e.getKind() )
          {
            case ENTER:
              classDecl.accept( new Enter_Start() );
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
    public void visitApply( JCMethodInvocation tree )
    {
      super.visitApply( tree );
      reparentOptParamsCall( tree );
    }

    private void reparentOptParamsCall( JCMethodInvocation tree )
    {
      List<JCExpression> treeArgs = tree.args;
      if( treeArgs.size() != 1 )
      {
        // expecting a single tuple expression arg eg.
        return;
      }

      JCExpression singleArg = treeArgs.get( 0 );
      if( !(singleArg instanceof JCTree.JCParens) )
      {
        return;
      }
      while( singleArg instanceof JCTree.JCParens )
      {
        singleArg = ((JCTree.JCParens)singleArg).expr;
      }
      if( !(singleArg instanceof JCTree.JCMethodInvocation) ||
          !(((JCTree.JCMethodInvocation)singleArg).meth instanceof JCTree.JCIdent) ||
          !((JCTree.JCIdent)((JCTree.JCMethodInvocation)singleArg).meth).name.toString().equals( "$manifold_tuple" ) )
      {
        // not a tuple expr
        return;
      }

      if( tree.meth instanceof JCTree.JCIdent )
      {
        Name name = ((JCTree.JCIdent)tree.meth).name;
        if( name == getNames()._this || name == getNames()._super )
        {
          // super()/this() ctor call
          return;
        }
      }

      // put the call into a Paren expression to help make it easier to rewrite the calls as Let expressions in ManAttr
      TreeMaker make = getTreeMaker();
      result = make.Parens( tree );
    }

    @Override
    public void visitClassDef( JCClassDecl tree )
    {
      ArrayList<JCTree> newDefs = new ArrayList<>();
      handleRecord( tree, newDefs );
      super.visitClassDef( tree );
      tree.defs = tree.defs.appendList( List.from( newDefs ) );
    }

    // We temporarily generate the record's ctor so we can later call `processParams( ctor )` during Enter_Finish
    // with the initializers on the ctor. Note, we do not add this ctor to the class' defs, instead we add it to
    // _recordCtors
    private void handleRecord( JCClassDecl tree, ArrayList<JCTree> newDefs )
    {
      if( _recordCtors.containsKey( tree ) )
      {
        // record already processed, this may be an annotation processor round
        return;
      }

      if( !tree.getKind().name().equals( "RECORD" ) )
      {
        return;
      }

      TreeMaker make = getTreeMaker();
      make.at( tree.pos );
      TreeCopier<?> copier = new TreeCopier<>( make );

      //## todo: this works, but will probably implement this via extension method instead -- once optional params works w ext methods
      addRecordCopyMethod( tree, copier, make, newDefs );

      if( tree.defs == null ||
          tree.defs.stream().noneMatch( def -> isRecordParam( def ) && ((JCVariableDecl)def).init != null ) )
      {
        // not a record having one or more optional parameters
        return;
      }

      List<JCVariableDecl> params = List.nil();

      for( JCTree def : tree.defs )
      {
        if( isRecordParam( def ) )
        {
          JCVariableDecl copy = (JCVariableDecl)copier.copy( def );
          copy.mods.flags = Flags.PARAMETER;
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

    private void addRecordCopyMethod( JCClassDecl tree, TreeCopier<?> copier, TreeMaker make, ArrayList<JCTree> newDefs )
    {
      boolean alreadyProcessed = tree.defs.stream().anyMatch(
        def -> def instanceof JCMethodDecl && "copyWith".equals( ((JCMethodDecl)def).name.toString() ) );
      if( alreadyProcessed )
      {
        return;
      }

      List<JCVariableDecl> params = List.nil();

      for( JCTree def : tree.defs )
      {
        if( isRecordParam( def ) )
        {
          JCVariableDecl copy = (JCVariableDecl)copier.copy( def );
          copy.mods.flags = Flags.PARAMETER;
          copy.mods.annotations = List.nil();
          copy.init = make.Select( make.Ident( getNames()._this ), copy.name );
          params = params.append( copy );
        }
      }

      // generate a copyWith() method with optional parameters for all the record's components
      List<JCExpression> args = List.from( params.stream().map( p -> make.Ident( p.name ) ).collect( Collectors.toList() ) );
      JCMethodDecl copyMeth = make.MethodDef( make.Modifiers( Flags.PUBLIC ), getNames().fromString( "copyWith" ),
                                              make.Ident( tree.name ), List.nil(), params, List.nil(),
                                              make.Block( 0L, List.of( make.Return( make.NewClass( null, null, make.Ident( tree.name ), args, null ) ) ) ), null );
      newDefs.add( copyMeth );
    }
  }

  private boolean isRecordParam( JCTree def )
  {
    return def instanceof JCVariableDecl && (((JCVariableDecl)def).getModifiers().flags & RECORD) != 0;
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
        markProcessed( tree );
        _newDefs.pop();
      }
    }

    private void markProcessed( JCClassDecl tree )
    {
      if( tree.sym == null || tree.sym.getQualifiedName() == null || tree.sym.getQualifiedName().isEmpty() )
      {
        // anonymous class, use the processed state of the enclosing class for these (see alreadyProcessed() below)
        return;
      }
      _processed.add( tree.sym.getQualifiedName().toString() );
    }
    private boolean alreadyProcessed( JCClassDecl classDecl )
    {
      while( classDecl.sym == null )  // null sym -> anonymous class -- use enclosing class to indicate processed
      {
        classDecl = getEnclosingClass( classDecl );
        if( classDecl == null )
        {
          // throw here? should never happen?
          return false;
        }
      }

      return _processed.contains( classDecl.sym.getQualifiedName().toString() );
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
      TreeMaker make = getTreeMaker().at( optParamsMeth.pos );

      TreeCopier<?> copier = new TreeCopier<>( make );

      ArrayList<JCTree> defs = _newDefs.peek().snd;

      MethodSymbol superMethod = findSuperMethod( optParamsMeth );
      Map<String, JCMethodInvocation> superOptParams = makeSuperDefaultValueMap( superMethod, make );

      boolean isConstructor = optParamsMeth.name == getNames().init;
      if( isConstructor )
      {
        // Generate a method/constructor to handle defaults and delegate to the original method.
        // Note this technique is necessary for constructors to support super()/this() calls, otherwise the LetExpr technique
        // used with method call sites create var decls that violate the "super()/this() must be the first statement in the ctor" rule.
        JCMethodDecl paramsMethod = makeParamsMethod( optParamsMeth, superOptParams, make, copier );
        defs.add( paramsMethod );

        ArrayList<JCMethodDecl> telescopeOptParamMethods = makeTelescopeOptParamMethods( optParamsMeth, superOptParams, make, copier );
        defs.addAll( telescopeOptParamMethods );
      }
      else
      {
        // Generate default value methods (necessary for: polymorphism wrt default values, LetExpr technique, referencing preceding params, binary compatibility)
        List<JCMethodDecl> defaultValueMethods = makeDefaultValueMethods( optParamsMeth, make, copier );
        defs.addAll( defaultValueMethods );

        // annotate the method to preserve the param names and to identify which params are optional
        tagParamsMethod( optParamsMeth, superMethod, make );
      }

      // Generate (positional) telescoping method overloads because:
      // - binary compatibility
      // - handles call sites having all positional arguments
      // - enables inheritance and indirect overriding of methods having opt params (the only viable way in the JVM)
      ArrayList<JCMethodDecl> telescopeMethods = makeTelescopeMethods( optParamsMeth, superOptParams, make, copier );
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
      JCAnnotation paramsAnno = make.Annotation( memberAccess( make, getNames(), param_default.class.getTypeName() ), List.nil() );
      copy.mods.annotations = copy.mods.annotations.append( paramsAnno );

      // Params
      copy.params = priorParams;
      copy.params.forEach( e -> e.init = null );

      // Return type
      copy.restype = copier.copy( param.vartype );

      copy.body = make.Block( 0L, List.of( make.Return( copier.copy( param.init ) ) ) );
      return copy;
    }

    private ArrayList<JCMethodDecl> makeTelescopeOptParamMethods( JCMethodDecl optParamsMeth, Map<String, JCMethodInvocation> superOptParams, TreeMaker make, TreeCopier<?> copier )
    {
      int firstTrailingOptParam = firstTrailingOptParamIndex( optParamsMeth, superOptParams );
      if( firstTrailingOptParam < 0 || optParamsMeth.params.size()-1 == firstTrailingOptParam )
      {
        return new ArrayList<>();
      }

      // start with a method having only one trailing opt param and forwarding remaining default param values,
      // end with having all the all trailing opt params but the last one
      ArrayList<JCMethodDecl> result = new ArrayList<>();
      for( int i = firstTrailingOptParam; i < optParamsMeth.params.size() - 1; i++ )
      {
        JCMethodDecl paramsMethod = makeTelescopeOptParamMethod( optParamsMeth, superOptParams, make, copier, i );
        result.add( paramsMethod );
      }
      return result;
    }

    private int firstTrailingOptParamIndex( JCMethodDecl optParamsMeth, Map<String, JCMethodInvocation> superOptParams )
    {
      int firstTrailingOptParam = -1;
      for( int i = 0; i < optParamsMeth.params.size(); i++ )
      {
        JCVariableDecl param = optParamsMeth.params.get( i );
        JCMethodInvocation superDefaultValue = superOptParams.get( param.name.toString() );
        if( param.init != null || superDefaultValue != null )
        {
          if( firstTrailingOptParam < 0 )
          {
            firstTrailingOptParam = i;
          }
        }
        else
        {
          firstTrailingOptParam = -1;
        }
      }
      return firstTrailingOptParam;
    }

    private JCMethodDecl makeTelescopeOptParamMethod( JCMethodDecl targetMethod, Map<String, JCMethodInvocation> superOptParams, TreeMaker make, TreeCopier<?> copier, int lastOptParamToKeep )
    {
      JCMethodDecl copy = copier.copy( targetMethod );

      // name
      boolean isConstructor = targetMethod.getName().equals( getNames().init );
      copy.name = getNames().fromString( (isConstructor ? "" : "$") + targetMethod.getName() );

      // mods & annotations
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
      // mark with @param_overload for binary/backward compatibility
      JCAnnotation paramsAnno = make.Annotation(
        memberAccess( make, getNames(), param_overload.class.getTypeName() ), List.nil() );
      copy.mods.annotations = copy.mods.annotations.append( paramsAnno );

      // params + forwarding args
      List<JCVariableDecl> params = List.nil();
      List<JCExpression> args = List.nil();
      for( int i = 0; i < targetMethod.params.size(); i++ )
      {
        JCVariableDecl methParam = targetMethod.params.get( i );
        JCMethodInvocation superDefaultValue = superOptParams.get( methParam.name.toString() );
        if( methParam.init != null || superDefaultValue != null )
        {
          if( i <= lastOptParamToKeep )
          {
            // default param "xxx" requires an "isXxx" param indicating whether the "xxx" should assume the default value
            Name isXxx = getNames().fromString( "$is" + ManStringUtil.capitalize( methParam.name.toString() ) );
            JCVariableDecl isParam = make.VarDef( make.Modifiers( Flags.PARAMETER ), isXxx, memberAccess( make, getNames(), bool.class.getTypeName() ), null );
            params = params.append( isParam );

            args = args.append( make.Ident( isXxx ) );
            args = args.append( make.Ident( methParam.name ) );
          }
          else
          {
            args = args.append( memberAccess( make, getNames(), bool.class.getTypeName() + "." + bool.False  ) );
            args = args.append( makeEmptyValue( getParamType( methParam ), make, getTypes(), getSymtab() ) );
          }
        }
        else
        {
          args = args.append( make.Ident( methParam.name ) );
        }

        if( i <= lastOptParamToKeep )
        {
          JCExpression type = (JCExpression)copier.copy( methParam.getType() );
          JCVariableDecl param = make.VarDef( make.Modifiers( Flags.PARAMETER ), methParam.name, type, null );
          param.pos = make.pos;
          params = params.append( param );
        }
      }
      copy.params = params;

      // body (forwarding call)
      JCMethodInvocation forwardCall;
      if( targetMethod.name.equals( getNames().init ) )
      {
        forwardCall = make.Apply( List.nil(), make.Ident( getNames()._this ), args );
      }
      else
      {
        forwardCall = make.Apply( List.nil(), make.Ident( getNames().fromString( "$" + targetMethod.name ) ), args );
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

    private Type getParamType( JCVariableDecl methParam )
    {
      if( methParam.sym != null )
      {
        return methParam.sym.type;
      }
      if( (classDecl().sym.flags_field & RECORD) != 0 )
      {
        // throw-away record constructors are created during ENTER:start so we can do our thing, so we need to get the type from record components
        for( JCTree def : classDecl().defs )
        {
          if( def instanceof JCVariableDecl && ((JCVariableDecl)def).name == methParam.name )
          {
            return ((JCVariableDecl)def).sym.type;
          }
        }
      }
      throw new IllegalStateException( "Expecting a symbol for parameter: " + methParam.name );
    }

    private ArrayList<JCMethodDecl> makeTelescopeMethods( JCMethodDecl optParamsMeth, Map<String, JCMethodInvocation> superOptParams, TreeMaker make, TreeCopier<?> copier )
    {
      int optParamsCount = 0;
      for( JCVariableDecl p : optParamsMeth.params )
      {
        JCMethodInvocation superDefaultValue = superOptParams.get( p.name.toString() );
        if( p.init != null || superDefaultValue != null )
        {
          optParamsCount++;
        }
      }

      // start with a method having all the required params and forwarding all default param values,
      // end with having all the optional params but the last one as required params (since the original method has all the params as required)
      ArrayList<JCMethodDecl> result = new ArrayList<>();
      for( int i = 0; i < optParamsCount; i++ )
      {
        JCMethodDecl telescopeMethod = makeTelescopeMethod( optParamsMeth, superOptParams, make, copier, i );
        result.add( telescopeMethod );
      }
      return result;
    }

    private JCMethodDecl makeTelescopeMethod( JCMethodDecl targetMethod, Map<String, JCMethodInvocation> superOptParams, TreeMaker make, TreeCopier<?> copier, int optParamsInSig )
    {
      JCMethodDecl copy = copier.copy( targetMethod );

      ArrayList<JCVariableDecl> reqParams = new ArrayList<>();
      ArrayList<JCVariableDecl> optParams = new ArrayList<>();
      for( JCVariableDecl p : copy.params )
      {
        JCMethodInvocation superDefaultValue = superOptParams.get( p.name.toString() );
        if( p.init == null && superDefaultValue == null )
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
        memberAccess( make, getNames(), params.class.getTypeName() ),
        List.of( paramsString( targetMethod.params.stream().map( p -> p.name.toString() ) ) ) );
      copy.mods.annotations = List.from( copy.mods.annotations.stream()
                                           .filter( anno -> !anno.getAnnotationType().toString().equals( params.class.getTypeName() ) &&
                                                            !anno.getAnnotationType().toString().endsWith( Override.class.getSimpleName() ) )
                                           .collect( Collectors.toList() ) );
      // annotations consist of all target method's minus @Override and
      // also minus @params because that @params has "opt$" encoded which we don't want here
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
        params.add( index, make.VarDef( make.Modifiers( Flags.PARAMETER ), optParam.name, optParam.vartype, null ) );
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

    private void tagParamsMethod( JCMethodDecl targetMethod, MethodSymbol superMethod, TreeMaker make )
    {
//      Log.instance( getContext() ).printRawLines( "tagParamsMethod: " + targetMethod.name );
      Map<String, JCMethodInvocation> superOptParams = makeSuperDefaultValueMap( superMethod, make );
      JCAnnotation paramsAnno = make.Annotation(
        memberAccess( make, getNames(), params.class.getTypeName() ), List.of( paramsString( paramNamesStream( targetMethod, superOptParams ) ) ) );
      targetMethod.mods.annotations = targetMethod.mods.annotations.append( paramsAnno );
      if( isOverridable( targetMethod ) )
      {
        // need this because annotations are not processed before we need to check for them
        _paramsByName.put( targetMethod.sym.owner.getQualifiedName() + "#$" + targetMethod.name,
                           paramNamesStream( targetMethod, superOptParams ).collect( Collectors.joining( "," ) ) );
      }

      // Must manually post task to process annotations, since we are in ENTER:finished
      Env<AttrContext> env = Enter.instance( getContext() ).getTopLevelEnv( (JCCompilationUnit)getCompilationUnit() );
      Env<?> localEnv = (Env<?>)ReflectUtil.method( MemberEnter.instance( getContext() ), "methodEnv", JCMethodDecl.class, Env.class )
                       .invoke( targetMethod, env );
      if( !JreUtil.isJava9orLater() )
      {
        ReflectUtil.method( MemberEnter.instance( getContext() ), "annotateLater", List.class, Env.class, Symbol.class, JCDiagnostic.DiagnosticPosition.class )
          .invoke( targetMethod.mods.annotations, localEnv, targetMethod.sym, targetMethod.pos() );
      }
      else
      {
        ReflectUtil.method( Annotate.instance( getContext() ), "annotateLater", List.class, Env.class, Symbol.class, JCDiagnostic.DiagnosticPosition.class )
          .invoke( targetMethod.mods.annotations, localEnv, targetMethod.sym, targetMethod.pos() );
      }
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
    private JCMethodDecl makeParamsMethod( JCMethodDecl targetMethod, Map<String, JCMethodInvocation> superOptParams, TreeMaker make, TreeCopier<?> copier )
    {
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
        memberAccess( make, getNames(), params.class.getTypeName() ), List.of( paramsString( paramNamesStream( targetMethod, superOptParams ) ) ) );
      modifiers.annotations = modifiers.annotations.append( paramsAnno );
      if( isOverridable( targetMethod ) )
      {
        // need this because annotations are not processed before we need to check for them
        _paramsByName.put( targetMethod.sym.owner.getQualifiedName() + "#$" + targetMethod.name,
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
          JCVariableDecl param = make.VarDef( make.Modifiers( Flags.PARAMETER ), isXxx, memberAccess( make, getNames(), bool.class.getTypeName() ), null );
          param.pos = make.pos;
          params = params.append( param );

          // build call to the default value method wrapper, which enables inheriting and overriding default values
          JCMethodInvocation defaultValueCall = make.Apply( List.nil(),
                                                            make.Ident( getNames().fromString( "$" + targetMethod.name + "_" + methParam.name ) ), optArgs );

          // pN = isPN ? pN : $foo_param(p1, p2, pN-1)  (assign is necessary to support refs to params in subsequent opt param default values)
          JCAssign assign = make.Assign( make.Ident( methParam.name ),
                                         make.Conditional( make.Binary( Tag.EQ, make.Ident( isXxx ), memberAccess( make, getNames(), bool.class.getTypeName() + "." + bool.True ) ),
                                                           make.Ident( methParam.name ),
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

    // checks params names of super method using @params of generated method
    private void checkParamNames( JCMethodDecl method, MethodSymbol superMethod )
    {
      List<String> superParams = getParamNamesFromSuperMethod( superMethod );
      if( superParams == null )
      {
        return;
      }

      for( int i = 0, paramsSize = superParams.size(); i < paramsSize; i++ )
      {
        String superParam = superParams.get( i );
        JCVariableDecl param = method.params.get( i );
        if( !superParam.equals( param.name.toString() ) )
        {
          reportError( param, MSG_OPT_PARAM_NAME_MISMATCH.get( param.name.toString(), superParam ) );
        }
      }
    }

    private List<String> getParamNamesFromSuperMethod( MethodSymbol superMethod )
    {
      Pair<MethodSymbol, String> value = getParamAnnoValue( superMethod );
      if( value == null )
      {
        //todo: this should never be the case, throw here?
        return null;
      }
      List<String> superParams = List.nil();
      StringTokenizer tokenizer = new StringTokenizer( value.snd, "," );
      while( tokenizer.hasMoreTokens() )
      {
        String paramName = tokenizer.nextToken();
        if( paramName.startsWith( "opt$" ) )
        {
          paramName = paramName.substring( "opt$".length() );
        }
        superParams = superParams.append( paramName );
      }
      return superParams;
    }

    private MethodSymbol findSuperMethod_NoParamCheck( JCMethodDecl method )
    {
      if( !couldOverride( method ) )
      {
        return null;
      }

      Symbol superMethod = findSuperMethod( classDecl().sym, method.name );
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

      superMethod = findSuperMethod( classDecl().sym, method.name );
      if( superMethod != null )
      {
        return (MethodSymbol)superMethod;
      }
      return null;
    }

    // search the ancestry for the super method, if the direct super method does not have optional parameters, search for
    // its direct super method, and so on until a super method with optional parameters is found, otherwise return null.
    private Symbol findSuperMethod( Symbol.TypeSymbol owner, Name name )
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
        Symbol result = null;
        int max = -1;
        for( Symbol e : members )
        {
          Pair<MethodSymbol, String> value = getParamAnnoValue( (MethodSymbol)e );
          if( value != null )
          {
            if( result == null || ((MethodSymbol)e).params().size() > max )
            {
              result = e;
              max = ((MethodSymbol)e).params().size();
            }
          }
        }
        return result;
      }
      return null;
    }
  }

  // answers: is method overridable by a subtype?
  private boolean isOverridable( JCMethodDecl method )
  {
    return !method.getName().equals( getNames().init ) && isOverridable( method.sym );
  }
  private boolean isOverridable( MethodSymbol msym )
  {
    return msym != null && !msym.isStatic() && !msym.isPrivate() && !msym.type.isFinal() &&
           !msym.isConstructor() && !msym.owner.type.isFinal() && (msym.owner.flags_field & RECORD) == 0;
  }

  // answers: could targetMethod possibly override a method in a supertype?
  private boolean couldOverride( JCMethodDecl targetMethod )
  {
    if( targetMethod.name.equals( getNames().init ) )
    {
      return false;
    }

    MethodSymbol msym = targetMethod.sym;
    return msym != null && !msym.isStatic() && !msym.isPrivate() && !msym.isConstructor();
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
      if( tree.type == null )
      {
        // classDecl not attributed yet
        return;
      }

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
      if( methodDecl.sym == null )
      {
        // adding BRIDGE to enum constructors makes compiler angry,
        // will have to use the IDE plugin to hide these :/
        return;
      }

      methodDecl.mods.getAnnotations().stream()
        .filter( anno -> {
          String annoTypeName = anno.getAnnotationType().type.tsym.getQualifiedName().toString();
          if( methodDecl.sym.isConstructor() && annoTypeName.equals( params.class.getTypeName() ) )
          {
            String paramNames = (String)anno.attribute.getElementValues().values().stream().findFirst().get().getValue();
            return paramNames.contains( "opt$" );
          }
          return annoTypeName.equals( param_default.class.getTypeName() ) ||
                 annoTypeName.equals( param_overload.class.getTypeName() );
        } )
        .findFirst().ifPresent( anno -> {
            if( methodDecl.sym.isConstructor() )
            {
              if( methodDecl.sym.isEnum() )
              {
                methodDecl.sym.flags_field |= Flags.SYNTHETIC;
              }
            }
            else
            {
              methodDecl.sym.flags_field |= Flags.BRIDGE;
            }
        } );
    }

    @Override
    public void visitApply( JCMethodInvocation tree )
    {
      super.visitApply( tree );
      replaceSuperCallToOverloadWithSuperCallToTarget( tree );
    }

    @Override
    public void visitExec( JCExpressionStatement tree )
    {
      JCTree translate = translate( tree.expr );
      //noinspection ConstantValue
      if( translate instanceof JCBlock )
      {
        result = translate;
      }
      else
      {
        tree.expr = (JCExpression)translate;
        result = tree;
      }
    }

    // Replace super calls to generated overloads with direct calls to source method,
    // otherwise calling the overload with super from the super's override will result in stack overflow
    // because the generated overload forwards to the source method, which dispatches to the override.
    private void replaceSuperCallToOverloadWithSuperCallToTarget( JCMethodInvocation tree )
    {
      MethodSymbol msym = null;

      //
      // Handle just super.foo() MyInterface.super.foo() positional overload calls
      //
      if( tree.meth instanceof JCTree.JCFieldAccess )
      {
        JCExpression selected = ((JCFieldAccess)tree.meth).selected;
        if( selected instanceof JCTree.JCIdent && ((JCIdent)selected).name == getNames()._super ||
            selected instanceof JCTree.JCFieldAccess && ((JCFieldAccess)selected).name == getNames()._super )
        {
          msym = (MethodSymbol)((JCFieldAccess)tree.meth).sym;
        }
      }

      //
      // Handle all positional overload calls
      //
//      if( tree.meth instanceof JCTree.JCFieldAccess )
//      {
//        msym = (MethodSymbol)((JCFieldAccess)tree.meth).sym;
//      }
//      else if( tree.meth instanceof JCTree.JCIdent )
//      {
//        msym = (MethodSymbol)((JCIdent)tree.meth).sym;
//      }

      if( msym == null || msym.isConstructor() )
      {
        return;
      }

      params paramsAnno = msym.getAnnotation( params.class );
      if( paramsAnno == null )
      {
        // method does not have opt params
        return;
      }
      String value = paramsAnno.value();
      if( value.contains( "opt$" ) )
      {
        // method is the source method, not a generated overload
        return;
      }

      Pair<MethodSymbol, String> target = getParamAnnoValue( msym );
      if( target == null )
      {
        // errant condition
        return;
      }

      MethodSymbol targetMethod = target.fst;
      String params = target.snd;

      int optsTotal = ManStringUtil.countMatches( params, "opt$" );
      int total = ManStringUtil.countMatches( params, "," ) + 1;
      int reqs = total - optsTotal;
      int overloadParams = msym.params().size();
      int overloadOptsTotal = overloadParams - reqs;

      LinkedHashMap<String, JCExpression> args = new LinkedHashMap<>();
      int overloadOptsUsed = 0;
      StringTokenizer tokenizer = new StringTokenizer( params, "," );
      for( int i = 0; tokenizer.hasMoreTokens(); i++ )
      {
        String paramName = tokenizer.nextToken();
        if( paramName.startsWith( "opt$" ) )
        {
          paramName = paramName.substring( "opt$".length() );
          if( overloadOptsUsed < overloadOptsTotal )
          {
            args.put( paramName, tree.args.get( i ) );
            overloadOptsUsed++;
          }
          else
          {
            args.put( paramName, null );
            i--;
          }
        }
        else
        {
          args.put( paramName, tree.args.get( i ) );
        }
      }

      TreeMaker make = TreeMaker.instance( getContext() );
      make.pos = tree.pos;
      result = makeLetExprForOptionalParamsCall( targetMethod, tree, args, make );
    }

    int[] tempVarIndex = {0};
    private JCTree makeLetExprForOptionalParamsCall(
      Symbol.MethodSymbol paramsMethod, JCTree.JCMethodInvocation tree, LinkedHashMap<String, JCExpression> args, TreeMaker make )
    {
      ArrayList<JCTree.JCVariableDecl> tempVars = new ArrayList<>();
      JCExpression receiverExpr;
      Symbol enclosingSymbol;
      JCExpression meth = tree.meth;
      enclosingSymbol = ManAttr.getEnclosingSymbol( tree, getNames(), t -> getParent( t ) );

      if( meth instanceof JCTree.JCFieldAccess )
      {
        receiverExpr = ((JCTree.JCFieldAccess)meth).selected;
        JCTree[] receiverTemp = tempify( false, tree, make, receiverExpr, getContext(), enclosingSymbol, "$receiverExprTemp", tempVarIndex[0] );
        if( receiverTemp != null )
        {
          tempVars.add( (JCTree.JCVariableDecl)receiverTemp[0] );
          receiverExpr = (JCExpression)receiverTemp[1];
        }
        ((JCTree.JCFieldAccess)meth).selected = receiverExpr;
      }
      else if( meth instanceof JCTree.JCIdent )
      {
        receiverExpr = null;
      }
      else
      {
        return tree;
      }

      if( tree.meth instanceof JCTree.JCFieldAccess )
      {
        ((JCFieldAccess)tree.meth).sym = paramsMethod;
      }
      else if( tree.meth instanceof JCTree.JCIdent )
      {
        ((JCIdent)tree.meth).sym = paramsMethod;
      }
      JCExpression expr = tree;
      tree.meth.type = paramsMethod.erasure( getTypes() );
      tree.args = addArgVars( tree, paramsMethod, enclosingSymbol, tempVars, receiverExpr, args, make, tempVarIndex[0] );
      List<JCTree.JCStatement> defs = List.from( tempVars );
      if( paramsMethod.getReturnType() == getSymtab().voidType )
      {
        // a void method call just becomes a def where the letexpr's expr is nothing
        defs = defs.append( make.Exec( expr ) );
        return make.Block( 0, defs );
//        expr = make.Literal( TypeTag.BOT, null );
//        expr.type = getSymtab().botType;
      }
      return ILetExpr.makeLetExpr( make, defs, expr, tree.type, tree.pos );
    }

    private List<JCExpression> addArgVars(
      JCTree.JCExpression optMethCall, Symbol.MethodSymbol paramsMethod, Symbol enclosingSymbol,
      ArrayList<JCTree.JCVariableDecl> tempVars, JCExpression receiverExpr, LinkedHashMap<String, JCExpression> args, TreeMaker make, int tempVarIndex )
    {
      List<JCExpression> defValueMethArgs = List.nil();
      int i = -1;
      for( String paramName : args.keySet() )
      {
        i++;
        JCExpression expr = args.get( paramName );
        Type paramType = paramsMethod.params.get( i ).type;
        if( expr == null )
        {
          // set expr to a default value method call
          Symbol.MethodSymbol defValueMethSym = getDefValueMethod( paramsMethod, paramName );
          JCTree.JCMethodInvocation defValueMethCall = make.Apply( optMethCall instanceof JCTree.JCMethodInvocation ? ((JCTree.JCMethodInvocation)optMethCall).typeargs : ((JCTree.JCNewClass)optMethCall).typeargs,
                                                                   receiverExpr != null ? IDynamicJdk.instance().Select( make, receiverExpr, defValueMethSym ) : make.Ident( defValueMethSym ), defValueMethArgs );
          paramType = paramType.isPrimitive() ? paramType : getTypes().erasure( paramType );
          defValueMethCall.type = paramType;
          JCTree[] argTemp = tempify( false, optMethCall, make, defValueMethCall, getContext(), enclosingSymbol, "$" + paramName + "_", tempVarIndex );
          if( argTemp != null )
          {
            tempVars.add( (JCTree.JCVariableDecl)argTemp[0] );
            expr = (JCExpression)argTemp[1];
          }
          else
          {
            throw new IllegalStateException( "Expecting let expr here" );
          }
        }
        else
        {
          // use the expr

          JCTree[] argTemp = tempify( false, optMethCall, make, expr, getContext(), enclosingSymbol, "$" + paramName + "_", tempVarIndex );
          if( argTemp != null )
          {
            tempVars.add( (JCTree.JCVariableDecl)argTemp[0] );
            expr = (JCExpression)argTemp[1];
          }
        }
        defValueMethArgs = defValueMethArgs.append( expr );
      }
      return defValueMethArgs;
    }

    private Symbol.MethodSymbol getDefValueMethod( Symbol.MethodSymbol paramsMethod, String paramName )
    {
      String defaultValueMethodName = defaultValueMethodName( paramsMethod, paramName );
      for( Type t : getTypes().closure( paramsMethod.owner.type ) )
      {
        Iterable<Symbol> membersByName = IDynamicJdk.instance().getMembersByName( (Symbol.ClassSymbol)t.tsym, getNames().fromString( defaultValueMethodName ) );
        for( Symbol m : membersByName )
        {
          return (Symbol.MethodSymbol)m;
        }
      }
      return null;
    }

    private String defaultValueMethodName( Symbol.MethodSymbol targetMethod, String param  )
    {
      Name methName = targetMethod.isConstructor() ? targetMethod.owner.getSimpleName() : targetMethod.name;
      return "$" + methName + "_" + param;
    }

    private MethodSymbol getTargetMethod( MethodSymbol telescopeMethod, Set<MethodSymbol> methods )
    {
      if( methods.isEmpty() )
      {
        ManTypes.getAllMethods( classDecl().type, m -> m != null && m.name.equals( telescopeMethod.name ), methods );
      }
      return findTargetMethod( telescopeMethod, methods );
    }

    private void checkDuplication( JCMethodDecl telescopeMethod )
    {
      Set<MethodSymbol> methods = new LinkedHashSet<>();
      MethodSymbol targetMethod = getTargetMethod( telescopeMethod.sym, methods );
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
      make.pos = expression.pos;

      JCTypeCast castCall = make.TypeCast( type, expression );
      ((JCTree)castCall).type = type;
      castCall.pos = expression.pos;

      return castCall;
    }

    private boolean isNullType( Type t )
    {
      return t != null && t.tsym != null && Null.class.getTypeName().equals( t.tsym.getQualifiedName().toString() );
    }
  }

  private Map<String, JCMethodInvocation> makeSuperDefaultValueMap( MethodSymbol superMethod, TreeMaker make )
  {
    if( superMethod == null )
    {
      return Collections.emptyMap();
    }

    Pair<MethodSymbol, String> value = getParamAnnoValue( superMethod );
    if( value == null )
    {
      // superMethod is not an opt params method, nor does it override one
      return Collections.emptyMap();
    }

    return makeDefaultValueMap( superMethod, make, value.snd );
  }

  private Map<String, JCMethodInvocation> makeDefaultValueMap( MethodSymbol targetMethod, TreeMaker make, String paramsValue )
  {
    TreeCopier<?> copier = new TreeCopier<>( make );

    Map<String, JCMethodInvocation> optParams = new HashMap<>();
    List<JCExpression> optArgs = List.nil();
    for( StringTokenizer tokenizer = new StringTokenizer( paramsValue, "," ); tokenizer.hasMoreTokens(); )
    {
      String paramName = tokenizer.nextToken();
      if( paramName.startsWith( "opt$" ) )
      {
        paramName = paramName.substring( "opt$".length() );
        optParams.put( paramName, make.Apply( List.nil(),
                                                   make.Ident( getNames().fromString( "$" + targetMethod.name + "_" + paramName ) ), copier.copy( optArgs ) ) );
      }
      optArgs = optArgs.append( make.Ident( getNames().fromString( paramName ) ) );
    }
    return optParams;
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

  private Pair<MethodSymbol, String> getParamAnnoValue( MethodSymbol msym )
  {
    Symbol owner = msym.owner;
    Pair<MethodSymbol, String> value = null;
    for( Symbol m: IDynamicJdk.instance().getMembersByName( (ClassSymbol)owner, msym.name, true ) )
    {
      if( m instanceof MethodSymbol )
      {
        params paramsAnno = m.getAnnotation( params.class );
        String result;
        if( paramsAnno != null )
        {
          result = paramsAnno.value();
        }
        else
        {
          result = _paramsByName.get( m.owner.getQualifiedName() + "#$" + m.name );
        }

        if( result != null && result.contains( "opt$" ) )
        {
          value = new Pair<>( (MethodSymbol)m, result );
          break;
        }
      }
    }
    return value;
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

  private JCTree.JCClassDecl getEnclosingClass( Tree tree )
  {
    Tree parent = getParent( tree );
    if( parent == null )
    {
      return null;
    }
    if( parent instanceof JCClassDecl )
    {
      return (JCClassDecl)parent;
    }
    return getEnclosingClass( parent );
  }
}
