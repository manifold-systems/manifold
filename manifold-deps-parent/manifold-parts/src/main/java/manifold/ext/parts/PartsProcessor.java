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

package manifold.ext.parts;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.MethodType;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.jvm.ClassFile;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import manifold.api.type.ICompilerComponent;
import manifold.api.util.JCTreeUtil;
import manifold.api.util.JavacDiagnostic;
import manifold.ext.parts.rt.api.DelegationLinkageError;
import manifold.ext.parts.rt.api.internal;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;
import manifold.ext.parts.rt.internal.$PartClass;
import manifold.ext.parts.rt.internal.Generated;
import manifold.ext.rt.ExtensionMethod;
import manifold.internal.javac.*;
import manifold.rt.api.util.Stack;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

import static com.sun.tools.javac.code.Flags.FINAL;
import static com.sun.tools.javac.code.Flags.SYNTHETIC;
import static com.sun.tools.javac.code.TypeTag.INT;
import static java.lang.reflect.Modifier.*;
import static manifold.ext.parts.PartsIssueMsg.*;
import static manifold.ext.parts.Util.getAnnotation;
import static manifold.util.JreUtil.isJava8;

public class PartsProcessor implements ICompilerComponent, TaskListener
{
  private static final String INTERFACE_CLOSURE_FIELD = "$INTERFACE_CLOSURE";
  private static final String LINKED_INTERFACES_FIELD = "$LINK_SCOPE_";
  private static final String LINK_PART_TO_SELF = "$linkPartToSelf";
  private static final String SELVES = "$selves";

  private BasicJavacTask _javacTask;
  private Context _context;
  private Map<Name, Map<Name, Integer>> _classToInterfaceToIndex;

  //todo: factor out the TaskEventTracker aspect into an abstract "BasicCompileComponent" class and factor out other common aspects
  //todo: extend BasicCompilerComponent in other compiler components, and replace their existing ways of detecting already processed types
  private TaskEventTracker _taskEventTracker;

  private TaskEvent _taskEvent;
  private ParentMap _parents;

  @Override
  public void init( BasicJavacTask javacTask, TypeProcessor typeProcessor )
  {
    _javacTask = javacTask;
    _context = _javacTask.getContext();
    _classToInterfaceToIndex = new HashMap<>();
    _taskEventTracker = new TaskEventTracker();
    _parents = new ParentMap( () -> getCompilationUnit() );

    if( JavacPlugin.instance() == null )
    {
      // does not function at runtime
      return;
    }

    // Ensure TypeProcessor follows this in the listener list e.g., so that delegation integrates with structural
    // typing and extension methods.
    typeProcessor.addTaskListener( this );
  }

