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
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import manifold.api.type.ICompilerComponent;
import manifold.api.util.JavacDiagnostic;
import manifold.ext.delegation.rt.RuntimeMethods;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;
import manifold.ext.delegation.rt.api.tags.enter_finish;
import manifold.ext.rt.api.Structural;
import manifold.internal.javac.*;
import manifold.rt.api.util.Stack;
import manifold.util.ReflectUtil;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

import static com.sun.tools.javac.code.Flags.FINAL;
import static java.lang.reflect.Modifier.*;
import static manifold.ext.delegation.DelegationIssueMsg.*;
import static manifold.ext.delegation.Util.getAnnotation;
import static manifold.ext.delegation.rt.RuntimeMethods.COVERED_FIELD;
import static manifold.ext.delegation.rt.RuntimeMethods.SELF_FIELD;
import static manifold.util.JreUtil.isJava8;

public class DelegationProcessor implements ICompilerComponent, TaskListener
{
  private BasicJavacTask _javacTask;
  private Context _context;
  private Stack<ClassInfo> _classInfoStack;
  private Stack<JCClassDecl> _classDeclStack;
  private TaskEvent _taskEvent;
  private ParentMap _parents;

  @Override
  public void init( BasicJavacTask javacTask, TypeProcessor typeProcessor )
  {
    _javacTask = javacTask;
    _context = _javacTask.getContext();
    _classInfoStack = new Stack<>();
    _classDeclStack = new Stack<>();
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
  public void started( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.ANALYZE )
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
          if( e.getKind() == TaskEvent.Kind.ANALYZE )
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
              ReflectUtil.method( MemberEnter.instance( getContext() ), "memberEnter", JCTree.class, Env.class )
                .invoke( methDecl, Enter.instance( getContext() ).getClassEnv( classDecl.sym ) );
            }
          }
        }
      }
      finally
      {
        _classInfoStack.pop();
      }
    }

    /**
     * Generate methods to override default interface methods. A generated override must forward the call to the default
     * method as if forwarded within $self. Since a default method implementation can call other methods in the interface,
     * and those interface methods can be overridden by linking classes, the default method must be called with @self as
     * the implementing class. This can only be done reflectively via method handles.
     */
    private void overrideDefaultInterfaceMethods( JCClassDecl classDecl )
    {
      // foreach default interface method,
      //   override the method
      //     if( $self != null && $self links the interface of the default method to classDecl )
      //       ReflectUtil.invokeDefault
      //     else
      //       Iface.super.method()
      for( JCExpression expr: classDecl.implementing )
      {
        Type exprType = expr.type;
        if( exprType.isErroneous() || !exprType.isInterface() )
        {
          continue;
        }
        //noinspection UnnecessaryLocalVariable
        Type iface = exprType;
        ArrayList<MethodSymbol> defaultMethods = new ArrayList<>();
        findDefaultMethodsToForward( classDecl, iface, new HashSet<>(), defaultMethods );
        for( MethodSymbol m : defaultMethods )
        {
          Type type = getTypes().memberType( iface, m );
          if( type instanceof MethodType )
          {
            generateDefaultMethodForwarder( classDecl, (ClassType)iface, new NamedMethodType( m, type ) );
          }
        }
      }
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
        m -> m instanceof MethodSymbol && ((MethodSymbol)m).isDefault() );
      defaultMethods.forEach( m -> {
        MethodSymbol implSym = ((MethodSymbol)m).binaryImplementation( classDecl.sym, getTypes() );
        if( implSym == null )
        {
          result.add( (MethodSymbol)m );
        }
      } );
      ((ClassSymbol)iface.tsym).getInterfaces().forEach( t -> findDefaultMethodsToForward( classDecl, t, seen, result ) );
    }

    private void generateDefaultMethodForwarder( JCClassDecl classDecl, ClassType iface, NamedMethodType namedMt )
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


      //     if( RuntimeMethods.linksInterfaceTo( $self ) )
      //       RuntimeMethods.invokeDefault
      //     else
      //       Iface.super.method()


      Symtab symtab = getSymtab();

      // RuntimeMethods.linksInterfaceTo( $self )

      JCTree.JCMethodInvocation linksInterfaceToCall = make.Apply( List.nil(),
        memberAccess( make, RuntimeMethods.class.getTypeName() + ".linksInterfaceTo" ),
        List.of( make.ClassLiteral( iface ), make.Ident( names.fromString( SELF_FIELD ) ), make.This( classDecl.sym.type ) ) );

      // ReflectUtil.invokeDefaultAsSelf

      ArrayList<JCExpression> paramTs = parameterTypes.stream().map( t -> make.ClassLiteral( t ) ).collect( Collectors.toCollection( () -> new ArrayList<>() ) );
      JCNewArray paramTypesArray = make.NewArray( make.Type( getTypes().erasure( symtab.classType ) ), List.nil(), List.from( paramTs ) );
      ArrayList<JCIdent> argIdents = params.stream().map( p -> make.Ident( p.name ) ).collect( Collectors.toCollection( () -> new ArrayList<>() ) );
      JCNewArray argsArray = make.NewArray( make.Type( symtab.objectType ), List.nil(), List.from( argIdents) );
      List<JCExpression> theArgs = List.of( make.Ident( names.fromString( SELF_FIELD ) ), make.ClassLiteral( iface ), make.Literal( namedMt.getName().toString() ),
        paramTypesArray, argsArray );
      JCTree.JCMethodInvocation linkPartCall = make.Apply( List.nil(), memberAccess( make, RuntimeMethods.class.getTypeName() + ".invokeDefault" ), theArgs );

      JCTypeCast castExpr = make.TypeCast( mt.getReturnType(), linkPartCall );
      JCStatement invokeDefaultAsSelfStmt;
      if( getTypes().isSameType( mt.getReturnType(), getSymtab().voidType ) )
      {
        invokeDefaultAsSelfStmt = make.Exec( castExpr );
      }
      else
      {
        invokeDefaultAsSelfStmt = make.Return( castExpr );
      }


      // Iface.super.method()

      JCTree.JCFieldAccess forwardRef = (JCFieldAccess)make.Select( make.Select( make.Type( iface ), names._super ),
        namedMt.getMethodSymbol() );
      forwardRef.type = mt.getReturnType();
      java.util.List<JCExpression> args = params.stream().map( p -> make.Ident( p.name ) ).collect( Collectors.toList() );
      JCTree.JCMethodInvocation forwardCall = make.Apply( List.nil(), forwardRef, List.from( args ) );
      forwardCall.type = forwardRef.type;
      ((JCTree.JCFieldAccess)forwardCall.meth).sym = namedMt.getMethodSymbol();

      JCStatement invokeSuperStmt;
      if( getTypes().isSameType( mt.getReturnType(), getSymtab().voidType ) )
      {
        invokeSuperStmt = make.Exec( forwardCall );
      }
      else
      {
        invokeSuperStmt = make.Return( forwardCall );
      }

      // if( linksInterfaceToCall() )
      //   invokeDefaultAsSelfStmt
      // else
      //   invokeSuperStmt
      JCIf ifStmt = make.If( linksInterfaceToCall, invokeDefaultAsSelfStmt, invokeSuperStmt );

      JCBlock block = make.Block( 0, List.of( ifStmt ) );

      JCMethodDecl defaultMethodForwarder = make.MethodDef( access, name, resType, typeParams, List.from( params ), thrown, block, null );

      ArrayList<JCTree> newDefs = new ArrayList<>( classDecl.defs );
      newDefs.add( defaultMethodForwarder );
      classDecl.defs = List.from( newDefs );

      ReflectUtil.method( MemberEnter.instance( getContext() ), "memberEnter", JCTree.class, Env.class )
        .invoke( defaultMethodForwarder, Enter.instance( getContext() ).getClassEnv( classDecl.sym ) );
    }

    private void processPartClass( JCClassDecl classDecl )
    {
      if( !isPartClass( classDecl.sym ) )
      {
        return;
      }

      checkSuperclass( classDecl );

      if( getAnnotationMirror( classDecl.sym, enter_finish.class ) != null )
      {
        // already processed, probably an annotation processing round
        return;
      }
      addAnnotation( classDecl.sym, enter_finish.class );

      addSelfField( classDecl );
      addCoveredField( classDecl );
      overrideDefaultInterfaceMethods( classDecl );
    }

    private void checkSuperclass( JCClassDecl classDecl )
    {
      Type superclass = classDecl.sym.getSuperclass();
      if( classDecl.getExtendsClause() != null &&
        superclass != null && getAnnotationMirror( superclass.tsym, part.class ) == null )
      {
        reportError( classDecl.getExtendsClause(), MSG_SUPERCLASS_NOT_PART.get() );
      }
    }

    private void addSelfField( JCClassDecl classDecl )
    {
      addWiringField( classDecl, SELF_FIELD, getSymtab().objectType );
    }

    // This field is a performance measure which helps with 'this' substitution. Basically, we can skip the work of determining
    // whether the interface of the context of 'this' is linked from $self to 'this'. Because, if we know all 'this' interfaces
    // are linked by $self, we know the context interface is linked to 'this'. (we determine statically that the interface
    // is implemented by 'this')
    private void addCoveredField( JCClassDecl classDecl )
    {
      addWiringField( classDecl, COVERED_FIELD, getSymtab().booleanType );
    }

    private void addWiringField( JCClassDecl classDecl, String fieldName, Type fieldType )
    {
      TreeMaker make = getTreeMaker();
      make.pos = classDecl.pos;

      // field name & modifiers & type
      JCModifiers access = make.Modifiers( PRIVATE );
      Names names = getNames();
      Name name = names.fromString( fieldName );
      JCExpression type = make.Type( fieldType );

      // the field
      JCVariableDecl fieldDecl = make.VarDef( access, name, type, null );

      // add field def to class AST
      ArrayList<JCTree> newDefs = new ArrayList<>( classDecl.defs );
      newDefs.add( fieldDecl );
      classDecl.defs = List.from( newDefs );

      // define covered field and add it to the class symbol's members
      ReflectUtil.method( MemberEnter.instance( getContext() ), "memberEnter", JCTree.class, Env.class )
        .invoke( fieldDecl, Enter.instance( getContext() ).getClassEnv( classDecl.sym ) );
    }

    private void processMethodOverlap( ClassInfo classInfo )
    {
      for( Map.Entry<JCVariableDecl, LinkInfo> entry : classInfo.getLinks().entrySet() )
      {
        LinkInfo li = entry.getValue();
        for( ClassType iface : li.getInterfaces() )
        {
          Iterable<Symbol> methods = IDynamicJdk.instance().getMembers( (ClassSymbol)iface.tsym,
            m -> m instanceof MethodSymbol && !m.isStatic() );
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
      MethodSymbol existingMethod = m.binaryImplementation( classDecl.sym, getTypes() );
      if( existingMethod != null &&
        !getTypes().isSameType( getSymtab().objectType, existingMethod.owner.type ) )
      {
        // class already implements method
        return;
      }

      LinkInfo linkInfo = _classInfoStack.peek().getLinks().get( li._linkField );

      // Method type as a member of the delegating class
      Type emt = getTypes().memberType( classDecl.sym.type, m );
      Type csr = emt;
      while( csr instanceof Type.DelegatedType )
      {
        csr = ((Type.DelegatedType)csr).qtype;
      }
      if( linkInfo.hasMethodType( m.name, emt ) )
      {
        // already defined previously in this link
        return;
      }
      linkInfo.addMethodType( m, emt );
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

          StringBuilder fieldNames = new StringBuilder();
          lis.forEach( li -> fieldNames.append( fieldNames.length() > 0 ? ", " : "" ).append( li._linkField.name ) );
          for( LinkInfo li: lis )
          {
            if( !li.isShare() )
            {
              if( !isInterfaceShared )
              {
                reportWarning( li.getLinkField(),
                  DelegationIssueMsg.MSG_INTERFACE_OVERLAP.get( iface.tsym.getSimpleName(), fieldNames ) );
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
        .filter( li -> li.isShare() )
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

        if( getAnnotationMirror( varDecl.sym, enter_finish.class ) != null )
        {
          // already processed, probably an annotation processing round
          return;
        }
        addAnnotation( varDecl.sym, enter_finish.class );

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
      ArrayList<ClassType> fromAnno = new ArrayList<>();
      boolean share = getInterfacesFromLinkAnno( linkAnno, fromAnno );
      if( fromAnno.isEmpty() )
      {
        // derive interfaces from field's declared type

        interfaces.addAll( getCommonInterfaces( ci, field.sym.type ) );
        if( interfaces.isEmpty() )
        {
          reportError( field.getType(), MSG_NO_INTERFACES.get( field.sym.type, ci._classDecl.sym.type ) );
        }
      }
      else
      {
        // derive interfaces from @link provided interfaces

        for( ClassType iface : fromAnno )
        {
          Set<ClassType> commonInterfaces = getCommonInterfaces( ci, iface, true );
          interfaces.addAll( commonInterfaces );
          if( commonInterfaces.isEmpty() )
          {
            reportError( linkAnno, MSG_NO_INTERFACES.get( iface, ci._classDecl.sym.type ) );
          }
        }
        verifyFieldTypeSatisfiesAnnoTypes( field, interfaces );
      }

      removeDups( interfaces );

      if( share )
      {
        // shared links must be final
        field.getModifiers().flags |= FINAL;
      }
      
      ci.getLinks().put( field, new LinkInfo( field, interfaces, share ) );
    }

    private void verifyFieldTypeSatisfiesAnnoTypes( JCVariableDecl field, ArrayList<ClassType> interfaces )
    {
      Types types = getTypes();
      for( ClassType t: interfaces )
      {
        if( !types.isAssignable( field.sym.type, t ) )
        {
          JCTree typeTree = field.getType();
          reportError( typeTree == null ? field : typeTree, MSG_FIELD_TYPE_NOT_ASSIGNABLE_TO.get(
            field.sym.type.tsym.getQualifiedName(), t.tsym.getQualifiedName() ) );
        }
      }
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

    private boolean getInterfacesFromLinkAnno( JCAnnotation linkAnno, ArrayList<ClassType> interfaces )
    {
      List<JCExpression> args = linkAnno.getArguments();
      if( args.isEmpty() )
      {
        return false;
      }

      boolean share = false;
      Attribute.Compound annoValues = linkAnno.attribute.getValue();
      int i = 0;
      for( Map.Entry<MethodSymbol, Attribute> entry: annoValues.getElementValues().entrySet() )
      {
        MethodSymbol argSym = entry.getKey();
        Attribute value = entry.getValue();
        if( argSym.name.toString().equals( "share" ) )
        {
          share = (boolean)value.getValue();
        }
        else if( argSym.name.toString().equals( "value" ) )
        {
          if( value instanceof Attribute.Class )
          {
            processClassType( (Attribute.Class)value, interfaces, args.get( i ) );
          }
          if( value instanceof Attribute.Array )
          {
            for( Attribute cls : ((Attribute.Array)value).values )
            {
              processClassType( (Attribute.Class)cls, interfaces, args.get( i ) );
            }
          }
        }
        else
        {
          throw new IllegalStateException();
        }
        i++;
      }
      return share;
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

    private Set<ClassType> getCommonInterfaces( ClassInfo ci, Type fieldType )
    {
      return getCommonInterfaces( ci, fieldType, false );
    }
    private Set<ClassType> getCommonInterfaces( ClassInfo ci, Type fieldType, boolean erasure )
    {
      ArrayList<ClassType> linkFieldInterfaces = new ArrayList<>();
      findAllInterfaces( fieldType, new HashSet<>(), linkFieldInterfaces );

      if( fieldType.isInterface() && Util.getAnnotationMirror( fieldType.tsym, Structural.class ) != null )
      {
        // A structural interface is assumed to be fully mapped onto the declaring class.
        // Note, structural interfaces work only with forwarding, not with parts
        return new HashSet<>( linkFieldInterfaces );
      }

      Types types = getTypes();
      if( erasure )
      {
        return ci.getInterfaces().stream()
          .filter( i1 -> linkFieldInterfaces.stream()
            .anyMatch( i2 -> types.isSameType( types.erasure( i1 ), types.erasure( i2 ) ) ) )
          .collect( Collectors.toSet() );
      }
      return ci.getInterfaces().stream()
        .filter( i1 -> linkFieldInterfaces.stream()
          .anyMatch( i2 -> types.isSameType( i1, i2 ) ) )
        .collect( Collectors.toSet() );
    }

    private void linkInterfaces( LinkInfo li )
    {
      for( Set<NamedMethodType> mtSet : li.getMethodTypes().values() )
      {
        for( NamedMethodType mt : mtSet )
        {
          generateInterfaceImplMethod( li, mt );
        }
      }
    }

    private void generateInterfaceImplMethod( LinkInfo li, NamedMethodType namedMt )
    {
      Type csr = namedMt.getType();
      while( csr instanceof Type.DelegatedType )
      {
        csr = ((Type.DelegatedType)csr).qtype;
      }
      MethodType mt = (MethodType)csr;

      TreeMaker make = getTreeMaker();
      make.pos = li.getLinkField().pos;

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
      JCExpression link = make.Ident( li.getLinkField() );
      JCTree.JCFieldAccess forwardRef = (JCFieldAccess)make.Select( link, namedMt.getMethodSymbol() );
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
  
  // - for @link fields, assign linking class instance to $self of part classes
  // - for @part classes, replace 'this' with '$self' where applicable
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
          !replaceThisReceiver( tree ) &&
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
          result = replaceThis( tree, classDecl, assignment.type );
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
          result = replaceThis( tree, classDecl, varDecl.getType().type );
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
          result = replaceThis( tree, classDecl, ternary.type );
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
          result = replaceThis( tree, classDecl, cast.type );
        }
        else
        {
          reportError( tree, MSG_PART_THIS_NONINTERFACE_USE.get() );
        }
        return true;
      }
      return false;
    }

    private boolean replaceThisReceiver( JCExpression tree )
    {
      Tree parent = getParent( tree );
      if( parent instanceof JCFieldAccess )
      {
        JCFieldAccess fa = (JCFieldAccess)parent;
        MethodSymbol sym = (MethodSymbol)fa.sym;
        Pair<JCClassDecl, Type> enclClass_Iface = findInterfaceOfEnclosingTypeThatSymImplements( sym );
        if( enclClass_Iface == null )
        {
          return false;
        }
        if( enclClass_Iface.snd != null )
        {
          result = replaceThis( tree, enclClass_Iface.fst, enclClass_Iface.snd );
          return true;
        }
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
          if( method != null )
          {
            Type returnType = types.erasure( method.sym.getReturnType() );
            if( returnType.isInterface() )
            {
              JCClassDecl classDecl = findClassDecl( tree.type );
              result = replaceThis( tree, classDecl, returnType );
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
          MethodSymbol sym = (MethodSymbol)(m.meth instanceof JCFieldAccess ? ((JCFieldAccess)m.meth).sym : ((JCIdent)m.meth).sym);
          Symbol.VarSymbol paramSym = sym.getParameters().get( index );
          Types types = getTypes();
          Type paramType = types.erasure( paramSym.type );
          if( paramType.isInterface() )
          {
            JCClassDecl classDecl = findClassDecl( m.args.get( index ).type );
            if( classDecl != null )
            {
              result = replaceThis( tree, classDecl, paramType );
            }
          }
          else if( !types.isSameType( getSymtab().objectType, paramType ) )
          {
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

      processImpliedThisCall( tree );
      replaceSuperDefaultMethodCall( tree );
    }

    private void processImpliedThisCall( JCMethodInvocation tree )
    {
      if( tree.meth instanceof JCIdent )
      {
        MethodSymbol sym = (MethodSymbol)((JCIdent)tree.meth).sym;
        if( sym.isStatic() )
        {
          return;
        }

        JCClassDecl classDecl = _classDeclStack.peek();
        if( !isInPartClass( classDecl.sym ) )
        {
          return;
        }

        Pair<JCClassDecl, Type> enclClass_Iface = findInterfaceOfEnclosingTypeThatSymImplements( sym );
        if( enclClass_Iface == null || !isPartClass( enclClass_Iface.fst.sym ) )
        {
          return;
        }

        if( enclClass_Iface.snd != null )
        {
          JCExpression thisSub = replaceThis( tree, enclClass_Iface.fst, enclClass_Iface.snd );
          TreeMaker make = getTreeMaker();
          make.pos = tree.pos;
          JCMethodInvocation apply = make.Apply( List.nil(), make.Select( thisSub, sym ), tree.args );
          apply.type = tree.type;
          result = apply;
        }
      }
    }

    private JCExpression replaceThis( JCExpression tree, JCClassDecl receiverType, Type contextType )
    {
      ClassSymbol runtimeMethodsClassSym = getRtClassSym( RuntimeMethods.class );

      Names names = getNames();

      Symtab symtab = getSymtab();
      MethodSymbol replaceThisMethod = resolveMethod( getContext(), getCompilationUnit(),
        tree.pos(), names.fromString( "replaceThis" ),
        runtimeMethodsClassSym.type,
        List.from( new Type[]{symtab.classType, symtab.objectType, symtab.objectType} ) );

      TreeMaker make = getTreeMaker();

      JCIdent selfIdent = make.Ident( resolveField( tree.pos(), getContext(), names.fromString( SELF_FIELD ),
        receiverType.sym.type, receiverType ) );

      JCExpression thisExpr = make.QualThis( receiverType.sym.type );

      JCMethodInvocation replaceThisCall = make.Apply( List.nil(),
        memberAccess( make, RuntimeMethods.class.getTypeName() + ".replaceThis" ),
        List.of( make.ClassLiteral( contextType ), selfIdent, thisExpr ) );
      replaceThisCall.setPos( tree.pos );
      replaceThisCall.type = symtab.objectType;
      JCFieldAccess methodSelect = (JCFieldAccess)replaceThisCall.getMethodSelect();
      methodSelect.sym = replaceThisMethod;
      methodSelect.type = replaceThisMethod.type;
      methodSelect.pos = tree.pos;
      assignTypes( methodSelect.selected, runtimeMethodsClassSym );
      methodSelect.selected.pos = tree.pos;

      //noinspection UnnecessaryLocalVariable
      JCTypeCast castExpr = make.TypeCast( contextType, replaceThisCall );

      return castExpr;
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
          fa.type.tsym.getQualifiedName().toString().equals( RuntimeMethods.class.getTypeName() ) )
        {
          // don't replace 'this' for generated methods
          return true;
        }
      }
      return false;
    }

    private void replaceSuperDefaultMethodCall( JCMethodInvocation superInterfaceCall )
    {
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
      if( iface.isErroneous() )
      {
        return;
      }

      NamedMethodType namedMt = new NamedMethodType( (MethodSymbol)meth.sym, meth.type );

      Type csr = namedMt.getType();
      while( csr instanceof Type.DelegatedType )
      {
        csr = ((Type.DelegatedType)csr).qtype;
      }
      MethodType mt = (MethodType)csr;

      TreeMaker make = getTreeMaker();
      make.pos = superInterfaceCall.pos;

      Names names = getNames();
      Symtab symtab = getSymtab();

      // RuntimeMethods class sym
      Symbol.ClassSymbol runtimeMethodsClassSym = getRtClassSym( RuntimeMethods.class );
      MethodSymbol linksInterfaceToMethod = resolveMethod( getContext(), getCompilationUnit(), superInterfaceCall, getNames().fromString( "linksInterfaceTo" ), runtimeMethodsClassSym.type,
        List.of( symtab.classType, symtab.objectType, symtab.objectType ) );

      // SELF_FIELD sym
      JCIdent selfIdent = make.Ident( resolveField( tree.pos(), getContext(), names.fromString( SELF_FIELD ),
        classDecl.sym.type, classDecl ) );

      // RuntimeMethods.linksInterfaceTo( $self )

      JCTree.JCMethodInvocation linksInterfaceToCall = make.Apply( List.nil(),
        memberAccess( make, RuntimeMethods.class.getTypeName() + ".linksInterfaceTo" ),
        List.of( make.ClassLiteral( iface ), selfIdent, make.This( classDecl.sym.type ) ) );
      linksInterfaceToCall.type = symtab.booleanType;
      JCTree.JCFieldAccess methodSelect = (JCTree.JCFieldAccess)linksInterfaceToCall.getMethodSelect();
      methodSelect.sym = linksInterfaceToMethod;
      methodSelect.type = linksInterfaceToMethod.type;
      assignTypes( methodSelect.selected, runtimeMethodsClassSym );

      // ReflectUtil.invokeDefaultAsSelf

      // Arg list
      List<Type> parameterTypes = mt.getParameterTypes();
      ArrayList<JCExpression> paramTs = parameterTypes.stream().map( t -> make.ClassLiteral( t ) )
        .collect( Collectors.toCollection( () -> new ArrayList<>() ) );
      JCNewArray paramTypesArray = make.NewArray(
        make.Type( getTypes().erasure( symtab.classType ) ), List.nil(), List.from( paramTs ) );
      paramTypesArray.type = new Type.ArrayType( symtab.classType, symtab.arrayClass );
      JCNewArray argsArray = make.NewArray( make.Type( symtab.objectType ), List.nil(), List.from( superInterfaceCall.args ) );
      argsArray.type = new Type.ArrayType( symtab.objectType, symtab.arrayClass );
      List<JCExpression> theArgs = List.of( selfIdent, make.ClassLiteral( iface ),
        make.Literal( namedMt.getName().toString() ), paramTypesArray, argsArray );

      // make invokeDefault() call
      MethodSymbol invokeDefaultMethod = resolveMethod( getContext(), getCompilationUnit(), superInterfaceCall,
        getNames().fromString( "invokeDefault" ), runtimeMethodsClassSym.type,
        List.of( symtab.objectType, symtab.classType, symtab.stringType, symtab.objectType, symtab.objectType ) );
      JCTree.JCMethodInvocation invokeDefaultCall = make.Apply( List.nil(),
        memberAccess( make, RuntimeMethods.class.getTypeName() + ".invokeDefault" ), theArgs );
      invokeDefaultCall.type = symtab.booleanType;
      JCTree.JCFieldAccess mselect = (JCTree.JCFieldAccess)invokeDefaultCall.getMethodSelect();
      mselect.sym = invokeDefaultMethod;
      mselect.type = invokeDefaultMethod.type;
      assignTypes( mselect.selected, runtimeMethodsClassSym );
      JCTypeCast castInvokeDefaultCall = make.TypeCast( mt.getReturnType(), invokeDefaultCall );

      // replace superInterfaceCall with:  linksInterfaceToCall() ? invokeDefaultCall : superInterfaceCall

      JCConditional conditional = make.Conditional( linksInterfaceToCall, castInvokeDefaultCall, superInterfaceCall );
      conditional.type = mt.getReturnType();

      result = conditional;
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
      //   field = (<field-ref-expr-type>)RuntimeMethods.linkPart( this, <field-name>, <value-expr> )

      Symbol.ClassSymbol runtimeMethodsClassSym = getRtClassSym( RuntimeMethods.class );

      Names names = getNames();

      Symtab symtab = getSymtab();
      Symbol.MethodSymbol assignPartMethod = resolveMethod( getContext(), getCompilationUnit(),
        assignmentOrVarDecl.pos(), names.fromString( "linkPart" ),
        runtimeMethodsClassSym.type,
        List.from( new Type[]{symtab.objectType, symtab.stringType, symtab.objectType} ) );

      TreeMaker make = getTreeMaker();

      JCTree.JCMethodInvocation assignPartCall = make.Apply( List.nil(),
        memberAccess( make, RuntimeMethods.class.getTypeName() + ".linkPart" ),
        List.of( make.This( _classDeclStack.peek().type ), make.Literal( linkField.name.toString() ), rhs ) );
      assignPartCall.setPos( assignmentOrVarDecl.pos );
      assignPartCall.type = symtab.objectType;
      JCTree.JCFieldAccess methodSelect = (JCTree.JCFieldAccess)assignPartCall.getMethodSelect();
      methodSelect.sym = assignPartMethod;
      methodSelect.type = assignPartMethod.type;
      methodSelect.pos = assignmentOrVarDecl.pos;
      assignTypes( methodSelect.selected, runtimeMethodsClassSym );
      methodSelect.selected.pos = assignmentOrVarDecl.pos;

      //noinspection UnnecessaryLocalVariable
      JCTypeCast castExpr = make.TypeCast( linkField.type, assignPartCall );
      return castExpr;
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
      if( lhs instanceof JCIdent && ((JCIdent)lhs).sym.getAnnotation( link.class ) != null )
      {
        return ((JCIdent)lhs).sym;
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
      if( tree.sym.getAnnotation( link.class ) != null )
      {
        return tree.sym;
      }
      return null;
    }
  }

  private void findAllInterfaces( Type type, Set<Type> seen, ArrayList<ClassType> result )
  {
    if( seen.stream().anyMatch( t -> getTypes().isSameType( t, type ) ) )
    {
      return;
    }
    seen.add( type );

    if( type.isInterface() )
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
      findAllInterfaces( type, superClass, seen, result );
    }

    List<Type> superInterfaces = ((ClassSymbol)type.tsym).getInterfaces();
    if( superInterfaces != null )
    {
      superInterfaces.forEach( superInterface -> findAllInterfaces( type, superInterface, seen, result ) );
    }
  }

  private void findAllInterfaces( Type type, Type superType, Set<Type> seen, ArrayList<ClassType> result )
  {
    superType = getUnderlyingType( superType );
    if( superType != Type.noType && !superType.isErroneous() )
    {
      // map the super type as declared in type
      superType = getTypes().asSuper( type, superType.tsym );
      findAllInterfaces( superType, seen, result );
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
    Attribute.Compound anno = new Attribute.Compound( annoSym.type,List.nil() );
    sym.appendAttributes( List.of( anno ) );
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
    Symbol.ClassSymbol sym = IDynamicJdk.instance().getTypeElement( getContext(), getCompilationUnit(), cls.getTypeName() );
    if( sym == null )
    {
      sym = JavacElements.instance( getContext() ).getTypeElement( cls.getTypeName() );
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

    ClassInfo( JCClassDecl classDecl )
    {
      _classDecl = classDecl;
      _linkInfos = new HashMap<>();
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
  }

  private class LinkInfo
  {
    private final JCVariableDecl _linkField;

    private final ArrayList<JCMethodDecl> _generatedMethods;
    private final Map<Name, Set<NamedMethodType>> _methodTypes;
    private final ArrayList<ClassType> _interfaces;
    private final boolean _share;

    LinkInfo( JCVariableDecl linkField, ArrayList<ClassType> linkdInterfaces, boolean share )
    {
      _linkField = linkField;
      _generatedMethods = new ArrayList<>();
      _methodTypes = new HashMap<>();
      _interfaces = new ArrayList<>( linkdInterfaces );
      _share = share;
    }

    public JCVariableDecl getLinkField()
    {
      return _linkField;
    }

    public boolean isShare()
    {
      return _share;
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
