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

package manifold.ext.delegation;

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
import manifold.ext.delegation.rt.api.DelegationLinkageError;
import manifold.ext.delegation.rt.api.internal;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;
import manifold.ext.delegation.rt.internal.$PartClass;
import manifold.ext.rt.ExtensionMethod;
import manifold.ext.rt.api.Structural;
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
import static java.lang.reflect.Modifier.*;
import static manifold.ext.delegation.DelegationIssueMsg.*;
import static manifold.ext.delegation.Util.getAnnotation;
import static manifold.util.JreUtil.isJava8;

public class DelegationProcessor implements ICompilerComponent, TaskListener
{
  private static final String LINKED_INTERFACES_FIELD = "$LINK_SCOPE_";
  private static final String LINK_PART_TO_SELF = "$linkPartToSelf";
  private static final String SELVES = "$selves";

  private BasicJavacTask _javacTask;
  private Context _context;
  private Stack<ClassInfo> _classInfoStack;
  private Stack<JCClassDecl> _classDeclStack;
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
    _classInfoStack = new Stack<>();
    _classDeclStack = new Stack<>();
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
    // Properties must be processed before DelegationProcessor so that manifold-props is fully supported with manifold-delegation
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
    Type partType = getTypes().erasure( partClass );
    return _classToInterfaceToIndex.computeIfAbsent( partType.tsym.getQualifiedName(), __ -> {
      ArrayList<ClassType> result = new ArrayList<>();
      findAllInterfaces( partType, new HashSet<>(), result );
      Map<Name, Integer> map = new HashMap<>();
      for( int i = 0; i < result.size(); i++ )
      {
        ClassType t = result.get( i );
        map.put( getTypes().erasure( t ).tsym.getQualifiedName(), i );
      }
      return map;
    } ).get( getTypes().erasure( iface ).tsym.getQualifiedName() );
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
     * StudentPart (from the TA sample code) has a $linkPartToSelf method that looks like this:
     * <pre><code>
     *   public void $linkPartToSelf(Object root, Class[] linkScope) {
     *     for(Class linkIface : linkScope) {
     *       if (Student.class == linkIface) {
     *         if($selves[0] == root) reportCycle(this, root, Student.class);
     *         $selves[0] = root;
     *         continue;
     *       }
     *       if (Person.class == linkIface) {
     *         if($selves[1] == root) reportCycle(this, root, Person.class);
     *         $selves[1] = root;
     *         continue;
     *       }
     *       throw new DelegationLinkageError("Unimplemented linked interface: " + linkIface);
     *     }
     *     // recurse through the fields of this class that are linked to `@part` classes
     *     Class[] root_personIntersection = Internal.intersect($LINK_SCOPE__person, linkScope);
     *     if (root_personIntersection.length != 0 && this._person instanceof .PartClass) {
     *         ((.PartClass)this._person).$linkPartToSelf(root, root_personIntersection);
     *     }
     *   }
     * </code></pre>
     */
    private void addLinkPartToSelfMethod()
    {
      ClassInfo ci = _classInfoStack.peek();
      JCClassDecl classDecl = ci._classDecl;

      TreeMaker make = getTreeMaker();
      make.pos = classDecl.pos;

      // Method name & modifiers
      JCModifiers access = make.Modifiers( PUBLIC /*| Flags.BRIDGE*/ );
      Names names = getNames();
      Name methName = names.fromString( LINK_PART_TO_SELF );

      // Params
      List<JCVariableDecl> params = List.nil();

      Name rootName = names.fromString( "root" );
      JCExpression rootType = make.Type( getSymtab().objectType );
      JCVariableDecl rootParam = make.VarDef( make.Modifiers( FINAL | Flags.PARAMETER ), rootName, rootType, null );
      params = params.append( rootParam );

      Name linkScopeName = names.fromString( "linkScope" );
      Type classType = getTypes().erasure( getSymtab().classType );
      JCExpression linkScopeType = make.Type( getTypes().makeArrayType( classType ) );
      JCVariableDecl linkScopeParam = make.VarDef( make.Modifiers( FINAL | Flags.PARAMETER ), linkScopeName, linkScopeType, null );
      params = params.append( linkScopeParam );

      // Return type
      JCExpression resType = make.Type( getSymtab().voidType );

      // Code
      List<JCStatement> loopStmts = List.nil();
      Name linkIfaceName = names.fromString( "linkIface" );
      for( ClassType iface : ci.getInterfaces() )
      {
        int ifaceIndex = indexOfInterface( classDecl.sym.type, iface );
        JCIf ifStmt = make.If(
          make.Binary( Tag.EQ, make.ClassLiteral( iface ), make.Ident( linkIfaceName ) ),
          make.Block( 0, List.of( make.If(
                                    make.Binary( Tag.EQ,
                                                 make.Indexed( make.Ident( names.fromString( SELVES ) ),
                                                               make.Literal( TypeTag.INT, ifaceIndex ) ),
                                                 make.Ident( rootName ) ),
                                    make.Exec( make.Apply( List.nil(), make.Select( make.Ident( names.fromString( "Internal" ) ), names.fromString( "reportCycle" ) ),
                                                           List.of( make.This( classDecl.sym.type ), make.Ident( rootName ), make.ClassLiteral( iface ) ) ) ),
                                    null ),
                                  make.Exec( make.Assign( make.Indexed( make.Ident( names.fromString( SELVES ) ),
                                                                        make.Literal( TypeTag.INT, ifaceIndex ) ),
                                                          make.Ident( rootName ) ) ),
                                  make.Continue( null ) ) ),
          null );
        loopStmts = loopStmts.append( ifStmt );
      }
      JCThrow throwDelegationLinkageError = make.Throw(
        make.NewClass( null, null, memberAccess( make, DelegationLinkageError.class.getTypeName() ),
                       List.of( make.Binary( Tag.PLUS, make.Literal( "Unimplemented linked interface: " ), make.Ident( linkIfaceName ) ) ), null ) );
      loopStmts = loopStmts.append( throwDelegationLinkageError );
      JCEnhancedForLoop assignSelves = make.ForeachLoop( make.VarDef( make.Modifiers( FINAL ), linkIfaceName, make.Type( classType ), null ),
                                                         make.Ident( linkScopeName ), make.Block( 0, loopStmts ) );
      List<JCStatement> methodBody = List.of( assignSelves );
      for( Map.Entry<JCVariableDecl, LinkInfo> link : ci.getLinks().entrySet() )
      {
        JCVariableDecl field = link.getKey();
        Name fieldName = field.name;

        JCMethodInvocation rootIntersection = make.Apply( List.nil(), make.Select( make.Ident( names.fromString( "Internal" ) ), names.fromString( "intersect" ) ),
                                                          List.of( make.Ident( names.fromString( LINKED_INTERFACES_FIELD + fieldName ) ), make.Ident( linkScopeName ) ) );
        Type.ArrayType arrayOfClassesType = getTypes().makeArrayType( getTypes().erasure( getSymtab().classType ) );
        Name rootIntersectionName = names.fromString( "root" + fieldName + "Intersection" );
        methodBody = methodBody.append(
          make.VarDef( make.Modifiers( FINAL ), rootIntersectionName, make.Type( arrayOfClassesType ), rootIntersection ) );
        methodBody = methodBody.append(
          make.If( make.Binary( Tag.AND,
                                make.Binary( Tag.NE,
                                             make.Select( make.Ident( rootIntersectionName ), names.fromString( "length" ) ),
                                             make.Literal( TypeTag.INT, 0 ) ),
                                make.TypeTest( make.Ident( fieldName ), memberAccess( make, $PartClass.class.getTypeName() ) ) ),
                   make.Exec( make.Apply( List.nil(), make.Select( make.TypeCast( memberAccess( make, $PartClass.class.getTypeName() ), make.Ident( fieldName ) ), methName ),
                                          List.of( make.Ident( rootName ), make.Ident( rootIntersectionName ) ) ) ),
                   null ) );
      }
      Type superclass = classDecl.sym.getSuperclass();
      if( superclass != null && !getTypes().isSameType( superclass, getSymtab().objectType ) )
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
        Type.ArrayType arrayOfClassesType = getTypes().makeArrayType( getTypes().erasure( getSymtab().classType ) );
        ArrayList<ClassType> interfaces = li.getInterfaces();
        List<JCExpression> interfaceTypes = List.from( interfaces.stream().map( t -> make.ClassLiteral( getTypes().erasure( t ) ) ).collect( Collectors.toList() ) );
        JCNewArray interfaceArray = make.NewArray( make.Type( getTypes().erasure( getSymtab().classType ) ), List.nil(), interfaceTypes );
        interfaceArray.type = arrayOfClassesType;

        addWiringField( ci._classDecl, Flags.STATIC, LINKED_INTERFACES_FIELD + field.name, interfaceArray.type, interfaceArray );
      }
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
      Type owner = getTypes().erasure( m.owner.type );
      for( LinkInfo li : _classInfoStack.peek().getLinks().values() )
      {
        for( ClassType claimed : li.getInterfaces() )
        {
          if( getTypes().isSubtype( getTypes().erasure( claimed ), owner ) )
          {
            // m is part of a delegated interface, it must be forwarded to the delegate,
            // which is the default generated behavior for a composite
            return true;
          }
        }
      }
      return false;
    }


    private void findDefaultMethodsToForward( JCClassDecl classDecl, Type iface, Set<Type> seen, ArrayList<MethodSymbol> result )
    {
      if( seen.stream().anyMatch( t -> getTypes().isSameType( t, iface ) ) )
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
      if( superclass == null || getTypes().isSameType( superclass, getSymtab().objectType ) )
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
      JCModifiers access = make.Modifiers( PRIVATE | mods /*| ACC_SYNTHETIC*/ );
      Names names = getNames();
      Name name = names.fromString( fieldName );
      JCExpression type = make.Type( fieldType );

      // the field
      JCVariableDecl fieldDecl = make.VarDef( access, name, type, initExpr );

      // add to AST
      classDecl.defs = classDecl.defs.append( fieldDecl );

      // enter symbol as class member
      memberEnter( fieldDecl, classDecl );
    }

    private void processMethodOverlap( ClassInfo classInfo )
    {
      for( Map.Entry<JCVariableDecl, LinkInfo> entry : classInfo.getLinks().entrySet() )
      {
        LinkInfo li = entry.getValue();
        for( ClassType iface : li.getInterfaces() )
        {
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
              DelegationIssueMsg.MSG_METHOD_OVERLAP.get( mt.getName(), fieldNames ) );

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
        !getTypes().isSameType( getSymtab().objectType, existingMethod.owner.type ) )
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
          if( li.getInterfaces().stream().anyMatch( e -> getTypes().isSameType( e, iface ) ) )
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

          for( LinkInfo li: lis )
          {
            StringBuilder overlappingLinks = new StringBuilder();
            lis.stream()
              .filter( l -> l != li )
              .forEach( l -> overlappingLinks.append( overlappingLinks.length() > 0 ? ", " : "" ).append( l._linkField.name ) );

            if( !li.shares( iface ) )
            {
              if( !isInterfaceShared )
              {
                reportError( li.getLinkField(),
                  DelegationIssueMsg.MSG_INTERFACE_OVERLAP.get( iface.tsym.getSimpleName(), overlappingLinks ) );
              }

              // remove the overlap interface from the link, only the sharing link provides it
              li.getInterfaces().remove( iface );
            }
          }
        }
      }
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
          DelegationIssueMsg.MSG_MULTIPLE_SHARING.get( iface.tsym.getSimpleName(), fieldNames ) ) );
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
          classDecl.sym.members().remove( ((JCMethodDecl)def).sym );
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
            String sig = m.name + getTypes().erasure( m.type ).toString();
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
      List.of( make.Literal( "Abstract method '" + m.name + "' called on unlinked part" ) ), null );

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
      ArrayList<ClassType> interfaces = new ArrayList<>();
      ArrayList<ClassType> shared = new ArrayList<>();
      ArrayList<ClassType> fromAnno = new ArrayList<>();
      getInterfacesFromLinkAnno( linkAnno, fromAnno, shared );
      if( fromAnno.isEmpty() )
      {
        // derive interfaces from field's declared type

        Type fieldType = field.sym.type;
        interfaces.addAll( getCommonInterfaces( ci.getInterfaces(), fieldType, false, true ) );

        if( fieldType.isInterface() && ci.getInterfaces().stream().noneMatch( t -> getTypes().isSameType( t, fieldType ) ) )
        {
          // if the linked field's type is an interface, the delegating class must implement it
          reportError( linkAnno, MSG_DELEGATING_CLASS_DOES_NOT_IMPLEMENT.get( ci._classDecl.name, fieldType, field.name ) );
        }
        else if( interfaces.isEmpty() )
        {
          // if the linked field's type is *not* an interface, the interfaces the type has in common with the delegating class must be non-empty
          reportError( field.getType(), MSG_NO_INTERFACES.get( field.sym.type, ci._classDecl.sym.type ) );
        }
        else if( (fieldType.tsym.flags_field & ABSTRACT) == 0 ) // abstract classes and interfaces are preferred
        {
          ArrayList<ClassType> minimizedInterfaces = minimizeInterfaces( interfaces );
          if( minimizedInterfaces.size() == 1 )
          {
            reportWarning( field.getType(), MSG_INTERFACE_LINK_FIELD_TYPE_EXPECTED_1.get(
              field.sym.type.tsym.getSimpleName(), minimizedInterfaces.get( 0 ).tsym.getSimpleName(),
              minimizedInterfaces.get( 0 ).tsym.getSimpleName() + ".class" ) );
          }
          else
          {
            reportWarning( field.getType(), MSG_INTERFACE_LINK_FIELD_TYPE_EXPECTED_N.get(
              field.sym.type.tsym.getSimpleName(),
              minimizedInterfaces.stream().map( t -> t.tsym.getSimpleName() + ".class" )
                .collect( Collectors.joining( ", " ) ) ) );
          }
        }
      }
      else
      {
        // derive interfaces from @link provided interfaces

        for( int i = 0; i < fromAnno.size(); i++ )
        {
          ClassType iface = fromAnno.get( i );
          Set<ClassType> commonInterfaces = getCommonInterfaces( ci.getInterfaces(), iface, true );
          interfaces.addAll( commonInterfaces );

          if( ci.getInterfaces().stream()
            .map( t -> getTypes().erasure( t ) )
            .noneMatch( t -> getTypes().isSameType( t, getTypes().erasure( iface ) ) ) )
          {
            // delegating class must implement all the interfaces specified in the link annotation
            reportError( linkAnno.getArguments().get( i ), MSG_DELEGATING_CLASS_DOES_NOT_IMPLEMENT.get( ci._classDecl.name, iface.tsym.getSimpleName(), field.name ) );
          }
        }
        checkFieldTypeSatisfiesAnnoTypes( field, interfaces );
      }

      removeDups( interfaces );

      if( !shared.isEmpty() )
      {
        // shared links must be final
        field.getModifiers().flags |= FINAL;
      }

      ci.getLinks().put( field, new LinkInfo( field, interfaces, shared ) );
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
          if( !getTypes().isSameType( ti, tj ) && getTypes().isSubtype( tj, ti ) )
          {
            continue outer; // ti is redundant
          }
        }
        result.add( ti );
      }

      return new ArrayList<>( result );
    }

    private void checkFieldTypeSatisfiesAnnoTypes( JCVariableDecl field, ArrayList<ClassType> interfaces )
    {
      Types types = getTypes();
      for( ClassType t: interfaces )
      {
        if( !types.isAssignable( field.sym.type, t ) )
        {
          JCTree typeTree = field.getType();
          reportWarning( typeTree == null ? field : typeTree, MSG_FIELD_TYPE_NOT_ASSIGNABLE_TO.get(
            field.sym.type.tsym.getQualifiedName(), t.tsym.getQualifiedName() ) );
        }
      }
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

      // Forward call statement
      JCExpression link = make.Ident( linkField );
      JCTree.JCFieldAccess forwardRef = IDynamicJdk.instance().Select( make, link, namedMt.getMethodSymbol() );
      forwardRef.type = mt.getReturnType();
      java.util.List<JCExpression> args = params.stream().map( p -> make.Ident( p.name ) ).collect( Collectors.toList() );
      JCTree.JCMethodInvocation forwardCall = make.Apply( List.nil(), forwardRef, List.from( args ) );
      forwardCall.type = forwardRef.type;
      ((JCTree.JCFieldAccess)forwardCall.meth).sym = namedMt.getMethodSymbol();

      JCStatement forwardStmt;
      if( getTypes().isSameType( mt.getReturnType(), getSymtab().voidType ) )
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

  private void getInterfacesFromLinkAnno( JCAnnotation linkAnno, ArrayList<ClassType> interfaces, ArrayList<ClassType> share )
  {
    List<JCExpression> args = linkAnno.getArguments();
    if( args.isEmpty() )
    {
      return;
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
      else if( argSym.name.toString().equals( "value" ) )
      {
        processClassType( interfaces, value, args.get( i ) );
      }
      else
      {
        throw new IllegalStateException();
      }
      i++;
    }
  }

  private void processClassType( ArrayList<ClassType> share, Attribute value, JCExpression expr )
  {
    if( value instanceof Attribute.Class )
    {
      processClassType( (Attribute.Class) value, share, expr );
    }
    if( value instanceof Attribute.Array )
    {
      for( Attribute cls : ((Attribute.Array) value).values )
      {
        processClassType( (Attribute.Class)cls, share, expr );
      }
    }
  }

  private void processClassType( Attribute.Class value, ArrayList<ClassType> interfaces, JCExpression location )
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

  private Set<ClassType> getCommonInterfaces( ArrayList<ClassType> ci, Type fieldType, boolean erasure )
  {
    return getCommonInterfaces( ci, fieldType, erasure, false );
  }
  private Set<ClassType> getCommonInterfaces( ArrayList<ClassType> ci, Type fieldType, boolean erasure, boolean excludeInternal  )
  {
    ArrayList<ClassType> linkFieldInterfaces = new ArrayList<>();
    findAllInterfaces( fieldType, new HashSet<>(), linkFieldInterfaces, excludeInternal );

    if( fieldType.isInterface() && Util.getAnnotationMirror( fieldType.tsym, Structural.class ) != null )
    {
      // A structural interface is assumed to be fully mapped onto the declaring class.
      // Note, structural interfaces work only with forwarding, not with parts
      return new HashSet<>( linkFieldInterfaces );
    }

    Types types = getTypes();
    if( erasure )
    {
      return ci.stream()
        .filter( i1 -> linkFieldInterfaces.stream()
          .anyMatch( i2 -> types.isSameType( types.erasure( i1 ), types.erasure( i2 ) ) ) )
        .collect( Collectors.toSet() );
    }
    return ci.stream()
      .filter( i1 -> linkFieldInterfaces.stream()
        .anyMatch( i2 -> types.isSameType( i1, i2 ) ) )
      .collect( Collectors.toSet() );
  }

  // add $PartClass to implements clause
  private class Enter_Start extends TreeTranslator
  {
    @Override
    public void visitClassDef( JCClassDecl classDecl )
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

      generate$ImplClassShell( classDecl );

      super.visitClassDef( classDecl );
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
    }

    @Override
    public void visitSelect( JCFieldAccess tree )
    {
      super.visitSelect( tree );

      if( tree.toString().endsWith( ".this" ) )
      {
        replaceThis_Explicit( tree );
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
        else
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
        else
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
        else
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
        else
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
// escaping through Object is prohibited, force an @internal marker interface or other strategy
//          else if( !types.isSameType( getSymtab().objectType, paramType ) )
//          {
            reportError( tree, MSG_PART_THIS_NONINTERFACE_USE.get() );
//          }
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
      for( Type sup : getTypes().closure( owner.type ) )
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
      for( Attribute.Compound attr : m.getRawTypeAttributes() )
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

      Pair<JCClassDecl, Type> enclClass_Iface = findInterfaceOfEnclosingTypeThatSymImplements( methSym );
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

    private Pair<JCClassDecl, Type> findInterfaceOfEnclosingTypeThatSymImplements( MethodSymbol sym )
    {
      for( int i = _classDeclStack.size()-1; i >= 0; i-- )
      {
        JCClassDecl classDecl = _classDeclStack.get( i );
        if( isPartClass( classDecl.sym ) )
        {
          ArrayList<ClassType> interfaces = new ArrayList<>();
          findAllInterfaces( classDecl.sym.type, new HashSet<>(), interfaces );
          for( ClassType iface : interfaces )
          {
            for( Symbol mm : IDynamicJdk.instance().getMembersByName( (ClassSymbol)iface.tsym, sym.name ) )
            {
              if( sym.overrides( mm, iface.tsym, getTypes(), false ) )
              {
                return new Pair<>( classDecl, iface );
              }
            }
          }
        }
      }
      return null;
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

      checkAbstract( linkField, rhs );

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

      ArrayList<ClassType> interfaces = getDelegatedInterfacesForWiring( linkField );
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

    private void checkAbstract( Symbol linkField, JCExpression rhs )
    {
      if( (rhs.type.tsym.flags_field & ABSTRACT) != 0 && !rhs.type.isInterface() &&
          !getTypes().isSameType( getTypes().erasure( rhs.type ), getTypes().erasure( linkField.type ) ) )
      {
        reportError( rhs, MSG_LINK_TYPE_MUST_MATCH_ABSTRACT_PART_TYPE.get( linkField.type.tsym.getQualifiedName(), rhs.type.tsym.getQualifiedName() ) );
      }
    }

    private void verifyLinkedDelegatedInterfacesAgainstDelegateType( ArrayList<ClassType> interfaces, JCExpression rhs )
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
    private ArrayList<ClassType> getDelegatedInterfacesForWiring( Symbol linkFieldSym )
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
        return new ArrayList<>();
      }

      ArrayList<ClassType> interfaces = new ArrayList<>();
      ArrayList<ClassType> shared = new ArrayList<>();
      ArrayList<ClassType> fromAnno = new ArrayList<>();
      getInterfacesFromLinkAnno( linkAnno, fromAnno, shared );
      ArrayList<ClassType> enclClassInterfaces = new ArrayList<>();
      findAllInterfaces( classDecl.sym.type, new HashSet<>(), enclClassInterfaces );
      if( fromAnno.isEmpty() )
      {
        // derive interfaces from field's declared type

        interfaces.addAll( getCommonInterfaces( enclClassInterfaces, linkFieldSym.type, false, true ) );
      }
      else
      {
        // derive interfaces from @link provided interfaces

        for( ClassType iface : fromAnno )
        {
          Set<ClassType> commonInterfaces = getCommonInterfaces( enclClassInterfaces, iface, true );
          interfaces.addAll( commonInterfaces );
          if( commonInterfaces.isEmpty() )
          {
            reportError( linkAnno, MSG_NO_INTERFACES.get( iface, classDecl.sym.type ) );
          }
        }
      }

      removeDups( interfaces );

      return interfaces;
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

  // hasSelf tests if `iface` is "owned": `selves[<index of `iface>] != this` means `iface` is dispatched through a delegating class
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

  private void findAllInterfaces( Type type, Set<Type> seen, ArrayList<ClassType> result )
  {
    findAllInterfaces( type, seen, result, false );
  }
  private void findAllInterfaces( Type type, Set<Type> seen, ArrayList<ClassType> result, boolean excludeInternal )
  {
    if( seen.stream().anyMatch( t -> getTypes().isSameType( t, type ) ) )
    {
      return;
    }
    seen.add( type );

    if( type.isInterface() && !isInterfaceExcluded( type ) )
    {
      if( result.stream()
        .noneMatch( e -> getTypes().isSameType( e, type ) ) )
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

  private void findAllInterfaces( Type type, Type superType, Set<Type> seen, ArrayList<ClassType> result )
  {
    findAllInterfaces( type, superType, seen, result, false );
  }
  private void findAllInterfaces( Type type, Type superType, Set<Type> seen, ArrayList<ClassType> result, boolean excludeInternal )
  {
    if( getTypes().isSameType( getSymtab().objectType, superType ) )
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
      Type et2 = getTypes().erasure( t2 );
      Type et1 = getTypes().erasure( t1 );
      if( getTypes().isSameType( et1, et2 ) )
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

  private static boolean isInPartClass( Symbol sym )
  {
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
    private final ArrayList<ClassType> _interfaces;
    private final ArrayList<ClassType> _shared;

    LinkInfo( JCVariableDecl linkField, ArrayList<ClassType> linkedInterfaces, ArrayList<ClassType> shared )
    {
      _linkField = linkField;
      _generatedMethods = new ArrayList<>();
      _methodTypes = new HashMap<>();
      _interfaces = new ArrayList<>( linkedInterfaces );
      sortInterfaces( _interfaces );
      _shared = shared;
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

    public ArrayList<ClassType> getInterfaces()
    {
      return _interfaces;
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
      return _shared.stream().anyMatch( t -> getTypes().isSameType( t, getTypes().erasure( iface ) ) );
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