  @Override
  public InitOrder initOrder( ICompilerComponent compilerComponent )
  {
    // Properties must be processed before DelegationProcessor so that manifold-props is fully supported with manifold-parts
    return compilerComponent.getClass().getName().equals( "manifold.ext.props.PropertyProcessor" )
      ? InitOrder.After
      : InitOrder.NA;
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

  private List<Type> interfaceClosure( Type type )
  {
    //noinspection ComparatorMethodParameterNotUsed
    return List.from( getTypes().closure( type ).stream()
                        .filter( Type::isInterface )
                        // guarantee subtype ordering (not relying on it from Types.closure)
                        .sorted( (a, b) -> isSubtype( a, b ) ? -1 : 1 )
                        .collect( Collectors.toList() ) );
  }

  private Type erasure( Type type )
  {
    return getTypes().erasure( type );
  }

  private boolean isSameType( Type t, Type s )
  {
    return getTypes().isSameType( t, s );
  }

  private boolean isSubtype( Type t, Type s )
  {
    return getTypes().isSubtype( t, s );
  }

  private Type classType()
  {
    Symtab symtab = getSymtab();
    return getTypes().erasure( symtab.classType );

//    Type.WildcardType wild = new Type.WildcardType( symtab.objectType, BoundKind.UNBOUND, symtab.boundClass );
//    return new Type.ClassType( Type.noType, List.of( wild ), symtab.classType.tsym );
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

  private int indexOfInterface( Type partClass, Type iface )
  {
    Type partType = erasure( partClass );
    return _classToInterfaceToIndex.computeIfAbsent( partType.tsym.getQualifiedName(), __ -> {
      ArrayList<ClassType> result = new ArrayList<>();
      findAllInterfaces( partType, new HashSet<>(), result );
      Map<Name, Integer> map = new HashMap<>();
      for( int i = 0; i < result.size(); i++ )
      {
        ClassType t = result.get( i );
        map.put( erasure( t ).tsym.getQualifiedName(), i );
      }
      return map;
    } ).get( erasure( iface ).tsym.getQualifiedName() );
  }

  @Override
  public void started( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.ENTER &&
        e.getKind() != TaskEvent.Kind.ANALYZE )
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
          if( _taskEventTracker.alreadyProcessed( e, true, classDecl ) )
          {
            continue;
          }
          if( e.getKind() == TaskEvent.Kind.ENTER )
          {
            classDecl.accept( new Enter_Start() );
          }
          else if( e.getKind() == TaskEvent.Kind.ANALYZE )
          {
            classDecl.accept( new Analyze_Start() );
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
    if( e.getKind() != TaskEvent.Kind.ENTER &&
        e.getKind() != TaskEvent.Kind.ANALYZE )
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
          if( _taskEventTracker.alreadyProcessed( e, false, (JCClassDecl)tree ) )
          {
            continue;
          }

          JCClassDecl classDecl = (JCClassDecl)tree;
          if( e.getKind() == TaskEvent.Kind.ENTER )
          {
            classDecl.accept( new Enter_Finish() );
          }
          else if( e.getKind() == TaskEvent.Kind.ANALYZE )
          {
            classDecl.accept( new Analyze_Finish() );
          }
        }
      }
    }
    finally
    {
      _taskEvent = null;
    }
  }

  // Make interface methods corresponding with @link fields
  //
  private class Enter_Finish extends TreeTranslator
  {
    private final Stack<ClassInfo> _classInfoStack = new Stack<>();

    @Override
    public void visitClassDef( JCClassDecl classDecl )
    {
      _classInfoStack.push( new ClassInfo( classDecl ) );
      try
      {
        if( classDecl.sym == null )
        {
          //todo: sym is null for method-local inner classes?
          super.visitClassDef( classDecl );
          return;
        }

        processPartClass( classDecl );

        super.visitClassDef( classDecl );

        if( isPartClass( classDecl.sym ) )
        {
          overrideDefaultInterfaceMethods( classDecl );
        }

        postProcessPartClass();

        ClassInfo classInfo = _classInfoStack.peek();
        if( classInfo.hasLinks() )
        {
          // find and remove overlapping interfaces to force delegating class to implement them, add warnings
          processInterfaceOverlap( classInfo );
          // find and remove overlapping methods to force delegating class to implement them, add warnings
          processMethodOverlap( classInfo );

          for( LinkInfo li : classInfo.getLinks().values() )
          {
            // build interface method defs
            linkInterfaces( li );

            // add interface method defs to class AST
            ArrayList<JCTree> newDefs = new ArrayList<>( classDecl.defs );
            newDefs.addAll( li.getGeneratedMethods() );
            classDecl.defs = List.from( newDefs );

            // define interface method symbols and add them to the class symbol's members
            for( JCMethodDecl methDecl : li.getGeneratedMethods() )
            {
              memberEnter( methDecl, classDecl );
            }
          }
        }
      }
      finally
      {
        _classInfoStack.pop();
      }
    }

    private void postProcessPartClass()
    {
      JCClassDecl classDecl = _classInfoStack.peek()._classDecl;
      if( !isPartClass( classDecl.sym ) )
      {
        return;
      }

      if( classDecl.sym.getSimpleName().contentEquals( "$Impl" ) )
      {
        generate$ImplClass();
      }

      addAsLinkMethods();
      addInterfaceClosureField();
      addLinkScopeFields();
      addLinkPartToSelfMethod();
    }

    private void addAsLinkMethods()
    {
      JCClassDecl classDecl = _classInfoStack.peek()._classDecl;
      if( (classDecl.mods.flags & ABSTRACT) == 0 )
      {
        return;
      }

      for( JCTree def : classDecl.defs )
      {
        if( def instanceof JCMethodDecl && ((JCMethodDecl)def).sym.isConstructor() && (((JCMethodDecl)def).sym.flags_field & SYNTHETIC) == 0 )
        {
          JCMethodDecl methDecl = generateAsLinkStaticMethod( (JCMethodDecl)def );
          classDecl.defs = classDecl.defs.append( methDecl );
          memberEnter( methDecl, classDecl );
        }
      }
    }

    /**
     * Generates a `$linkPartToSelf()` method on part classes (class annotated with '@part'). This method is responsible
     * for wiring the composite object's "self" identities to linked part classes throughout the composite. The wiring happens
     * at `@link` field assignments where a call to `$PartClass.Internal#linkPart()` is generated and subsumes the RHS of
     * the assignment. The `linkPart()` method tests for error conditions and, if the RHS is a part class instance, forwards
     * the delegating class instance to the part's `$linkPartToSelf()` as its "self" identity, then returns the original RHS.
     * In addition to assigning delegating class identity to the part class `$linkPartToSelf()` also propagates the identity
     * by recursing into the part's own `@link` fields.
     * <p/>
     * Following the documentation's Teacher Assistant example:
     * <pre><code>
     *
     * // StudentPart is a `@part` class
     *
     * {@literal @}link Student student = new StudentPart(person);
     *
     * // RHS of assignment is rewrittent as:
     *
     * {@literal @}link Student student = $PartClass.Internal.linkPart(
     *   this, new Class[] {Student.class, Person.class}, "student", new StudentPart(person));
     *
     * // linkPart() calls $linkPartToSelf(), where the delegating class `this` is
     * // propagated as the composite identity in terms of Student and Person:
     *
     * (($PartClass)delegate).$linkPartToSelf(this, new Class[] {Student.class, Person.class});
     *
     * </code></pre>
     * <pre><code>
     *  public void $linkPartToSelf(Object root, Class<?>[] linkScope) {
     *    if (this == root) {
     *      Internal.reportCycle(this, root, iface);
     *    }
     *
     *    outer:
     *    for (Class<?> linkIface : linkScope) {
     *      for (int i = 0; i < $INTERFACE_CLOSURE.length; ++i) {
     *        Class<?> iface = $INTERFACE_CLOSURE[i];
     *        if (iface == linkIface) {
     *          this.$selves[i] = root;
     *          continue outer;
     *        }
     *      }
     *      throw new DelegationLinkageError("Unimplemented linked interface: " + linkIface);
     *    }
     *
     *    // recurse through the fields of this part that are linked to `@part` classes
     *    Class[] scopeLinkField1 = Internal.widest($LINK_FIELD_1, linkScope);
     *    if (scopeLinkField1.length != 0 && this.linkField1 instanceof $PartClass) {
     *      (($PartClass)this.linkField1).$linkPartToSelf(root, scopeLinkField1);
     *    }
     *    // likewise for the linkField2, etc.
     *  }
     * </code></pre>
     */
    private void addLinkPartToSelfMethod()
    {
      ClassInfo ci = _classInfoStack.peek();
      JCClassDecl classDecl = ci._classDecl;

      TreeMaker make = getTreeMaker();
      make.pos = classDecl.pos;

      // Method name & modifiers
      JCModifiers access = make.Modifiers( PUBLIC /*| Flags.BRIDGE*/, List.of( suppressRawTypesWarnings( make ) ) );
      Names names = getNames();
      Name methName = names.fromString( LINK_PART_TO_SELF );

      // Params
      List<JCVariableDecl> params = List.nil();

      Name rootName = names.fromString( "root" );
      Symtab symtab = getSymtab();
      JCExpression rootType = make.Type( symtab.objectType );
      JCVariableDecl rootParam = make.VarDef( make.Modifiers( FINAL | Flags.PARAMETER ), rootName, rootType, null );
      params = params.append( rootParam );

      Name linkScopeName = names.fromString( "linkScope" );

      JCExpression linkScopeType = make.Type( getTypes().makeArrayType( classType() ) );
      JCVariableDecl linkScopeParam = make.VarDef( make.Modifiers( FINAL | Flags.PARAMETER ), linkScopeName, linkScopeType, null );
      params = params.append( linkScopeParam );

      // Return type
      JCExpression resType = make.Type( symtab.voidType );

      // Code
      JCIf cycleCheck = make.If(
        make.Binary( Tag.EQ, make.This( classDecl.sym.type ), make.Ident( rootName ) ),
        make.Exec( make.Apply( List.nil(), make.Select( make.Ident( names.fromString( "Internal" ) ), names.fromString( "reportCycle" ) ),
                               List.of( make.This( classDecl.sym.type ), make.Ident( rootName ) ) ) ),
        null );
      List<JCStatement> outerLoopStmts = List.nil();
      Name linkIfaceName = names.fromString( "linkIface" );
      Name i = names.fromString( "i" );
      JCVariableDecl indexVar = make.VarDef( make.Modifiers( 0 ), i, make.Type( symtab.intType ), make.Literal( INT, 0 ) );
      JCBinary cond = make.Binary( Tag.LT,
                                     make.Ident( i ),
                                     make.Select( make.Ident( names.fromString( INTERFACE_CLOSURE_FIELD ) ), names.fromString( "length" ) ) );
      JCExpressionStatement step = make.Exec( make.Unary( Tag.POSTINC, make.Ident( i ) ) );

      Name ifaceName = names.fromString( "iface" );
      JCVariableDecl ifaceVar = make.VarDef( make.Modifiers( FINAL ), ifaceName,
                                        make.Type( classType() ),
                                        make.Indexed( make.Ident( names.fromString( INTERFACE_CLOSURE_FIELD ) ), make.Ident( i ) ) );
      JCIf ifStmt = make.If(
        make.Binary( Tag.EQ, make.Ident( ifaceName ), make.Ident( linkIfaceName ) ),
        make.Block( 0, List.of( make.Exec( make.Assign( make.Indexed( make.Ident( names.fromString( SELVES ) ), make.Ident( i ) ), make.Ident( rootName ) ) ),
                                make.Continue( names.fromString( "outer" ) ) ) ),
        null );
      JCForLoop innerLoop = make.ForLoop( List.of( indexVar ), cond, List.of( step ), make.Block( 0, List.of( ifaceVar, ifStmt ) ) );
      outerLoopStmts = outerLoopStmts.append( innerLoop );

      JCThrow throwDelegationLinkageError = make.Throw(
        make.NewClass( null, null, memberAccess( make, DelegationLinkageError.class.getTypeName() ),
                       List.of( make.Binary( Tag.PLUS, make.Literal( "Unimplemented linked interface: " ), make.Ident( linkIfaceName ) ) ), null ) );
      outerLoopStmts = outerLoopStmts.append( throwDelegationLinkageError );
      //////
      JCEnhancedForLoop assignSelves = make.ForeachLoop( make.VarDef( make.Modifiers( FINAL ), linkIfaceName, make.Type( classType() ), null ),
                                                         make.Ident( linkScopeName ), make.Block( 0, outerLoopStmts ) );
      JCLabeledStatement outerLoop = make.Labelled( names.fromString( "outer" ), assignSelves );
      List<JCStatement> methodBody = List.of( cycleCheck, outerLoop );
      for( Map.Entry<JCVariableDecl, LinkInfo> link : ci.getLinks().entrySet() )
      {
        JCVariableDecl field = link.getKey();
        Name fieldName = field.name;

        JCMethodInvocation widestOfRootOrLink = make.Apply( List.nil(), make.Select( make.Ident( names.fromString( "Internal" ) ), names.fromString( "widest" ) ),
                                                          List.of( make.Ident( names.fromString( LINKED_INTERFACES_FIELD + fieldName ) ),
                                                                   make.Ident( linkScopeName ) ) );
        Name widestOfRootOrLink_Name = names.fromString( "widestOf_root_or_" + fieldName );
        methodBody = methodBody.append(
          make.VarDef( make.Modifiers( FINAL ), widestOfRootOrLink_Name, make.Type( getTypes().makeArrayType( classType() ) ), widestOfRootOrLink ) );
        methodBody = methodBody.append(
          make.If( make.Binary( Tag.AND,
                                make.Binary( Tag.NE,
                                             make.Select( make.Ident( widestOfRootOrLink_Name ), names.fromString( "length" ) ),
                                             make.Literal( TypeTag.INT, 0 ) ),
                                make.TypeTest( make.Ident( fieldName ), memberAccess( make, $PartClass.class.getTypeName() ) ) ),
                   make.Exec( make.Apply( List.nil(), make.Select( make.TypeCast( memberAccess( make, $PartClass.class.getTypeName() ), make.Ident( fieldName ) ), methName ),
                                          List.of( make.Ident( rootName ), make.Ident( widestOfRootOrLink_Name ) ) ) ),
                   null ) );
      }
      Type superclass = classDecl.sym.getSuperclass();
      if( superclass != null && !isSameType( superclass, symtab.objectType ) )
      {
        methodBody = methodBody.append( make.Exec( make.Apply( List.nil(), make.Select( make.Ident( names._super ), methName ),
                                                               List.of( make.Ident( rootName ), make.Ident( linkScopeName ) ) ) ) );
      }
      JCBlock block = make.Block( 0, methodBody );
      JCMethodDecl methDecl = make.MethodDef( access, methName, resType, List.nil(), params, List.nil(), block, null );

      classDecl.defs = classDecl.defs.append( methDecl );

      memberEnter( methDecl, classDecl );
    }

    private void addLinkScopeFields()
    {
      ClassInfo ci = _classInfoStack.peek();
      for( Map.Entry<JCVariableDecl, LinkInfo> link: ci.getLinks().entrySet() )
      {
        TreeMaker make = getTreeMaker();
        make.pos = ci._classDecl.pos;

        JCVariableDecl field = link.getKey();
        LinkInfo li = link.getValue();

        Type.ArrayType arrayOfClassesType = getTypes().makeArrayType( classType() );
        List<Type> interfaces = li.getInterfaces();
        List<JCExpression> interfaceTypes = List.from( interfaces.stream().map( t -> make.ClassLiteral( getTypes().erasure( t ) ) ).collect( Collectors.toList() ) );
        JCNewArray interfaceArray = make.NewArray( make.Type( classType() ), List.nil(), interfaceTypes );
        interfaceArray.type = arrayOfClassesType;

        addWiringField( ci._classDecl, STATIC | FINAL, LINKED_INTERFACES_FIELD + field.name, interfaceArray.type, interfaceArray );
      }
    }

    private void addInterfaceClosureField()
    {
      ClassInfo ci = _classInfoStack.peek();

      TreeMaker make = getTreeMaker();
      make.pos = ci._classDecl.pos;

      Type.ArrayType arrayOfClassesType = getTypes().makeArrayType( classType() );
      ArrayList<ClassType> interfaces = ci.getInterfaces();
      List<JCExpression> interfaceTypes = List.from( interfaces.stream().map( t -> make.ClassLiteral( getTypes().erasure( t ) ) ).collect( Collectors.toList() ) );
      JCNewArray interfaceArray = make.NewArray( make.Type( classType() ), List.nil(), interfaceTypes );
      interfaceArray.type = arrayOfClassesType;

      addWiringField( ci._classDecl, STATIC | FINAL, INTERFACE_CLOSURE_FIELD, interfaceArray.type, interfaceArray );
    }

    /**
     * Generate methods to override default interface methods. A generated override must forward the call to the default
     * method as if forwarded within $self. Since a default method implementation can call other methods in the interface,
     * and those interface methods can be overridden by linking classes, the default method must be called with @self as
     * the implementing class -- ALL method calls must be dispatched from $self. This can only be done reflectively via method
     * handles.
     */
    private void overrideDefaultInterfaceMethods( JCClassDecl classDecl )
    {
      // foreach default interface method,
      //   override the method
      //     invokedynammic $self Iface.super.method() // call default method from $self
      java.util.List<Type> implIfaces = classDecl.implementing.stream().map( e -> e.type ).collect( Collectors.toList() );
      sortInterfaces( implIfaces );
      for( Type implIface: implIfaces )
      {
        ArrayList<ClassType> superIfaces = new ArrayList<>();
        findAllInterfaces( classDecl.sym.type, new HashSet<>(), superIfaces );
        sortInterfaces( superIfaces, false );

        for( ClassType superIface : superIfaces )
        {
          if( superIface.isErroneous() || !superIface.isInterface() )
          {
            continue;
          }
          ArrayList<MethodSymbol> defaultMethods = new ArrayList<>();
          findDefaultMethodsToForward( classDecl, superIface, new HashSet<>(), defaultMethods );
          Set<NamedMethodType> seen = new HashSet<>();
          for( MethodSymbol m : defaultMethods )
          {
            if( isDelegated( m ) )
            {
              continue;
            }
            if( isExtensionMethod( m ) )
            {
              continue;
            }

            Type type = getTypes().memberType( superIface, m );
            if( type instanceof MethodType )
            {
              NamedMethodType namedMt = new NamedMethodType( m, type );
              if( !seen.contains( namedMt ) )
              {
                ClassInfo ci = _classInfoStack.peek();
                ci.addDefaultMethodForwarder( namedMt );
                generateDefaultMethodForwarder( classDecl, (ClassType)implIface, superIface, namedMt );
              }
              seen.add( namedMt );
            }
          }
        }
      }
    }

    private boolean isDelegated( MethodSymbol m )
    {
      Type owner = erasure( m.owner.type );
      for( LinkInfo li : _classInfoStack.peek().getLinks().values() )
      {
        if( isSubtype( erasure( li.getInterface() ), owner ) )
        {
          // m is part of a delegated interface, it must be forwarded to the delegate,
          // which is the default generated behavior for a composite
          return true;
        }
      }
      return false;
    }


    private void findDefaultMethodsToForward( JCClassDecl classDecl, Type iface, Set<Type> seen, ArrayList<MethodSymbol> result )
    {
      if( seen.stream().anyMatch( t -> isSameType( t, iface ) ) )
      {
        return;
      }
      seen.add( iface );

      Symbol classSym = iface.tsym;
      if( !(classSym instanceof ClassSymbol) )
      {
        return;
      }
      Iterable<Symbol> defaultMethods = IDynamicJdk.instance().getMembers( (ClassSymbol)classSym,
        m -> m instanceof MethodSymbol && ((MethodSymbol)m).isDefault() && (m.flags() & SYNTHETIC) == 0 );
      defaultMethods.forEach( m -> {
        MethodSymbol implSym = ((MethodSymbol)m).implementation( classDecl.sym, getTypes(), false );
        if( implSym == null )
        {
          result.add( (MethodSymbol)m );
        }
      } );
      List<Type> interfaces = ((ClassSymbol)iface.tsym).getInterfaces();
      interfaces = sortInterfaces( interfaces );
      interfaces.forEach( t -> findDefaultMethodsToForward( classDecl, t, seen, result ) );
    }

    private void generateDefaultMethodForwarder( JCClassDecl classDecl, ClassType implIface, ClassType delegatedIface, NamedMethodType namedMt )
    {
      Type csr = namedMt.getType();
      while( csr instanceof Type.DelegatedType )
      {
        csr = ((Type.DelegatedType)csr).qtype;
      }
      MethodType mt = (MethodType)csr;

      TreeMaker make = getTreeMaker();
      make.pos = classDecl.pos;

      // Method name & modifiers
      JCModifiers access = make.Modifiers( PUBLIC );
      Names names = getNames();
      Name name = namedMt.getName();

      // Throws
      List<JCExpression> thrown = make.Types( mt.getThrownTypes() );

      // Type params
      List<JCTypeParameter> typeParams;
      if( namedMt.getType() instanceof Type.ForAll )
      {
        typeParams = make.TypeParams( namedMt.getType().getTypeArguments() );
      }
      else
      {
        List<Type> typeParamTypes = List.from( namedMt.getMethodSymbol().getTypeParameters().stream().map( tp -> tp.type )
          .collect( Collectors.toList() ) );
        typeParams = make.TypeParams( typeParamTypes );
      }

      // Params
      List<Type> parameterTypes = mt.getParameterTypes();
      ArrayList<JCVariableDecl> params = new ArrayList<>();
      for( int i = 0; i < parameterTypes.size(); i++ )
      {
        Type pt = parameterTypes.get( i );
        Name paramName = names.fromString( "$param" + i );
        JCExpression paramType = make.Type( pt );
        JCVariableDecl param = make.VarDef( make.Modifiers( FINAL | Flags.PARAMETER ), paramName, paramType, null );
        params.add( param );
      }

      // Return type
      JCExpression resType = make.Type( mt.getReturnType() );

      // NOTE!! only the direct Iface.super.method() is generated here, the callDefaultMethodWithInvokeDynamic() method
      //        during ANALYZE will rewrite the direct call to the following:
      //
      //     Object $self = $selves[<index of iface>]
      //     if( $self != this )
      //       invokedynamic $self Iface.super.method() // call default method from $self
      //     else
      //       Iface.super.method()  // call as-is


      Types types = getTypes();

      // Iface.super.method()

      JCTree.JCFieldAccess forwardRef = IDynamicJdk.instance().Select(
        make, make.Select( make.Type( implIface ), names._super ), namedMt.getMethodSymbol() );
      forwardRef.type = mt.getReturnType();
      java.util.List<JCExpression> args = params.stream().map( p -> make.Ident( p.name ) ).collect( Collectors.toList() );
      JCTree.JCMethodInvocation forwardCall = make.Apply( List.nil(), forwardRef, List.from( args ) );
      forwardCall.type = forwardRef.type;
      ((JCTree.JCFieldAccess)forwardCall.meth).sym = namedMt.getMethodSymbol();

      JCStatement invokeSuperStmt;
      if( types.isSameType( mt.getReturnType(), getSymtab().voidType ) )
      {
        invokeSuperStmt = make.Exec( forwardCall );
      }
      else
      {
        invokeSuperStmt = make.Return( forwardCall );
      }

      JCBlock block = make.Block( 0, List.of( invokeSuperStmt ) );

      JCMethodDecl defaultMethodForwarder = make.MethodDef( access, name, resType, typeParams, List.from( params ), thrown, block, null );

      ArrayList<JCTree> newDefs = new ArrayList<>( classDecl.defs );
      newDefs.add( defaultMethodForwarder );
      classDecl.defs = List.from( newDefs );

      memberEnter( defaultMethodForwarder, classDecl );
    }

    private void processPartClass( JCClassDecl classDecl )
    {
      checkSuperclass( classDecl );

      if( !isPartClass( classDecl.sym ) )
      {
        return;
      }

      addSelvesField();
    }

    // enforce:
    // part subclass requires part superclass
    // part superclass requires part subclass
    private void checkSuperclass( JCClassDecl classDecl )
    {
      if( classDecl.getExtendsClause() == null )
      {
        return;
      }

      Type superclass = classDecl.sym.getSuperclass();
      if( superclass == null || isSameType( superclass, getSymtab().objectType ) )
      {
        return;
      }

      Attribute.Compound partMirror = superclass.tsym == null ? null : getAnnotationMirror( superclass.tsym, part.class );
      if( isPartClass( classDecl.sym ) )
      {
        if( partMirror == null )
        {
          // part subclass must derive from part
          reportError( classDecl.getExtendsClause(), MSG_SUPERCLASS_NOT_PART.get() );
        }
      }
      else
      {
        if( partMirror != null )
        {
          // non-part subclass cannot derive from part
          reportError( classDecl.getExtendsClause(), MSG_SUPERCLASS_PART.get() );
        }
      }
    }

    private void addSelvesField()
    {
      ClassInfo ci = _classInfoStack.peek();
      JCClassDecl classDecl = ci._classDecl;

      TreeMaker make = getTreeMaker();
      make.pos = classDecl.pos;

      // initialize with `this` to avoid having to do so at runtime, $linkPartToSelf overwrites these values as needed
      ArrayList<ClassType> interfaces = ci.getInterfaces();
      List<JCExpression> thisExprs = List.fill( interfaces.size(), make.QualThis( classDecl.sym.type ) );
      JCNewArray interfaceArray = make.NewArray( make.Type( getSymtab().objectType ), List.nil(), thisExprs );
      interfaceArray.type = getTypes().makeArrayType( getSymtab().objectType );

      addWiringField( classDecl, FINAL, SELVES, interfaceArray.type, interfaceArray );
    }

    private void addWiringField( JCClassDecl classDecl, long mods, String fieldName, Type fieldType, JCExpression initExpr )
    {
      TreeMaker make = getTreeMaker();
      make.pos = classDecl.pos;

      // field name & modifiers & type
      JCModifiers access = make.Modifiers( PRIVATE | mods, List.of( suppressRawTypesWarnings( make ) ) );

      Names names = getNames();
      Name name = names.fromString( fieldName );
      JCExpression type = make.Type( fieldType );

      // the field
      JCVariableDecl fieldDecl = make.VarDef( access, name, type, initExpr );

      // add to AST, add at the *head* of the list to guarantee initialization ahead of user field init
      classDecl.defs = classDecl.defs.prepend( fieldDecl );

      // enter symbol as class member
      memberEnter( fieldDecl, classDecl );
    }

    private void processMethodOverlap( ClassInfo classInfo )
    {
      for( Map.Entry<JCVariableDecl, LinkInfo> entry : classInfo.getLinks().entrySet() )
      {
        LinkInfo li = entry.getValue();
        for( Type iface : li.getInterfaces() )
        {
          if( li._provided.stream().anyMatch( t -> isSameType( t, iface ) ) )
          {
            // overlapping interface is forwarded through a different link
            continue;
          }

          Iterable<Symbol> methods = IDynamicJdk.instance().getMembers( (ClassSymbol)iface.tsym,
            m -> m instanceof MethodSymbol && !m.isStatic() && !m.isPrivate() && (m.flags() & SYNTHETIC) == 0 );
          for( Symbol m: methods )
          {
            processMethods( classInfo._classDecl, li, (MethodSymbol)m );
          }
        }
      }

      // Map method types to links, so we can find overlapping methods
      Map<NamedMethodType, Set<LinkInfo>> mtToDi = new HashMap<>();
      for( Map.Entry<JCVariableDecl, LinkInfo> entry : classInfo.getLinks().entrySet() )
      {
        LinkInfo li = entry.getValue();
        li.getMethodTypes().values()
          .forEach( mtSet -> mtSet
            .forEach( mt -> mtToDi.computeIfAbsent( mt, k -> new HashSet<>() ).add( li ) ) );
      }

      for( Map.Entry<NamedMethodType, Set<LinkInfo>> entry: mtToDi.entrySet() )
      {
        NamedMethodType mt = entry.getKey();
        Set<LinkInfo> lis = entry.getValue();
        if( lis.size() > 1 )
        {
          StringBuilder fieldNames = new StringBuilder();
          lis.forEach( li -> fieldNames.append( fieldNames.length() > 0 ? ", " : "" ).append( li._linkField.name ) );
          for( LinkInfo li : lis )
          {
            reportWarning( li.getLinkField(),
                           PartsIssueMsg.MSG_METHOD_OVERLAP.get( mt.getName(), fieldNames ) );

            // remove the overlap method type from the link, the delegating class must implement it directly
            li.getMethodTypes().get( mt.getName() ).remove( mt );
          }
        }
      }
    }

    private void processMethods( JCClassDecl classDecl, LinkInfo li, MethodSymbol m )
    {
      MethodSymbol existingMethod = m.implementation( classDecl.sym, getTypes(), false );
      if( existingMethod != null &&
        !isSameType( getSymtab().objectType, existingMethod.owner.type ) )
      {
        // class already implements method
        return;
      }

      if( isExtensionMethod( m ) )
      {
        return;
      }

      LinkInfo linkInfo = _classInfoStack.peek().getLinks().get( li._linkField );

      // Method type as a member of the delegating class
      Type emt = getTypes().memberType( classDecl.sym.type, m );
      if( linkInfo.hasMethodType( m.name, emt ) )
      {
        // already defined previously in this link
        return;
      }
      linkInfo.addMethodType( m, emt );
    }

    private boolean isExtensionMethod( Symbol sym )
    {
      if( sym instanceof Symbol.MethodSymbol )
      {
        for( Attribute.Compound annotation : sym.getAnnotationMirrors() )
        {
          if( annotation.type.toString().equals( ExtensionMethod.class.getName() ) )
          {
            return true;
          }
        }
      }
      return false;
    }

    private void processInterfaceOverlap( ClassInfo ci )
    {
      Map<ClassType, Set<LinkInfo>> interfaceToLinks = new HashMap<>();

      for( Map.Entry<JCVariableDecl, LinkInfo> entry: ci.getLinks().entrySet() )
      {
        LinkInfo li = entry.getValue();
        for( ClassType iface : ci.getInterfaces() )
        {
          if( li.getInterfaces().stream().anyMatch( e -> isSameType( e, iface ) ) )
          {
            Set<LinkInfo> lis = interfaceToLinks.computeIfAbsent( iface, k -> new HashSet<>() );
            lis.add( li );
          }
        }
      }

      for( Map.Entry<ClassType, Set<LinkInfo>> entry: interfaceToLinks.entrySet() )
      {
        ClassType iface = entry.getKey();
        Set<LinkInfo> lis = entry.getValue();
        if( lis.size() > 1 )
        {
          boolean isInterfaceShared = checkSharedLinks( iface, lis );

          if( autoResolveShare( lis, iface ) )
          {
            continue;
          }

          for( LinkInfo li: lis )
          {
            if( !li.shares( iface ) )
            {
              if( !isInterfaceShared )
              {
                StringBuilder overlappingLinks = new StringBuilder();
                lis.stream()
                  .filter( l -> l != li )
                  .forEach( l -> overlappingLinks.append( overlappingLinks.length() > 0 ? ", " : "" ).append( l._linkField.name ) );

                reportError( li.getLinkField(),
                             PartsIssueMsg.MSG_INTERFACE_OVERLAP.get( iface.tsym.getSimpleName(), overlappingLinks ) );
              }

              // record that the interface is provided: the link will not forward it (the sharing link provides it exclusively)
              li.provided( iface );
            }
          }
        }
      }
    }

    // if `iface` is directly delegated by a link in the set of `overlappingLinks` and all other links in the set delegate a sub-type of `iface`,
    // auto resolve the share to that link
    private boolean autoResolveShare( Set<LinkInfo> overlappingLinks, ClassType iface )
    {
      for( LinkInfo li: overlappingLinks )
      {
        if( !li.sharesTransitive( iface ) )
        {
          Type rawIface = erasure( iface );
          if( li.getInterfaces().stream().anyMatch( t -> isSameType( t, rawIface ) ) &&
              li.getInterfaces().stream().noneMatch( t -> !isSameType( t, rawIface ) && isSubtype( t, rawIface ) ) &&
              overlappingLinks.stream()
                .filter( l -> l != li )
                .allMatch( l -> !l.shares( iface ) && l.getInterfaces().stream()
                  .anyMatch( t -> isSubtype( t, rawIface ) ) ) )
          {
            // automatically resolve the diamond, designating the link that directly delegates the superinterface
            li._shared.add( iface );
            overlappingLinks.remove( li );
            overlappingLinks.forEach( l -> l.provided( iface ) );

            StringBuilder otherLinks = new StringBuilder();
            overlappingLinks.forEach( l -> otherLinks.append( otherLinks.length() > 0 ? ", " : "" ).append( l._linkField.name ) );

            reportWarning( li.getLinkField(),
                           PartsIssueMsg.MSG_INTERFACE_OVERLAP_AUTO_RESOLVE.get(
                           iface.tsym.getSimpleName(), otherLinks, li.getLinkField().getName() ) );
            return true;
          }
        }
      }
      return false;
    }

    private boolean checkSharedLinks( ClassType iface, Set<LinkInfo> lis )
    {
      ArrayList<LinkInfo> sharedLinks = lis.stream()
        .filter( li -> li.shares( iface ) )
        .collect( Collectors.toCollection( () -> new ArrayList<>() ) );
      if( sharedLinks.size() > 1 )
      {
        StringBuilder fieldNames = new StringBuilder();
        sharedLinks.forEach( li -> fieldNames.append( fieldNames.length() > 0 ? ", " : "" ).append( li._linkField.name ) );

        sharedLinks.forEach( li -> reportError( li.getLinkField(),
                                                PartsIssueMsg.MSG_MULTIPLE_SHARING.get( iface.tsym.getSimpleName(), fieldNames ) ) );
      }
      return !sharedLinks.isEmpty();
    }

    @Override
    public void visitVarDef( JCVariableDecl tree )
    {
      super.visitVarDef( tree );

      if( _classInfoStack.peek()._classDecl.sym == null )
      {
        //todo: sym is null for method-local inner classes?
        return;
      }

      processLinkField( tree );
    }

    private void generate$ImplClass()
    {
      JCClassDecl classDecl = _classInfoStack.peek()._classDecl;
      JCClassDecl classDeclOwner = _classInfoStack.peek( 1 )._classDecl;

      if( !isPartClass( classDecl.sym ) )
      {
        return;
      }

      TreeMaker make = getTreeMaker();
      make.pos = classDecl.pos;

      // generate stub overrides for each abstract method
      List<JCTree> implDefs = List.nil();
      ArrayList<MethodSymbol> unimplementedMethods = findUnimplementedInterfaceMethods( classDeclOwner.sym );
      for( MethodSymbol m: unimplementedMethods )
      {
        Type type = getTypes().memberType( classDecl.sym.type, m );
        MethodType mt = (MethodType)type;
        implDefs = implDefs.append( generateDeferredStub( m, mt ) );
      }

      // generate forwarding constructors mirroring super's constructors
      java.util.List<JCMethodDecl> ctors = classDeclOwner.defs.stream()
        .filter( def -> def instanceof JCMethodDecl && ((JCMethodDecl)def).sym.isConstructor() && (((JCMethodDecl)def).sym.flags_field & SYNTHETIC) == 0 )
        .map( def -> (JCMethodDecl)def )
        .collect( Collectors.toList() );

      removeUnwantedNoArgCtor( classDecl );

      for( JCMethodDecl def : ctors )
      {
        JCMethodDecl methDecl = generateForwardingConstructor( (JCMethodDecl)def );
        implDefs = implDefs.append( methDecl );
      }

      classDecl.defs = classDecl.defs.appendList( implDefs );
      implDefs.forEach( d -> memberEnter( d, classDecl ) );
    }

    // remove free no-arg ctor we don't want (we add it below if needed)
    private void removeUnwantedNoArgCtor( JCClassDecl classDecl )
    {
      for( JCTree def: classDecl.defs )
      {
        if( def instanceof JCMethodDecl && ((JCMethodDecl)def).sym.isConstructor() )
        {
          if( JreUtil.isJava8() )
          {
            classDecl.sym.members().remove( ((JCMethodDecl)def).sym );
          }
          else
          {
            Object scope = ReflectUtil.method( classDecl.sym, "members" ).invoke();
            ReflectUtil.method( scope, "remove", Symbol.class ).invoke( ((JCMethodDecl)def).sym );
          }
        }
      }
      classDecl.defs = List.from( classDecl.defs.stream()
                                    .filter( def -> !(def instanceof JCMethodDecl) || !((JCMethodDecl)def).sym.isConstructor() )
                                    .collect( Collectors.toList() ) );
    }

    private ArrayList<MethodSymbol> findUnimplementedInterfaceMethods( ClassSymbol classSym )
    {
      Set<String> visited = new HashSet<>();
      ArrayList<MethodSymbol> unimplemented = new ArrayList<>();

      ArrayList<ClassType> interfaces = new ArrayList<>();
      findAllInterfaces( classSym.type, new HashSet<>(), interfaces );
      for( ClassType iface : interfaces )
      {
        Iterable<Symbol> members = IDynamicJdk.instance().getMembers(
          (ClassSymbol)iface.tsym,
          m -> m instanceof MethodSymbol &&
               !m.isStatic() &&
               (m.flags() & SYNTHETIC) == 0 &&
               !((MethodSymbol)m).isDefault() );
        for( Symbol member : members )
        {
          MethodSymbol m = (MethodSymbol)member;
          MethodSymbol impl = m.implementation( classSym, getTypes(), false );
          if( impl == null || (impl.flags() & ABSTRACT) != 0 )
          {
            String sig = m.name + erasure( m.type ).toString();
            if( visited.add( sig ) )
            {
              unimplemented.add( m );
            }
          }
        }
      }
      return unimplemented;
    }

    private JCMethodDecl generateDeferredStub( MethodSymbol m, MethodType mt )
    {
      TreeMaker make = getTreeMaker();
      make.pos = _classInfoStack.peek()._classDecl.pos;
      Names names = getNames();

      List<JCTypeParameter> typeParams;
      if( m.type instanceof Type.ForAll )
      {
        typeParams = make.TypeParams( m.type.getTypeArguments() );
      }
      else
      {
        List<Type> typeParamTypes = List.from( m.getTypeParameters().stream()
          .map( tp -> tp.type ).collect( Collectors.toList() ) );
        typeParams = make.TypeParams( typeParamTypes );
      }

      ArrayList<JCVariableDecl> params = new ArrayList<>();
      for( int i = 0; i < mt.getParameterTypes().size(); i++ )
      {
        params.add( make.VarDef(
          make.Modifiers( FINAL | Flags.PARAMETER ),
          names.fromString( "$p" + i ),
          make.Type( mt.getParameterTypes().get( i ) ),
          null ) );
      }

      JCExpression errorClass = JCTreeUtil.memberAccess( make, names, DelegationLinkageError.class.getTypeName() );
      JCNewClass newError = make.NewClass( null, List.nil(), errorClass,
      List.of( make.Literal( "Abstract method '" + m.name + mt + "' called on unlinked part or on unimplemented method of late-bound part." ) ), null );

      return make.MethodDef(
        make.Modifiers( PUBLIC ),
        m.name,
        make.Type( mt.getReturnType() ),
        typeParams,
        List.from( params ),
        make.Types( mt.getThrownTypes() ),
        make.Block( 0, List.of( make.Throw( newError ) ) ),
        null );
    }

    private JCMethodDecl generateAsLinkStaticMethod( JCMethodDecl meth )
    {
      MethodSymbol m = meth.sym;
      MethodType mt = (MethodType)m.type;

      ClassInfo classInfo = _classInfoStack.peek();
      JCClassDecl classDecl = classInfo._classDecl;

      TreeMaker make = getTreeMaker();
      make.pos = classDecl.pos;
      Names names = getNames();

      List<JCTypeParameter> typeParams = List.nil();
      List<JCTypeParameter> classTypeParams = List.nil();
      TreeCopier copier = new TreeCopier( make );
      for( JCTypeParameter tp: classDecl.getTypeParameters() )
      {
        JCTypeParameter copy = (JCTypeParameter)copier.copy( tp );
        classTypeParams = classTypeParams.append( copy );
        typeParams = typeParams.append( copy );
      }
      for( JCTypeParameter tp: meth.getTypeParameters() )
      {
        typeParams = typeParams.append( (JCTypeParameter)copier.copy( tp ) );
      }

      List<JCVariableDecl> params = copier.copy( meth.getParameters() );

      List<JCExpression> typeArgs = List.nil();
      for( JCTypeParameter tp: classDecl.getTypeParameters() )
      {
        typeArgs = typeArgs.append( make.Ident( tp.name ) );
      }

      List<JCExpression> args = List.nil();
      for( JCVariableDecl param: meth.params )
      {
        args = args.append( make.Ident( param.name ) );
      }
      JCNewClass implClassCtor = make.NewClass( null, typeArgs, make.Ident( names.fromString( "$Impl" ) ), args, null );


      JCExpression returnType = null;
      if( !classTypeParams.isEmpty() )
      {
        List<JCExpression> tps = List.from( classTypeParams.stream().map( tp -> make.Ident( tp.name ) ).collect( Collectors.toList() ) );
        returnType = make.TypeApply( make.Ident( classDecl.getSimpleName() ), tps );
      }
      else
      {
        returnType = make.Type( classDecl.sym.type );
      }
      return make.MethodDef(
        make.Modifiers( STATIC | (m.flags_field & (PRIVATE | PROTECTED | PUBLIC)) ),
        getNames().fromString( "asLink" ),
        returnType,
        typeParams,
        List.from( params ),
        make.Types( mt.getThrownTypes() ),
        make.Block( 0, List.of( make.Return( implClassCtor ) ) ),
        null );
    }

    private JCMethodDecl generateForwardingConstructor( JCMethodDecl meth )
    {
      TreeMaker make = getTreeMaker();
      make.pos = _classInfoStack.peek()._classDecl.pos;
      Names names = getNames();

      TreeCopier copier = new TreeCopier( make );
      JCMethodDecl copy = (JCMethodDecl)copier.copy( meth );

      // super(...)
      List<JCExpression> args = List.from(
        meth.params.stream().map( p -> make.Ident( p.name ) ).collect( Collectors.toList() ) );
      JCMethodInvocation superCall = make.Apply( List.nil(), make.Ident( names._super ), args );
      copy.body = make.Block( 0, List.of( make.Exec( superCall ) ) );

      return copy;
    }

    private void processLinkField( JCVariableDecl varDecl )
    {
      int modifiers = (int)varDecl.getModifiers().flags;

      ClassInfo classInfo = _classInfoStack.peek();
      JCClassDecl classDecl = classInfo._classDecl;
      if( classDecl.defs.contains( varDecl ) )
      {
        JCAnnotation linkAnno = getAnnotation( varDecl, link.class );
        if( linkAnno == null )
        {
          // not a link field
          return;
        }

        if( varDecl.sym.isStatic() )
        {
          reportError( varDecl, MSG_LINK_STATIC_FIELD.get() );
          return;
        }

        checkModifiersAndApplyDefaults( varDecl, modifiers, classDecl );

        addLinkedInterfaces( linkAnno, classInfo, varDecl );
      }
    }

    private void checkModifiersAndApplyDefaults( JCVariableDecl varDecl, int modifiers, JCClassDecl classDecl )
    {
      if( getAnnotationMirror( classDecl.sym, part.class ) == null )
      {
        // modifier restrictions and defaults apply only to links declared in part classes
        return;
      }

      if( (modifiers & (PUBLIC | PROTECTED)) != 0 )
      {
        reportError( varDecl.getModifiers(), MSG_MODIFIER_NOT_ALLOWED_HERE.get( (modifiers & PUBLIC) != 0 ? "public" : "protected") );
      }

      if( (modifiers & PRIVATE) != 0 )
      {
        reportWarning( varDecl.getModifiers(), MSG_MODIFIER_REDUNDANT_FOR_LINK.get( "private" ) );
      }
      else
      {
        // default @link fields to PRIVATE
        varDecl.getModifiers().flags |= PRIVATE;
      }

      if( (modifiers & FINAL) != 0 )
      {
        reportWarning( varDecl.getModifiers(), MSG_MODIFIER_REDUNDANT_FOR_LINK.get( "final" ) );
      }
      else
      {
        // default @link fields to FINAL
        varDecl.getModifiers().flags |= FINAL;
      }
    }

    private void addLinkedInterfaces( JCAnnotation linkAnno, ClassInfo ci, JCVariableDecl field )
    {
      Type fieldType = field.sym.type;
      if( !fieldType.isInterface() )
      {
        reportError( linkAnno, MSG_INTERFACE_LINK_FIELD_TYPE_EXPECTED.get() );
      }
      else if( ci.getInterfaces().stream().noneMatch( t -> isSameType( t, fieldType ) ) )
      {
        // the delegating class must implement the field's interface
        reportError( linkAnno, MSG_DELEGATING_CLASS_DOES_NOT_IMPLEMENT.get( ci._classDecl.name, fieldType, field.name ) );
      }

      ci.getLinks().put( field, new LinkInfo( field, fieldType, getSharedInterfacesFromLink( linkAnno ) ) );
    }

    ArrayList<ClassType> minimizeInterfaces( ArrayList<ClassType> list )
    {
      Set<ClassType> result = new LinkedHashSet<>();
      outer:
      for( int i = 0; i < list.size(); i++ )
      {
        ClassType ti = list.get( i );
        for( int j = 0; j < list.size(); j++ )
        {
          if( i == j )
          {
            continue;
          }

          ClassType tj = list.get( j );
          if( !isSameType( ti, tj ) && isSubtype( tj, ti ) )
          {
            continue outer; // ti is redundant
          }
        }
        result.add( ti );
      }

      return new ArrayList<>( result );
    }

    private void linkInterfaces( LinkInfo li )
    {
      for( Set<NamedMethodType> mtSet : li.getMethodTypes().values() )
      {
        for( NamedMethodType mt : mtSet )
        {
          ClassInfo ci = _classInfoStack.peek();
          if( !ci.getDefaultMethodForwarders().contains( mt ) )
          {
            generateInterfaceImplMethod( li, mt );
          }
        }
      }
    }

    private void generateInterfaceImplMethod( LinkInfo li, NamedMethodType namedMt )
    {
      JCVariableDecl linkField = li.getLinkField();
      Type linkType = linkField.vartype.type;
      if( !linkType.isInterface() )
      {
        ArrayList<MethodSymbol> unimpled = findUnimplementedInterfaceMethods( (ClassSymbol)linkType.tsym );
        for( MethodSymbol m : unimpled )
        {
          if( new NamedMethodType( m, m.type ).equals( namedMt ) )
          {
            // force delegating class to impl abstract methods
            return;
          }
        }
      }
      Type csr = namedMt.getType();
      while( csr instanceof Type.DelegatedType )
      {
        csr = ((Type.DelegatedType)csr).qtype;
      }
      MethodType mt = (MethodType)csr;

      TreeMaker make = getTreeMaker();
      make.pos = linkField.pos;


      // Method name & modifiers
      JCExpression generated = memberAccess( make, Generated.class.getName() );
      JCModifiers access = make.Modifiers( PUBLIC, List.of( make.Annotation( generated, List.nil() ) ) );
      Names names = getNames();
      Name name = namedMt.getName();

      // Throws
      List<JCExpression> thrown = make.Types( mt.getThrownTypes() );

      // Type params
      List<JCTypeParameter> typeParams;
      if( namedMt.getType() instanceof Type.ForAll )
      {
        typeParams = make.TypeParams( namedMt.getType().getTypeArguments() );
      }
      else
      {
        List<Type> typeParamTypes = List.from( namedMt.getMethodSymbol().getTypeParameters().stream().map( tp -> tp.type )
          .collect( Collectors.toList() ) );
        typeParams = make.TypeParams( typeParamTypes );
      }

      // Params
      List<Type> parameterTypes = mt.getParameterTypes();
      ArrayList<JCVariableDecl> params = new ArrayList<>();
      for( int i = 0; i < parameterTypes.size(); i++ )
      {
        Type pt = parameterTypes.get( i );
        Name paramName = names.fromString( "$param" + i );
        JCExpression paramType = make.Type( pt );
        JCVariableDecl param = make.VarDef( make.Modifiers( FINAL | Flags.PARAMETER ), paramName, paramType, null );
        params.add( param );
      }

      // Return type
      JCExpression resType = make.Type( mt.getReturnType() );

      // Forward call statement
      JCExpression link = make.Ident( linkField );
      JCTree.JCFieldAccess forwardRef = IDynamicJdk.instance().Select( make, link, namedMt.getMethodSymbol() );
      forwardRef.type = mt.getReturnType();
      java.util.List<JCExpression> args = params.stream().map( p -> make.Ident( p.name ) ).collect( Collectors.toList() );
      JCTree.JCMethodInvocation forwardCall = make.Apply( List.nil(), forwardRef, List.from( args ) );
      forwardCall.type = forwardRef.type;
      ((JCTree.JCFieldAccess)forwardCall.meth).sym = namedMt.getMethodSymbol();

      JCStatement forwardStmt;
      if( isSameType( mt.getReturnType(), getSymtab().voidType ) )
      {
        forwardStmt = make.Exec( forwardCall );
      }
      else
      {
        forwardStmt = make.Return( forwardCall );
      }

      JCBlock block = make.Block( 0, List.of( forwardStmt ) );
      JCMethodDecl ifaceMethod = make.MethodDef( access, name, resType, typeParams, List.from( params ), thrown, block, null );
      li.addGeneratedMethod( ifaceMethod );
    }
  }

  private JCAnnotation suppressRawTypesWarnings( TreeMaker make )
  {
    // @SuppressWarnings("rawtypes") so we can use raw java.lang.Class and not Class<?> without warnings in user files
    JCExpression sw = make.Type( getSymtab().suppressWarningsType );
    JCExpression val = make.Literal( "rawtypes" ); // + "unchecked" if the array init needs it??
    return make.Annotation( sw, List.of( make.Assign( make.Ident( getNames().value),
                                                                  make.NewArray( null, List.nil(), List.of( val ) ) ) ) );
  }

  private void memberEnter( JCTree memberDecl, JCClassDecl classDecl )
  {
    ReflectUtil.method( MemberEnter.instance( getContext() ), "memberEnter", JCTree.class, Env.class )
      .invoke( memberDecl, Enter.instance( getContext() ).getClassEnv( classDecl.sym ) );
  }

  private void removeDups( ArrayList<ClassType> interfaces )
  {
    Types types = getTypes();
    for( int i = 0; i < interfaces.size(); i++ )
    {
      ClassType ti = interfaces.get( i );
      for( int j = i+1; j < interfaces.size(); j++ )
      {
        ClassType tj = interfaces.get( j );
        if( types.isSameType( ti, tj ) )
        {
          interfaces.remove( j-- );
        }
      }
    }
  }

  private Set<ClassType> getSharedInterfacesFromLink( JCAnnotation linkAnno )
  {
    Set<ClassType> share = new LinkedHashSet<>();

    List<JCExpression> args = linkAnno.getArguments();
    if( args.isEmpty() )
    {
      return share;
    }

    Attribute.Compound annoValues = linkAnno.attribute.getValue();
    int i = 0;
    for( Map.Entry<MethodSymbol, Attribute> entry: annoValues.getElementValues().entrySet() )
    {
      MethodSymbol argSym = entry.getKey();
      Attribute value = entry.getValue();
      if( argSym.name.toString().equals( "share" ) )
      {
        processClassType( share, value, args.get( i ) );
      }
      else
      {
        throw new IllegalStateException();
      }
      i++;
    }
    return share;
  }

  private void processClassType( Set<ClassType> share, Attribute value, JCExpression expr )
  {
    if( value instanceof Attribute.Class )
    {
      processClassType( (Attribute.Class)value, share, expr );
    }
    if( value instanceof Attribute.Array )
    {
      for( Attribute cls : ((Attribute.Array)value).values )
      {
        processClassType( (Attribute.Class)cls, share, expr );
      }
    }
  }

  private void processClassType( Attribute.Class value, Set<ClassType> interfaces, JCExpression location )
  {
    ClassType classType = (ClassType)value.classType;
    if( classType.isInterface() )
    {
      interfaces.add( classType );
    }
    else
    {
      reportError( location, MSG_ONLY_INTERFACES_HERE.get() );
    }
  }

  // add $PartClass to implements clause
  private class Enter_Start extends TreeTranslator
  {
    private final Stack<JCClassDecl> _classDecls = new Stack<>();

    @Override
    public void visitClassDef( JCClassDecl classDecl )
    {
      boolean isInnerClass = !_classDecls.isEmpty();
      _classDecls.push( classDecl );
      try
      {
        if( classDecl.mods.annotations.stream().noneMatch( anno ->
                                                             anno.annotationType.toString().equals( part.class.getSimpleName() ) ||
                                                             anno.annotationType.toString().equals( part.class.getTypeName() ) ) )
        {
          // not a @part class
          super.visitClassDef( classDecl );
          return;
        }

        if( classDecl.implementing.stream().anyMatch( e -> e.toString().contains( $PartClass.class.getSimpleName() ) ) )
        {
          // already processed, probably an annotation processing round
          result = classDecl;
          return;
        }

        // add $PartClass to interfaces as a marker for quicker instanceof part check
        List<JCExpression> implementsClause = classDecl.getImplementsClause();
        if( implementsClause.stream().noneMatch( iface -> iface.toString().contains( $PartClass.class.getSimpleName() ) ) )
        {
          // add $PartClass to interfaces if not already added
          TreeMaker make = getTreeMaker();
          make.pos = classDecl.pos;
          classDecl.implementing = implementsClause
            .append( JCTreeUtil.memberAccess( make, getNames(), $PartClass.class.getTypeName() ) );
        }

        if( isInnerClass )
        {
          // part inner classes are always static (generated static fields)
          classDecl.mods.flags |= STATIC;
        }

        generate$ImplClassShell( classDecl );

        super.visitClassDef( classDecl );
      }
      finally
      {
        _classDecls.pop();
      }
    }

    private void generate$ImplClassShell( JCClassDecl classDecl )
    {
      if( (classDecl.mods.flags & ABSTRACT) == 0 ||
          classDecl.getModifiers().getAnnotations().stream()
            .noneMatch( expr -> expr.annotationType.toString().contains( "part" ) ) )
      {
        return;
      }

      TreeMaker make = getTreeMaker();
      make.pos = classDecl.pos;
      Names names = getNames();
      TreeCopier copier = new TreeCopier( make );

      JCExpression extendedType;
      if( classDecl.getTypeParameters().isEmpty() )
      {
        extendedType = make.Ident( classDecl.name );
      }
      else
      {
        List<JCExpression> typeParams = List.from( classDecl.getTypeParameters().stream().map( tp -> make.Ident( tp.name ) ).collect( Collectors.toList() ) );
        extendedType = make.TypeApply( make.Ident( classDecl.getSimpleName() ), typeParams );
      }

      JCClassDecl implClass = make.ClassDef(
        make.Modifiers( PRIVATE | STATIC, List.of( make.Annotation( memberAccess( make, part.class.getTypeName() ), List.nil() ) ) ),
        names.fromString( "$Impl" ),
        copier.copy( classDecl.getTypeParameters() ),
        extendedType, // extends the abstract @part
        List.nil(),
        List.nil()
      );

      classDecl.defs = classDecl.defs.append( implClass );
    }
  }

  // - reduce parenthesized expressions of the form (this) and (Foo.this) to this and Foo.this. To make 'this' replacements
  // easier to deal with.
  //
  private class Analyze_Start extends TreeTranslator
  {
    @Override
    public void visitParens( JCParens tree )
    {
      super.visitParens( tree );
      if( tree.expr instanceof JCIdent && tree.expr.toString().equals( "this" ) || 
          tree.expr instanceof JCFieldAccess && tree.expr.toString().endsWith( ".this" ) )
      {
        result = tree.expr;
      }
    }
  }

  // - for @link fields, assign linking class instance to '$selves[<interface index>]' of part classes
  // - for @part classes, replace 'this' with '$selves[<interface index>]' where applicable
  //
  private class Analyze_Finish extends TreeTranslator
  {
    private final Stack<JCClassDecl> _classDeclStack = new Stack<>();

    @Override
    public void visitClassDef( JCClassDecl tree )
    {
      _classDeclStack.push( tree );
      try
      {
        super.visitClassDef( tree );
      }
      finally
      {
        if( tree != _classDeclStack.pop() )
        {
          throw new IllegalStateException();
        }
      }
    }

    @Override
    public void visitIdent( JCIdent tree )
    {
      super.visitIdent( tree );

      if( tree.name.toString().equals( "this" ) )
      {
        replaceThis_Explicit( tree );
      }
      else
      {
        prohibitLinkFieldUse( tree );
      }
    }

    @Override
    public void visitSelect( JCFieldAccess tree )
    {
      super.visitSelect( tree );

      if( tree.toString().endsWith( ".this" ) )
      {
        replaceThis_Explicit( tree );
      }
      else
      {
        prohibitLinkFieldUse( tree );
      }
    }

    private void prohibitLinkFieldUse( JCExpression tree )
    {
      JCClassDecl classDecl = _classDeclStack.peek();
      if( !isInPartClass( classDecl.sym ) ||
          tree.pos == classDecl.pos ||  // classDecl.pos means tree is generated
          notInInstanceMethod( tree ) ) // probably in a constructor, which is where fields may be assigned
      {
        return;
      }

      Symbol linkFieldRef = getLinkFieldRef( tree );
      if( linkFieldRef != null )
      {
        JCVariableDecl linkField = getField( classDecl, linkFieldRef );
        if( linkField == null || tree.pos == linkField.pos ) // linkField.pos means tree is generated
        {
          // not a link field ref, or is generated
          return;
        }

        Tree parent = getParent( tree );
        while( !(parent instanceof JCMethodInvocation) )
        {
          parent = getParent( parent );
          if( parent == null )
          {
            break;
          }
        }

        if( parent != null )
        {
          JCMethodInvocation m = (JCMethodInvocation)parent;
          JCExpression methodSelect = m.getMethodSelect();
          for( parent = tree; parent != null; parent = getParent( parent ) )
          {
            if( parent == methodSelect )
            {
              // interface method call is ok (analog to super.methodCall())
              return;
            }
          }
        }

        reportError( tree, MSG_PART_LINKFIELD_USE.get( tree.toString() ) );
      }
    }

    private void replaceThis_Explicit( JCExpression tree )
    {
      if( isPartClass( tree.type.tsym ) )
      {
        if( !replaceThisArgument( tree ) &&
          !replaceThisReturn( tree ) &&
          !replaceThisCast( tree ) &&
          !replaceThisTernary( tree ) &&
          !replaceThisAssignment( tree ) )
        {
        }
      }
    }

    private boolean replaceThisAssignment( JCExpression tree )
    {
      Tree parent = getParent( tree );
      if( parent instanceof JCAssign )
      {
        JCAssign assignment = (JCAssign)parent;
        if( assignment.type.isInterface() )
        {
          JCClassDecl classDecl = findClassDecl( tree.type );
          result = getSelf( tree, classDecl, assignment.type );
        }
        else if( !isSameType( assignment.type, getSymtab().objectType ) )
        {
          reportError( tree, MSG_PART_THIS_NONINTERFACE_USE.get() );
        }
        return true;
      }
      else if( parent instanceof JCVariableDecl )
      {
        JCVariableDecl varDecl = (JCVariableDecl)parent;
        if( varDecl.getType().type.isInterface() )
        {
          JCClassDecl classDecl = findClassDecl( tree.type );
          result = getSelf( tree, classDecl, varDecl.getType().type );
        }
        else if( !isSameType( varDecl.getType().type, getSymtab().objectType ))
        {
          reportError( tree, MSG_PART_THIS_NONINTERFACE_USE.get() );
        }
        return true;
      }
      return false;
    }

    private boolean replaceThisTernary( JCExpression tree )
    {
      Tree parent = getParent( tree );
      if( parent instanceof JCConditional )
      {
        JCConditional ternary = (JCConditional)parent;
        if( ternary.type.isInterface() )
        {
          JCClassDecl classDecl = findClassDecl( tree.type );
          result = getSelf( tree, classDecl, ternary.type );
        }
        else if( !isSameType( ternary.type, getSymtab().objectType ) )
        {
          reportError( tree, MSG_PART_THIS_NONINTERFACE_USE.get() );
        }
        return true;
      }
      return false;
    }

    private boolean replaceThisCast( JCExpression tree )
    {
      Tree parent = getParent( tree );
      if( parent instanceof JCTypeCast )
      {
        JCTypeCast cast = (JCTypeCast)parent;
        if( cast.type.isInterface() )
        {
          JCClassDecl classDecl = findClassDecl( tree.type );
          result = getSelf( tree, classDecl, cast.type );
        }
        else if( !isSameType( cast.type, getSymtab().objectType ) )
        {
          reportError( tree, MSG_PART_THIS_NONINTERFACE_USE.get() );
        }
        return true;
      }
      return false;
    }

    private boolean replaceThisReturn( JCExpression tree )
    {
      Tree parent = getParent( tree );
      if( parent instanceof JCReturn )
      {
        JCReturn retStmt = (JCReturn)parent;
        if( retStmt.expr == tree )
        {
          Types types = getTypes();
          JCMethodDecl method = findMethod( retStmt );
          if( method != null && !LINK_PART_TO_SELF.equals( method.name.toString() ) )
          {
            Type returnType = types.erasure( method.sym.getReturnType() );
            if( returnType.isInterface() )
            {
              JCClassDecl classDecl = findClassDecl( tree.type );
              result = getSelf( tree, classDecl, returnType );
            }
            else if( !types.isSameType( getSymtab().objectType, returnType ) )
            {
              // Note, Object is permitted for local identity purposes
              reportError( tree, MSG_PART_THIS_NONINTERFACE_USE.get() );
            }
          }
        }
        return true;
      }
      return false;
    }

    private JCMethodDecl findMethod( Tree tree )
    {
      if( tree == null )
      {
        return null;
      }

      if( tree instanceof JCMethodDecl )
      {
        return (JCMethodDecl)tree;
      }

      return findMethod( getParent( tree ) );
    }

    private boolean replaceThisArgument( JCExpression tree )
    {
      Tree parent = getParent( tree );
      if( parent instanceof JCMethodInvocation )
      {
        JCMethodInvocation m = (JCMethodInvocation)parent;
        if( m.getArguments() != null && m.getArguments().contains( tree ) && !isException_Arg( m ) )
        {
          int index = m.getArguments().indexOf( tree );
          Symbol sym = m.meth instanceof JCFieldAccess ? ((JCFieldAccess)m.meth).sym : ((JCIdent)m.meth).sym;
          if( !(sym instanceof MethodSymbol) )
          {
            return false;
          }
          if( sym.owner.type.tsym.getQualifiedName().toString().equals( $PartClass.Internal.class.getCanonicalName() ) )
          {
            // call to $PartClass.Interal, do not replace
            return false;
          }
          Symbol.VarSymbol paramSym = ((MethodSymbol)sym).getParameters().get( index );
          Types types = getTypes();
          Type paramType = types.erasure( paramSym.type );
          if( paramType.isInterface() )
          {
            JCClassDecl classDecl = findClassDecl( m.args.get( index ).type );
            if( classDecl != null )
            {
              result = getSelf( tree, classDecl, paramType );
            }
            return true;
          }
          else if( !types.isSameType( getSymtab().objectType, paramType ) )
          {
            // Note, Object is permitted for local identity purposes
            reportError( tree, MSG_PART_THIS_NONINTERFACE_USE.get() );
          }
          return true;
        }
      }
      return false;
    }

    @Override
    public void visitApply( JCMethodInvocation tree )
    {
      super.visitApply( tree );

      if( !replaceThisMethodCall( tree ) )
      {
        replaceSuperDefaultMethodCall( tree, false );
      }
      checkInternalCall( tree );
    }

    private void checkInternalCall( JCMethodInvocation tree )
    {
      if( !(tree.meth instanceof JCFieldAccess) )
      {
        return;
      }

      JCFieldAccess fa = (JCFieldAccess)tree.meth;
      if( !(fa.sym instanceof MethodSymbol) )
      {
        return;
      }
      MethodSymbol msym = (MethodSymbol)fa.sym;

      if( !hasInternalAnnotation( msym, new HashSet<>() ) )
      {
        return;
      }

      JCMethodDecl enclosingMethod = getEnclosingMethod( tree );
      if( enclosingMethod != null && enclosingMethod.getName().contentEquals( LINK_PART_TO_SELF ) )
      {
        return; // ignore recursive call in generated $linkPartToSelf
      }

      if( tree.meth instanceof JCFieldAccess )
      {
        if( fa.selected instanceof JCIdent && ((JCIdent)fa.selected).name.toString().equals( "this" ) ||
            fa.selected instanceof JCFieldAccess && fa.selected.toString().endsWith( ".this" ) ||
            fa.selected instanceof JCIdent && ((JCIdent)fa.selected).name.toString().equals( "super" ) ||
            fa.selected instanceof JCFieldAccess && fa.selected.toString().endsWith( ".super" ) )
        {
          // this or super access
          return;
        }

        if( fa.selected instanceof JCIdent )
        {
          JCIdent ident = (JCIdent)fa.selected;
          boolean isLinkFieldRef = ident.sym.getAnnotation( link.class ) != null;
          if( isLinkFieldRef )
          {
            // link field access
            return;
          }
        }

        reportError( tree, MSG_INTERNAL_ACCESS_NOT_ALLOWED_HERE.get( msym, msym.owner.getQualifiedName() ) );
      }
    }

    private boolean hasInternalAnnotation( MethodSymbol m, Set<MethodSymbol> visited )
    {
      if( !visited.add( m ) )
      {
        return false;
      }

      if( hasInternalAnnotation( m ) )
      {
        return true;
      }

      // walk supers
      ClassSymbol owner = (ClassSymbol)m.owner;
      for( Type sup : interfaceClosure( owner.type ) )
      {
        if( sup.tsym == owner )
        {
          continue;
        }

        for( Symbol sym : IDynamicJdk.instance().getMembersByName( (ClassSymbol)sup.tsym, m.name ) )
        {
          if( sym instanceof MethodSymbol )
          {
            if( m.overrides( sym, owner, getTypes(), false ) )
            {
              if( hasInternalAnnotation( (MethodSymbol)sym, visited ) )
              {
                return true;
              }
            }
          }
        }
      }
      return false;
    }

    private boolean hasInternalAnnotation( MethodSymbol m )
    {
      for( Attribute.Compound attr : m.getRawAttributes() )
      {
        if( attr.type.tsym.getQualifiedName().contentEquals( internal.class.getTypeName() ) )
        {
          return true;
        }
      }
      return false;
    }


    @Override
    public void visitExec( JCExpressionStatement tree )
    {
      super.visitExec( tree );

      if( tree.expr instanceof JCMethodInvocation )
      {
        replaceSuperDefaultMethodCall( (JCMethodInvocation)tree.expr, true );
      }
    }

    private boolean replaceThisMethodCall( JCMethodInvocation tree )
    {
      JCClassDecl classDecl = _classDeclStack.peek();
      if( !isInPartClass( classDecl.sym ) )
      {
        return false;
      }

      Symbol sym = null;
      if( tree.meth instanceof JCIdent )
      {
        sym = ((JCIdent)tree.meth).sym;
      }
      else if( tree.meth instanceof JCFieldAccess )
      {
        JCFieldAccess fa = (JCFieldAccess)tree.meth;
        if( fa.selected instanceof JCIdent && ((JCIdent)fa.selected).name.toString().equals( "this" ) ||
            fa.selected instanceof JCFieldAccess && fa.selected.toString().endsWith( ".this" ) )
        {
          sym = ((JCFieldAccess)tree.meth).sym;
        }
      }

      if( !(sym instanceof MethodSymbol) )
      {
        return false;
      }

      MethodSymbol methSym = (MethodSymbol)sym;
      if( methSym.isStatic() )
      {
        return false;
      }

      Pair<JCClassDecl, Type> enclClass_Iface = findInterfaceOfEnclosingTypeThatSymImplements( tree.meth, methSym );
      if( enclClass_Iface == null || !isPartClass( enclClass_Iface.fst.sym ) )
      {
        return false;
      }

      if( enclClass_Iface.snd != null )
      {
        Type iface = enclClass_Iface.snd;
        JCClassDecl encClass = enclClass_Iface.fst;
        JCExpression thisSub = getSelf( tree, encClass, iface );
        TreeMaker make = getTreeMaker();
        make.pos = tree.pos;

        // For a method call like `this.foo("hi")` we not only need to swap out `this` with `selves[i]`, but since `selves[i]`
        // is always an interface-directed receiver and `this` is always an implementor, we must also re-resolve the method
        // as seen from the interface. This is necessary, for example, with generic interfaces like:
        // `interface Foo<T> { void foo(T t ); }` where:
        // `class StringFoo implements Foo<String> { public void foo(String t) {...} }`.
        // If we rewrite the receiver without re-resolving the method, the call will result in a runtime exception
        // because `this` statically resolves as `FooString` so the method originally resolves as `foo(String)` which
        // does not exist in interface `Foo`. The Foo-resolved method is `Foo(Object)`. Note, the runtime class of `this`
        // will have a bridge method for Foo(Object) that redirects the call to its Foo(Stirng) implementation.
        methSym = resolveMethod( getContext(), getCompilationUnit(), tree.pos(),
                                 methSym.name, iface,
                                 List.from( tree.args.stream().map( e -> e.type ).collect( Collectors.toList() ) ) );

        JCMethodInvocation apply = make.Apply( List.nil(), IDynamicJdk.instance().Select( make, thisSub, methSym ), tree.args );
        apply.type = tree.type;

        result = apply;
        return true;
      }
      return false;
    }

    private JCClassDecl findClassDecl( Type type )
    {
      for( int i = _classDeclStack.size()-1; i >= 0; i-- )
      {
        JCClassDecl classDecl = _classDeclStack.get( i );
        Types types = getTypes();
        if( isPartClass( classDecl.sym ) && types.isSameType( types.erasure( classDecl.sym.type ), types.erasure( type ) ) )
        {
          return classDecl;
        }
      }
      return null;
    }

    private Pair<JCClassDecl, Type> findInterfaceOfEnclosingTypeThatSymImplements( JCExpression meth, MethodSymbol sym )
    {
      for( int i = _classDeclStack.size()-1; i >= 0; i-- )
      {
        JCClassDecl classDecl = _classDeclStack.get( i );
        if( isPartClass( classDecl.sym ) )
        {
          ArrayList<ClassType> interfaces = new ArrayList<>();
          findAllInterfaces( classDecl.sym.type, new HashSet<>(), interfaces );
          interfaces = maximalInterfaces( interfaces );
          List<Pair<ClassType, ClassType>> matches = List.nil();
          for( ClassType iface : interfaces )
          {
            for( Type t : interfaceClosure( iface ) )
            {
              for( Symbol mm : IDynamicJdk.instance().getMembersByName( (ClassSymbol)t.tsym, sym.name ) )
              {
                if( sym.overrides( mm, t.tsym, getTypes(), false ) )
                {
                  matches = matches.append( new Pair<>( iface, (ClassType)t ) );
                }
              }
            }
          }
          if( !matches.isEmpty() )
          {
            if( matches.size() > 1 )
            {
              reportAmbiguousReceiverError( meth, sym, matches );
            }
            return Pair.of( classDecl, matches.head.snd );
          }
        }
      }
      return null;
    }

    private void reportAmbiguousReceiverError( JCExpression meth, MethodSymbol sym, List<Pair<ClassType, ClassType>> matches )
    {
      for( Pair<ClassType, ClassType> pair: matches )
      {
        if( pair.snd != matches.get( 0 ).snd )
        {
          String ifaceList = matches.stream()
            .map( e -> e.snd.tsym.getSimpleName() )
            .collect( Collectors.joining( ", " ) );
          reportError( meth, MSG_AMBIGUOUS_RECEIVER.get(
            sym.toString(), ifaceList, matches.head.snd.tsym.getSimpleName() ) );
          return;
        }
      }
      String ifaceList = matches.stream()
        .map( e -> e.fst.tsym.getSimpleName() )
        .collect( Collectors.joining( ", " ) );
      reportError( meth, MSG_AMBIGUOUS_RECEIVER.get(
        sym.toString(), ifaceList, matches.head.snd.tsym.getSimpleName() ) );
    }

    // given {BigInteger, Number, List, Collection, Iterable}, returns {BigInteger, List}
    private ArrayList<ClassType> maximalInterfaces( ArrayList<ClassType> interfaces )
    {
      ArrayList<ClassType> maximal = interfaces.stream()
        .map( t -> (ClassType)erasure( t ) ).collect( Collectors.toCollection( ArrayList::new ) );
      for( int i = 0; i < maximal.size(); i++ )
      {
        ClassType iface = maximal.get( i );
        for( int j = 0; j < maximal.size(); j++ )
        {
          if( i == j )
          {
            continue;
          }

          ClassType jface = maximal.get( j );
          if( isSubtype( iface, jface ) )
          {
            if( j < i )
            {
              i--;
            }

            maximal.remove( j );
            j--;
          }
        }
      }
      return maximal;
    }

    private boolean isException_Arg( JCMethodInvocation tree )
    {
      if( tree.meth instanceof JCFieldAccess && ((JCFieldAccess)tree.meth).selected instanceof JCFieldAccess )
      {
        JCFieldAccess fa = (JCFieldAccess)((JCFieldAccess)tree.meth).selected;
        if( fa.type instanceof ClassType &&
          fa.type.tsym.getQualifiedName().toString().equals( $PartClass.Internal.class.getCanonicalName() ) )
        {
          // don't replace 'this' for generated methods
          return true;
        }
      }
      return false;
    }

    private void replaceSuperDefaultMethodCall( JCMethodInvocation superInterfaceCall, boolean exec )
    {
      if( !exec && getParent( superInterfaceCall ) instanceof JCExpressionStatement )
      {
        return;
      }

      JCClassDecl classDecl = _classDeclStack.peek();
      if( !isInPartClass( classDecl.sym ) )
      {
        return;
      }

      JCFieldAccess tree = findSuperInterfaceSelect( superInterfaceCall );
      if( tree == null )
      {
        return;
      }

      JCFieldAccess meth = (JCFieldAccess)superInterfaceCall.meth;
      Type iface = meth.selected.type;
      if( iface.isErroneous() || !(meth.sym instanceof MethodSymbol) )
      {
        return;
      }

      NamedMethodType namedMt = new NamedMethodType( (MethodSymbol)meth.sym, meth.type );

      // invoke the method using the interface of the method's declaring type so that the proper interface is used for dispatch
      iface = meth.sym.owner.type;

      Type csr = namedMt.getType();
      while( csr instanceof Type.DelegatedType )
      {
        csr = ((Type.DelegatedType)csr).qtype;
      }
      MethodType mt = (MethodType)csr;

      callDefaultMethodWithInvokeDynamic( superInterfaceCall, exec, mt, classDecl, iface, namedMt );
    }

    private void callDefaultMethodWithInvokeDynamic( JCMethodInvocation superInterfaceCall, boolean exec, MethodType mt, JCClassDecl classDecl, Type iface, NamedMethodType namedMt )
    {
      TreeMaker make = getTreeMaker();
      make.pos = superInterfaceCall.pos;

      Names names = getNames();
      Symtab symtab = getSymtab();
      Types types = getTypes();

      // $PartClass.Internal class sym
      ClassSymbol internalMethodsClassSym = getRtClassSym( $PartClass.Internal.class );

      // ReflectUtil.invokeDefaultAsSelf

      // Arg list
      List<JCExpression> theArgs = List.of( getSelf( superInterfaceCall, classDecl, iface ) )
        .appendList( superInterfaceCall.args );

      // make invokeDefault() call
      Name bsmName = getNames().fromString( "bootstrapDefault" );
      Type methType = superInterfaceCall.meth.type;
      MethodType indyType = new MethodType( List.of( iface ).appendList( methType.getParameterTypes() ), methType.getReturnType(), List.nil(), symtab.methodClass );
      JCMethodInvocation invokeDefaultCall = makeIndyCall(
        make, superInterfaceCall, internalMethodsClassSym.type, bsmName, indyType, theArgs, ((JCFieldAccess)superInterfaceCall.meth).name );

      result = exec ? make.Exec( invokeDefaultCall ) : invokeDefaultCall;
    }

    private JCMethodInvocation makeIndyCall( TreeMaker make,
                                             JCDiagnostic.DiagnosticPosition pos, Type site, Name bsmName,
                                             MethodType indyType, List<JCExpression> indyArgs,
                                             Name methName) {
      make.at(pos);
      Symtab syms = getSymtab();
      List<Type> bsm_staticArgs = List.of(syms.methodHandleLookupType,
                                          syms.stringType,
                                          syms.methodTypeType);

      MethodSymbol bsm = resolveMethod( getContext(), getCompilationUnit(), pos, bsmName, site, bsm_staticArgs );

      Symbol.DynamicMethodSymbol dynSym;
      if( JreUtil.isJava17orLater() )
      {
        Class<?> loadableConstantType = ReflectUtil.type( "com.sun.tools.javac.jvm.PoolConstant$LoadableConstant" );
        Object emptyLoadableConstantArray = ReflectUtil.method( Array.class, "newInstance", Class.class, int.class ).invokeStatic( loadableConstantType, 0 );

        dynSym = (Symbol.DynamicMethodSymbol)ReflectUtil.constructor( "com.sun.tools.javac.code.Symbol$DynamicMethodSymbol",
                                                                      Name.class, Symbol.class, ReflectUtil.type( "com.sun.tools.javac.code.Symbol$MethodHandleSymbol" ),
                                                                      Type.class, emptyLoadableConstantArray.getClass() )
          .newInstance(
            methName,
            syms.noSymbol,
            ReflectUtil.method( bsm, "asHandle" ).invoke(),
            indyType,
            emptyLoadableConstantArray );
      }
      else
      {
        dynSym = new Symbol.DynamicMethodSymbol( methName,
                                                 syms.noSymbol,
                                                 ClassFile.REF_invokeStatic,
                                                 bsm,
                                                 indyType,
                                                 new Object[0] );
      }
      JCFieldAccess qualifier = make.Select( make.QualIdent( site.tsym ), bsmName );
      qualifier.sym = dynSym;
      qualifier.type = indyType;

      JCMethodInvocation proxyCall = make.Apply( List.nil(), qualifier, indyArgs );
      proxyCall.type = indyType.getReturnType();
      return proxyCall;
    }

    private JCFieldAccess findSuperInterfaceSelect( JCMethodInvocation methodCall )
    {
      for( JCTree csr = methodCall.meth; csr instanceof JCFieldAccess; csr = ((JCFieldAccess)csr).selected )
      {
        if( csr.toString().endsWith( ".super" ) )
        {
          return (JCFieldAccess)csr;
        }
      }
      return null;
    }

    @Override
    public void visitAssign( JCAssign tree )
    {
      super.visitAssign( tree );

      Symbol linkField = getLinkFieldRef( tree.lhs );
      if( linkField != null )
      {
        tree.rhs = assignSelf( linkField, tree.rhs, tree );
      }
    }

    @Override
    public void visitVarDef( JCVariableDecl tree )
    {
      super.visitVarDef( tree );

      if( tree.init != null )
      {
        Symbol linkField = getLinkFieldRef( tree );
        if( linkField != null )
        {
          tree.init = assignSelf( linkField, tree.init, tree );
        }
      }
    }

    private JCExpression assignSelf( Symbol linkField, JCExpression rhs, JCTree assignmentOrVarDecl )
    {
      // replace assigned expr
      //   field = <value-expr>
      // with method call expr
      //   field = (<field-ref-expr-type>)$PartClass.Internal.linkPart( this, linkScope, "field-name", <value-expr> )

      Symbol.ClassSymbol internalMethodsClassSym = getRtClassSym( $PartClass.Internal.class );

      Names names = getNames();

      Symtab symtab = getSymtab();
      Type.ArrayType arrayOfClassesType = getTypes().makeArrayType( symtab.classType );
      Symbol.MethodSymbol assignPartMethod = resolveMethod( getContext(), getCompilationUnit(),
                                                            assignmentOrVarDecl.pos(), names.fromString( "linkPart" ),
                                                            internalMethodsClassSym.type,
                                                            List.from( new Type[]{symtab.objectType, arrayOfClassesType, symtab.stringType, symtab.objectType} ) );

      TreeMaker make = getTreeMaker();
      make.pos = assignmentOrVarDecl.pos;

      List<Type> interfaces = getDelegatedInterfacesForWiring( linkField );
      verifyLinkedDelegatedInterfacesAgainstDelegateType( interfaces, rhs );
      List<JCExpression> interfaceTypes = List.from( interfaces.stream().map( t -> make.ClassLiteral( t ) ).collect( Collectors.toList() ) );
      JCNewArray interfaceArray = make.NewArray( make.Type( symtab.classType ), List.nil(), interfaceTypes );
      interfaceArray.type = arrayOfClassesType;

      JCTree.JCMethodInvocation assignPartCall = make.Apply( List.nil(),
        memberAccess( make, $PartClass.Internal.class.getCanonicalName() + ".linkPart" ),
        List.of( make.This( _classDeclStack.peek().type ), interfaceArray, make.Literal( linkField.name.toString() ), rhs ) );
      assignPartCall.type = symtab.objectType;
      JCTree.JCFieldAccess methodSelect = (JCTree.JCFieldAccess)assignPartCall.getMethodSelect();
      methodSelect.sym = assignPartMethod;
      methodSelect.type = assignPartMethod.type;
      assignTypes( methodSelect.selected, internalMethodsClassSym );

      //noinspection UnnecessaryLocalVariable
      JCTypeCast castExpr = make.TypeCast( linkField.type, assignPartCall );
      return castExpr;
    }

    //todo: this is not quite right. If linking interface is a superinterface of a component's @internal interface,
    // it should be reported an error if no other non-@internal interfaces are assignable to it. The method must be
    // revised to support this logic.
    private void verifyLinkedDelegatedInterfacesAgainstDelegateType( List<Type> interfaces, JCExpression rhs )
    {
      Symbol.TypeSymbol delegateSym = rhs.type.tsym;
      for( Attribute.TypeCompound attr : delegateSym.getRawTypeAttributes() )
      {
        if( !attr.type.tsym.getQualifiedName().toString().equals( internal.class.getTypeName() ) )
        {
          continue;
        }

        TypeAnnotationPosition p = attr.position;
        if( p.type == TargetType.CLASS_EXTENDS )
        {
          int ifaceIndex = p.type_index;

          List<Type> delegateClassImplementsList = ((ClassSymbol)rhs.type.tsym).getInterfaces();
          if( ifaceIndex >= 0 && ifaceIndex < delegateClassImplementsList.size() )
          {
            Type t = getTypes().erasure( delegateClassImplementsList.get( ifaceIndex ) );
            if( interfaces.stream().anyMatch( iface -> getTypes().isSameType( iface, t ) ) )
            {
              reportError( rhs, MSG_INTERFACE_IS_INTERNAL_TO_DELEGATE.get( t.tsym.getQualifiedName(), rhs.type.tsym.getQualifiedName() ) );
            }
          }
        }
      }
    }

    // Note, we recompute these here to include interfaces that would otherwise be bypassed by use of `share`
    private List<Type> getDelegatedInterfacesForWiring( Symbol linkFieldSym )
    {
      JCClassDecl classDecl = _classDeclStack.peek();
      JCVariableDecl linkField = (JCVariableDecl)classDecl.defs.stream()
        .filter( def -> def instanceof JCVariableDecl && ((JCVariableDecl)def).sym == linkFieldSym )
        .findFirst()
        .orElseThrow( () -> new IllegalStateException( "Should have found @link field for " + linkFieldSym.name ) );

      JCAnnotation linkAnno = getAnnotation( linkField, link.class );
      if( linkAnno == null )
      {
        // compile error was issued for this during enter
        return List.nil();
      }

      return interfaceClosure( linkFieldSym.type );
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
        JCTree.JCIdent ident = (JCTree.JCIdent)m;
        ident.sym = symbol;
        ident.type = symbol.type;
      }
    }

    private Symbol getLinkFieldRef( JCExpression lhs )
    {
      if( lhs instanceof JCIdent )
      {
        Symbol lhsSym = ((JCIdent)lhs).sym;
        if( lhsSym != null && lhsSym.getAnnotation( link.class ) != null )
        {
          return lhsSym;
        }
      }

      if( lhs instanceof JCFieldAccess && ((JCFieldAccess)lhs).sym.getAnnotation( link.class ) != null )
      {
        return ((JCFieldAccess)lhs).sym;
      }

      if( lhs instanceof JCParens )
      {
        return getLinkFieldRef( ((JCParens)lhs).expr );
      }
      return null;
    }

    private Symbol getLinkFieldRef( JCVariableDecl tree )
    {
      if( tree.sym != null && tree.sym.getAnnotation( link.class ) != null )
      {
        return tree.sym;
      }
      return null;
    }

    private boolean notInInstanceMethod( Tree tree )
    {
      Tree parent = getParent( tree );
      if( parent == null )
      {
        return true;
      }
      if( parent instanceof JCMethodDecl )
      {
        return ((JCMethodDecl)parent).sym.isStatic() || ((JCMethodDecl)parent).sym.isConstructor();
      }
      return notInInstanceMethod( parent );
    }
  }

  /**
   * generates `$selves[N]`, by computing N as the compile-time constant index of `iface`
   * which corresponds with its position in the list of interfaces obtained from ClassInfo#getInterfaces.
   */
  private JCExpression getSelf( JCTree tree, JCClassDecl receiverType, Type iface )
  {
    return getSelf( tree, receiverType, iface, true );
  }
  private JCExpression getSelf( JCTree tree, JCClassDecl receiverType, Type iface, boolean typed )
  {
    Names names = getNames();
    TreeMaker make = getTreeMaker();
    make.pos = tree.pos;

    JCLiteral interfaceIndex = make.Literal( TypeTag.INT, indexOfInterface( receiverType.sym.type, iface ) );
    interfaceIndex.type = getSymtab().intType;
    JCIdent selves = make.Ident( names.fromString( SELVES ) );
    if( typed )
    {
      selves.sym = resolveField( tree.pos(), getContext(), names.fromString( SELVES ), receiverType.type, receiverType );
      selves.type = getTypes().makeArrayType( getSymtab().objectType );
    }
    JCArrayAccess arrayAccessExpr = make.Indexed( selves, interfaceIndex );

    return make.TypeCast( iface, arrayAccessExpr );
  }

  // hasSelf tests if `iface` is "claimed": `selves[<index of `iface>] != this` means `iface` is dispatched through a delegating class
  private JCExpression hasSelf( JCExpression tree, JCClassDecl receiverType, Type iface )
  {
    TreeMaker make = getTreeMaker();
    make.pos = tree.pos;

    JCExpression getSelf = getSelf( tree, receiverType, iface );
    JCBinary hasSelf = make.Binary( Tag.NE, getSelf, make.QualThis( receiverType.sym.type ) );

    Env<AttrContext> classEnv = Enter.instance( getContext() ).getClassEnv( receiverType.sym );
    Attr.instance( getContext() ).attribExpr( hasSelf, classEnv );

    return hasSelf;
  }

  private JCVariableDecl getField( JCClassDecl classDecl, Symbol sym )
  {
    return (JCVariableDecl)classDecl.defs.stream()
      .filter( def -> def instanceof JCVariableDecl && ((JCVariableDecl)def).sym == sym )
      .findFirst()
      .orElse( null );
  }

  private void findAllInterfaces( Type type, Set<Type> seen, ArrayList<ClassType> result )
  {
    findAllInterfaces( type, seen, result, false );
  }
  private void findAllInterfaces( Type type, Set<Type> seen, ArrayList<ClassType> result, boolean excludeInternal )
  {
    if( seen.stream().anyMatch( t -> isSameType( t, type ) ) )
    {
      return;
    }
    seen.add( type );

    if( type.isInterface() && !isInterfaceExcluded( type ) )
    {
      if( result.stream()
        .noneMatch( e -> isSameType( e, type ) ) )
      {
        result.add( (ClassType)type );
      }
    }
    else
    {
      Type superClass = ((ClassSymbol)type.tsym).getSuperclass();
      findAllInterfaces( type, superClass, seen, result, excludeInternal );
    }

    List<Type> superInterfaces = getInterfaces( type, excludeInternal );
    if( superInterfaces != null )
    {
      superInterfaces.forEach( superInterface -> findAllInterfaces( type, superInterface, seen, result, excludeInternal ) );
    }
  }

  // Don't include interfaces in a class's implements list annotated with @internal
  private List<Type> getInterfaces( Type type, boolean excludeInternal )
  {
    ArrayList<Type> interfaces = new ArrayList<>( ((ClassSymbol)type.tsym).getInterfaces() );
    if( type.isInterface() )
    {
      // an interface can't have internal interfaces (only a class's implements clause may annotate interfaces with @internal)
      return List.from( interfaces );
    }

    if( excludeInternal )
    {
      Symbol.TypeSymbol delegateSym = type.tsym;
      for( Attribute.TypeCompound attr : delegateSym.getRawTypeAttributes() )
      {
        if( !attr.type.tsym.getQualifiedName().toString().equals( internal.class.getTypeName() ) )
        {
          continue;
        }

        TypeAnnotationPosition p = attr.position;
        if( p.type == TargetType.CLASS_EXTENDS )
        {
          int ifaceIndex = p.type_index;
          if( ifaceIndex >= 0 && ifaceIndex < interfaces.size() )
          {
            interfaces.set( ifaceIndex, null );
          }
        }
      }
      interfaces.removeIf( e -> e == null );
    }

    return List.from( interfaces );
  }

  private static boolean isInterfaceExcluded( Type type )
  {
    // internal interface $PartClass should never be included as a delegatable interface
    return type.tsym.getQualifiedName().toString().equals( $PartClass.class.getTypeName() );
  }

  private void findAllInterfaces( Type type, Type superType, Set<Type> seen, ArrayList<ClassType> result, boolean excludeInternal )
  {
    if( isSameType( getSymtab().objectType, superType ) )
    {
      return;
    }

    superType = getUnderlyingType( superType );
    if( superType != Type.noType && !superType.isErroneous() )
    {
      // map the super type as declared in type
      superType = getTypes().asSuper( type, superType.tsym );
      findAllInterfaces( superType, seen, result, excludeInternal );
    }
  }

  private static Type getUnderlyingType( Type type )
  {
    return isJava8()
      ? type.unannotatedType()
      : (Type)ReflectUtil.method( type, "stripMetadata" ).invoke();
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

  private void addAnnotation( Symbol sym, @SuppressWarnings( "SameParameterValue" ) Class<? extends Annotation> annoClass )
  {
    ClassSymbol annoSym = IDynamicJdk.instance().getTypeElement( _context,
      getCompilationUnit(), annoClass.getTypeName() );
    if( annoSym != null ) // annoSym can be null if processing an extension class's extended class e.g., java.lang.Object can't see an annotation in a manifold package
    {
      Attribute.Compound anno = new Attribute.Compound( annoSym.type, List.nil() );
      sym.appendAttributes( List.of( anno ) );
    }
  }

  static Attribute.Compound getAnnotationMirror( Symbol sym, Class<? extends Annotation> annoClass )
  {
    if( sym == null )
    {
      // anon class?
      return null;
    }

    for( Attribute.Compound anno : sym.getAnnotationMirrors() )
    {
      if( annoClass.getTypeName().equals( anno.type.tsym.getQualifiedName().toString() ) )
      {
        return anno;
      }
    }
    return null;
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

  private JCTree.JCExpression memberAccess( TreeMaker make, String path )
  {
    return memberAccess( make, path.split( "\\." ) );
  }

  private JCTree.JCExpression memberAccess( TreeMaker make, String... components )
  {
    Names names = Names.instance( getContext() );
    JCTree.JCExpression expr = make.Ident( names.fromString( ( components[0] ) ) );
    for( int i = 1; i < components.length; i++ )
    {
      expr = make.Select( expr, names.fromString( components[i] ) );
    }
    return expr;
  }

  private Symbol.ClassSymbol getRtClassSym( Class cls )
  {
    Symbol.ClassSymbol sym = IDynamicJdk.instance().getTypeElement( getContext(), getCompilationUnit(), cls.getCanonicalName() );
    if( sym == null )
    {
      sym = JavacElements.instance( getContext() ).getTypeElement( cls.getCanonicalName() );
    }
    return sym;
  }

  public static Symbol.MethodSymbol resolveMethod( Context context, CompilationUnitTree compUnit, JCDiagnostic.DiagnosticPosition pos, Name name, Type qual, List<Type> args )
  {
    return resolveMethod( pos, context, (JCTree.JCCompilationUnit)compUnit, name, qual, args );
  }

  private static Symbol.MethodSymbol resolveMethod( JCDiagnostic.DiagnosticPosition pos, Context ctx, JCTree.JCCompilationUnit compUnit, Name name, Type qual, List<Type> args )
  {
    Resolve rs = Resolve.instance( ctx );
    AttrContext attrContext = new AttrContext();
    Env<AttrContext> env = new AttrContextEnv( pos.getTree(), attrContext );
    env.toplevel = compUnit;
    return rs.resolveInternalMethod( pos, env, qual, name, args, null );
  }

  private Symbol.VarSymbol resolveField( JCDiagnostic.DiagnosticPosition pos, Context ctx, Name name, Type qual, JCClassDecl classPos )
  {
    Resolve rs = Resolve.instance( ctx );
    AttrContext attrContext = new AttrContext();
    Env<AttrContext> env = new AttrContextEnv( pos.getTree(), attrContext );
    env.toplevel = (JCTree.JCCompilationUnit)getCompilationUnit();
    env.enclClass = classPos;
    return rs.resolveInternalField( pos, env, qual, name );
  }

  /**
   * Sorts interfaces by type assignability where sub-interfaces precede super-interfaces.
   * This order ensures the overriding method trumps the overridden method involving covariant
   * return types. Where the delegating class must implement the more specific return type.
   */
  private void sortInterfaces( java.util.List<? extends Type> interfaces )
  {
    sortInterfaces( interfaces, true );
  }
  private void sortInterfaces( java.util.List<? extends Type> interfaces, boolean subFirst )
  {
    interfaces.sort( (t1, t2) -> {
      Type et2 = erasure( t2 );
      Type et1 = erasure( t1 );
      if( isSameType( et1, et2 ) )
      {
        return 0;
      }
      return (subFirst
             ? getTypes().isAssignable( et1, et2 )
             : getTypes().isAssignable( et2, et1 ))
        ? -1
        : subFirst
          ? getTypes().isAssignable( et2, et1 ) ? 1 : 0
          : getTypes().isAssignable( et1, et2 ) ? 1 : 0;
    } );
  }
  private List<Type> sortInterfaces( List<Type> interfaces )
  {
    ArrayList<Type> sorted = new ArrayList<>( interfaces );
    sortInterfaces( sorted );
    return List.from( sorted );
  }

  private static boolean isPartClass( Symbol sym )
  {
    Attribute.Compound partAnno = getAnnotationMirror( sym, part.class );
    return partAnno != null;
  }

  private JCMethodDecl getEnclosingMethod( Tree tree )
  {
    Tree parent = getParent( tree );
    if( parent == null )
    {
      return null;
    }
    if( parent instanceof JCMethodDecl )
    {
      return (JCMethodDecl)parent;
    }
    return getEnclosingMethod( parent );
  }

  private static boolean isInPartClass( Symbol sym )
  {
    if( sym == null )
    {
      // anon class
      return false;
    }

    Attribute.Compound partAnno = getAnnotationMirror( sym, part.class );
    if( partAnno != null )
    {
      return true;
    }

    Symbol owner = sym.owner;
    return owner != null && owner != sym && isInPartClass( owner );
  }

  private class ClassInfo
  {
    private final JCClassDecl _classDecl;
    private ArrayList<ClassType> _interfaces;
    private final Map<JCVariableDecl, LinkInfo> _linkInfos;
    private final Set<NamedMethodType> _defaultMethodForwarders;

    ClassInfo( JCClassDecl classDecl )
    {
      _classDecl = classDecl;
      _linkInfos = new HashMap<>();
      _defaultMethodForwarders = new HashSet<>();
    }

    public ArrayList<ClassType> getInterfaces()
    {
      if( _interfaces == null )
      {
        ArrayList<ClassType> result = new ArrayList<>();
        findAllInterfaces( _classDecl.sym.type, new HashSet<>(), result );
        _interfaces = result;
      }
      return _interfaces;
    }

    boolean hasLinks()
    {
      return !_linkInfos.isEmpty();
    }

    Map<JCVariableDecl, LinkInfo> getLinks()
    {
      return _linkInfos;
    }

    public void addDefaultMethodForwarder( NamedMethodType namedMt )
    {
      _defaultMethodForwarders.add( namedMt );
    }
    public Set<NamedMethodType> getDefaultMethodForwarders()
    {
      return _defaultMethodForwarders;
    }
  }

  private class LinkInfo
  {
    private final JCVariableDecl _linkField;

    private final ArrayList<JCMethodDecl> _generatedMethods;
    private final Map<Name, Set<NamedMethodType>> _methodTypes;
    private final Type _interface;
    private final Set<ClassType> _shared;
    private final Set<ClassType> _provided;

    LinkInfo( JCVariableDecl linkField, Type linkedInterface, Set<ClassType> shared )
    {
      _linkField = linkField;
      _generatedMethods = new ArrayList<>();
      _methodTypes = new HashMap<>();
      _interface = linkedInterface;
      _shared = shared;
      _provided = new HashSet<>();
    }

    public JCVariableDecl getLinkField()
    {
      return _linkField;
    }

    public Collection<JCMethodDecl> getGeneratedMethods()
    {
      return _generatedMethods;
    }

    void addGeneratedMethod( JCMethodDecl methodDecl )
    {
      _generatedMethods.add( methodDecl );
    }

    public Type getInterface()
    {
      return _interface;
    }
    public List<Type> getInterfaces()
    {
      return interfaceClosure( getInterface() );
    }

    public Map<Name, Set<NamedMethodType>> getMethodTypes()
    {
      return _methodTypes;
    }
    void addMethodType( MethodSymbol m, Type mt )
    {
      if( hasMethodType( m.name, mt ) )
      {
        throw new IllegalStateException();
      }

      Set<NamedMethodType> methodTypes = _methodTypes.computeIfAbsent( m.name, k -> new HashSet<>() );
      methodTypes.add( new NamedMethodType( m, mt ) );
    }
    boolean hasMethodType( Name name, Type mt )
    {
      Set<NamedMethodType> methodTypes = _methodTypes.computeIfAbsent( name, k -> new HashSet<>() );
      return methodTypes.stream().anyMatch( m -> getTypes().hasSameArgs( m.getType(), mt ) );
    }

    public boolean shares( ClassType iface )
    {
      return _shared.stream().anyMatch( t -> isSameType( t, erasure( iface ) ) );
    }
    public boolean sharesTransitive( ClassType iface )
    {
      return _shared.stream().anyMatch( t -> isSubtype( t, erasure( iface ) ) );
    }

    public void provided( ClassType iface )
    {
      _provided.add( iface );
//      LinkFieldData.instance( getContext() )
//        .putProvided( _linkField.sym, getTypes().erasure( iface ) );
    }
  }

  private class NamedMethodType
  {
    private final MethodSymbol _m;
    private final Type _type;

    public NamedMethodType( MethodSymbol m, Type type )
    {
      _m = m;
      _type = type;
    }

    public MethodSymbol getMethodSymbol()
    {
      return _m;
    }

    public Name getName()
    {
      return _m.name;
    }

    public Type getType()
    {
      return _type;
    }

    @Override
    public boolean equals( Object o )
    {
      if( this == o ) return true;
      if( o == null || getClass() != o.getClass() ) return false;
      NamedMethodType that = (NamedMethodType)o;
      return Objects.equals( _m.name, that._m.name ) && getTypes().hasSameArgs( _type, that._type );
    }

    @Override
    public int hashCode()
    {
      return Objects.hash( _m.name );
    }
  }
}
