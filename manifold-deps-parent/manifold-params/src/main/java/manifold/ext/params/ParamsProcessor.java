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
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import manifold.api.type.ICompilerComponent;
import manifold.api.util.JavacDiagnostic;
import manifold.ext.params.rt.manifold_params;
import manifold.internal.javac.*;
import manifold.rt.api.Null;
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.Stack;
import manifold.util.ReflectUtil;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static manifold.ext.params.ParamsIssueMsg.MSG_OVERRIDE_DEFAULT_VALUES_NOT_ALLOWED;

public class ParamsProcessor implements ICompilerComponent, TaskListener
{
  private BasicJavacTask _javacTask;
  private Context _context;
  private TaskEvent _taskEvent;
  private ParentMap _parents;

  @Override
  public void init( BasicJavacTask javacTask, TypeProcessor typeProcessor )
  {
    _javacTask = javacTask;
    _context = _javacTask.getContext();
    _parents = new ParentMap( () -> getCompilationUnit() );

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
  public void started( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.ENTER )
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

  @Override
  public boolean isSuppressed( JCDiagnostic.DiagnosticPosition pos, String issueKey, Object[] args )
  {
    return ICompilerComponent.super.isSuppressed( pos, issueKey, args );
  }

  // Add data classes reflecting parameters and corresponding method overloads
  //
  private class Enter_Start extends TreeTranslator
  {
    private final Stack<Pair<JCClassDecl, ArrayList<JCTree>>> _newDefs;

    public Enter_Start()
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
      _newDefs.push( new Pair<>( tree, new ArrayList<>() ) );
      try
      {
        handleRecord( tree );
        super.visitClassDef( tree );

        // add them to defs
        ArrayList<JCTree> addedDefs = _newDefs.peek().snd;
        if( !addedDefs.isEmpty() )
        {
          ArrayList<JCTree> newDefs = new ArrayList<>( tree.defs );
          newDefs.addAll( addedDefs );
          tree.defs = List.from( newDefs );
        }
      }
      finally
      {
        _newDefs.pop();
      }
    }

    // since at this early stage the record's implicit ctor isn't there yet,
    // we create the ctor as a convenience, so we can call `processParams( ctor )`
    // as if the initializers were on the ctor
    private void handleRecord( JCClassDecl tree )
    {
      if( !tree.getKind().name().equals( "RECORD" ) )
      {
        return;
      }

      if( tree.defs == null || tree.defs.stream().noneMatch( def -> isRecordParam( def ) ) )
      {
        return;
      }

      TreeMaker make = getTreeMaker();
      make.at( tree.pos );
      TreeCopier<?> copier = new TreeCopier<>( make );

      List<JCVariableDecl> params = List.nil();
      for( JCTree def: tree.defs )
      {
        if( isRecordParam( def ) )
        {
          params = params.append( (JCVariableDecl)copier.copy( def ) );
        }
      }

      if( !params.isEmpty() )
      {
        JCMethodDecl ctor = make.MethodDef( make.Modifiers( Flags.PUBLIC ), getNames().init, make.TypeIdent( TypeTag.VOID ),
          List.nil(), params, List.nil(), make.Block( 0L, List.nil() ), null );
        processParams( ctor );
      }
    }

    private boolean isRecordParam( JCTree def )
    {
      final long RECORD = 1L << 61; // from Flags in newer JDKs
      return def instanceof JCVariableDecl && (((JCVariableDecl)def).getModifiers().flags & RECORD) != 0;
    }

    @Override
    public void visitMethodDef( JCMethodDecl methodDecl )
    {
      processParams( methodDecl );
      super.visitMethodDef( methodDecl );
    }

    private void processParams( JCMethodDecl methodDecl )
    {
      for( JCVariableDecl param : methodDecl.params )
      {
        if( param.init != null )
        {
          handleOptionalParams( methodDecl );
          return;
        }
      }
    }

    private void handleOptionalParams( JCMethodDecl optParamsMeth )
    {
      TreeMaker make = getTreeMaker();
      make.at( optParamsMeth.pos );

      TreeCopier<?> copier = new TreeCopier<>( make );

      ArrayList<JCTree> defs = _newDefs.peek().snd;

      JCClassDecl paramsClass = makeParamsClass( optParamsMeth, make, copier );
      defs.add( paramsClass );

      JCMethodDecl paramsMethod = makeParamsMethod( optParamsMeth, paramsClass, make, copier );
      defs.add( paramsMethod );

      if( shouldAddTelescopeMethods( optParamsMeth ) )
      {
        ArrayList<JCMethodDecl> telescopeMethods = makeTelescopeMethods( optParamsMeth, make, copier );
        defs.addAll( telescopeMethods );
      }
    }

    private boolean shouldAddTelescopeMethods( JCMethodDecl optParamsMeth )
    {
      return true;
    }

    private ArrayList<JCMethodDecl> makeTelescopeMethods( JCMethodDecl optParamsMeth, TreeMaker make, TreeCopier<?> copier )
    {
      int optParamsCount = 0;
      for( JCVariableDecl p: optParamsMeth.params )
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
        if( true /*!methodExists( telescopeMethod )*/ )
        {
          result.add( telescopeMethod );
        }
      }
      return result;
    }

    private JCMethodDecl makeTelescopeMethod( JCMethodDecl targetMethod, TreeMaker make, TreeCopier<?> copier, int optParamsInSig )
    {
      JCMethodDecl copy = copier.copy( targetMethod );

      ArrayList<JCVariableDecl> reqParams = new ArrayList<>();
      ArrayList<JCVariableDecl> optParams = new ArrayList<>();
      for( JCVariableDecl p: copy.params )
      {
        if( p.init == null )
        {
          reqParams.add( p );
        }
        else
        {
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
      copy.mods.flags = flags;
      // mark with @manifold_params for IJ use
      JCAnnotation paramsAnno = make.Annotation(
        memberAccess( make, manifold_params.class.getTypeName() ),
        List.of( paramsString( targetMethod.params.stream().map( p -> p.name.toString() ) ) ) );
      copy.mods.annotations = copy.mods.annotations.append( paramsAnno );

      // Params
      ArrayList<JCVariableDecl> params = new ArrayList<>();
      ArrayList<JCExpression> args = new ArrayList<>();
      for( JCVariableDecl reqParam: reqParams )
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
//    private JCMethodDecl makeTelescopeMethod( JCMethodDecl targetMethod, TreeMaker make, TreeCopier<?> copier, int optParamsInSig )
//    {
//      JCMethodDecl copy = copier.copy( targetMethod );
//
//      ArrayList<JCVariableDecl> reqParams = new ArrayList<>();
//      ArrayList<JCVariableDecl> optParams = new ArrayList<>();
//      for( JCVariableDecl p: copy.params )
//      {
//        if( p.init == null )
//        {
//          reqParams.add( p );
//        }
//        else
//        {
//          optParams.add( p );
//        }
//      }
//
//      List<Name> targetParams = List.from( copy.params.stream().map( e -> e.name ).collect( Collectors.toList() ) );
//
//      // telescope interface methods have default impls that forward to original
//      long flags = targetMethod.getModifiers().flags;
//      if( (flags & Flags.STATIC) == 0 &&
//        (classDecl().mods.flags & Flags.INTERFACE) != 0 )
//      {
//        flags |= Flags.DEFAULT;
//      }
//      copy.mods.flags = flags;
//      // mark with @manifold_params for IJ use
//      JCAnnotation paramsAnno = make.Annotation( memberAccess( make, manifold_params.class.getTypeName() ), List.nil() );
//      copy.mods.annotations = copy.mods.annotations.append( paramsAnno );
//
//      // Params
//      ArrayList<JCVariableDecl> params = new ArrayList<>();
//      ArrayList<JCExpression> args = new ArrayList<JCExpression>()
//      {{
//        addAll( targetParams.stream().map( e -> (JCExpression)null ).collect( Collectors.toList() ) );
//      }};
//      ArrayList<JCStatement> locals = new ArrayList<>();
//      for( JCVariableDecl reqParam: reqParams )
//      {
//        params.add( make.VarDef( make.Modifiers( Flags.PARAMETER ), reqParam.name, reqParam.vartype, null ) );
//        int index = targetParams.indexOf( reqParam.name );
//        args.set( index, make.Ident( reqParam.name ) );
//      }
//      for( int i = 0; i < optParamsInSig; i++ )
//      {
//        JCVariableDecl optParam = optParams.get( i );
//        int index = targetParams.indexOf( optParam.name );
//        params.add( index, optParam );
//        args.set( index, make.Ident( optParam.name ) );
//      }
//      for( int i = optParamsInSig; i < optParams.size(); i++ )
//      {
//        JCVariableDecl local = optParams.get( i );
//        local.mods.flags &= ~Flags.PARAMETER;
//        int index = targetParams.indexOf( local.name );
//        args.set( index, make.Ident( local.name ) );
//        locals.add( local );
//      }
//      if( args.stream().anyMatch( Objects::isNull ) )
//      {
//        throw new IllegalStateException( "null " );
//      }
//      copy.params = List.from( params );
//
//      // body
//      JCMethodInvocation forwardCall;
//      if( targetMethod.name.equals( getNames().init ) )
//      {
//        forwardCall = make.Apply( List.nil(), make.Ident( getNames()._this ), List.from( args ) );
//      }
//      else
//      {
//        forwardCall = make.Apply( List.nil(), make.Ident( targetMethod.name ), List.from( args ) );
//      }
//      JCStatement stmt;
//      if( targetMethod.restype == null ||
//        (targetMethod.restype instanceof JCPrimitiveTypeTree &&
//          ((JCPrimitiveTypeTree)targetMethod.restype).typetag == TypeTag.VOID) )
//      {
//        stmt = make.Exec( forwardCall );
//      }
//      else
//      {
//        stmt = make.Return( forwardCall );
//      }
//      copy.body = make.Block( 0L, List.from( locals ).append( stmt ) );
//      return copy;
//    }

//    // make a structural interface reflecting the parameters of the method
//    // String foo(String name, int age = 100) {...}
//    // @Structural interface $foo {
//    //   @val String name;
//    //   @val int age = 100;
//    // }
//    private JCClassDecl makeStructuralInterface( JCMethodDecl targetMethod, TreeMaker make, TreeCopier<?> copier )
//    {
//      // name & modifiers
//      JCTree.JCModifiers modifiers = make.Modifiers( Flags.INTERFACE, List.of( makeAnnotation( Structural.class ) ) );
//      modifiers.pos = make.pos;
//      Names names = getNames();
//      Name name = names.fromString( "$" + targetMethod.getName() +
//        targetMethod.params.stream().map( e -> "_" + e.name ).reduce( "", (a,b) -> a+b ) );
//
//      // Type params
//      List<JCTypeParameter> typeParams = List.nil();
//      typeParams = typeParams.appendList( copier.copy( List.from( targetMethod.getTypeParameters() ) ) );
//      if( (targetMethod.getModifiers().flags & Flags.STATIC) == 0 )
//      {
//        typeParams = typeParams.appendList( copier.copy( List.from( classDecl().getTypeParameters().stream()
//          .map( e -> make.TypeParameter( e.name, List.nil() ) ).collect( Collectors.toList() ) ) ) );
//      }
//
//      // Properties
//      List<JCVariableDecl> methParams = targetMethod.getParameters();
//      List<JCTree> properties = List.nil();
//      for( JCVariableDecl methParam : methParams )
//      {
//        JCExpression propType = (JCExpression)copier.copy( methParam.getType() );
//        JCVariableDecl property = make.VarDef( make.Modifiers( 0L, List.of( makeAnnotation( val.class ) ) ), methParam.name, propType, methParam.init );
//        property.pos = make.pos;
//        properties = properties.append( property );
//      }
//
//      return make.ClassDef( modifiers, name, typeParams, null, List.nil(), properties );
//    }
//

    // make a static inner class reflecting the parameters of the method
    //
    // String foo(String name, int age = 100) {...}
    // static class $foo<T> {
    //   String name;
    //   int age = 100;
    //   $foo(EncClass<T> foo, String name,  boolean isAge,int age) {
    //     this.name = name;
    //     this.age = isAge ? age : this.age;
    //   }
    // }
    private JCClassDecl makeParamsClass( JCMethodDecl targetMethod, TreeMaker make, TreeCopier<?> copier )
    {
      // name & modifiers
      JCModifiers modifiers = make.Modifiers( Flags.PUBLIC | Flags.STATIC );
      modifiers.pos = make.pos;
      Names names = getNames();
      Name typeName = targetMethod.getName();
      boolean isConstructor = typeName.equals( getNames().init );
      if( isConstructor )
      {
        typeName = getNames().fromString( "constructor" );
      }
      Name name = names.fromString( "$" + typeName +
        targetMethod.params.stream().map( e -> "_" + (e.init == null ? "" : "opt$") + e.name ).reduce( "", (a,b) -> a+b ) );

      // Type params (copy from method and, if target method is nonstatic, from the enclosing class)
      List<JCTypeParameter> typeParams = List.nil();
      typeParams = typeParams.appendList( copier.copy( List.from( targetMethod.getTypeParameters() ) ) );
      boolean isStaticMethod = (targetMethod.getModifiers().flags & Flags.STATIC) != 0;
      if( !isStaticMethod )
      {
        typeParams = typeParams.appendList( copier.copy( List.from( classDecl().getTypeParameters().stream()
          .map( e -> make.TypeParameter( e.name, e.bounds ) ).collect( Collectors.toList() ) ) ) );
      }

      // Fields
      List<JCVariableDecl> methParams = targetMethod.getParameters();
      List<JCTree> fields = List.nil();
      for( JCVariableDecl methParam : methParams )
      {
        JCExpression type = (JCExpression)copier.copy( methParam.getType() );
        JCVariableDecl field = make.VarDef( make.Modifiers( 0L ), methParam.name, type, copier.copy( methParam.init ) );
        field.pos = make.pos;
        fields = fields.append( field );
      }

      // constructor
      JCMethodDecl ctor;
      {
        List<JCVariableDecl> ctorParams = List.nil();
        if( !isStaticMethod && !isConstructor )
        {
          // add Foo<T> parameter for context to solve owning type's type vars, so we can new up $foo type with diamond syntax

          JCIdent owningType = make.Ident( classDecl().getSimpleName() );
          List<JCExpression> tparams = List.from( classDecl().getTypeParameters().stream().map( tp -> make.Ident( tp.name ) ).collect( Collectors.toList() ) );
          JCExpression type = tparams.isEmpty() ? owningType : make.TypeApply( owningType, tparams );
          Name paramName = getNames().fromString( "$" + ManStringUtil.uncapitalize( owningType.name.toString() ) );
          JCVariableDecl param = make.VarDef( make.Modifiers( Flags.PARAMETER ), paramName, type, null );
          param.pos = make.pos;
          ctorParams = ctorParams.append( param );
        }
        List<JCStatement> ctorStmts = List.nil();
        for( int i = 0; i < methParams.size(); i++ )
        {
          JCVariableDecl methParam = methParams.get( i );
          JCStatement assignStmt;
          if( methParam.init != null )
          {
            // default param "xxx" requires an "isXxx" param indicating whether the "xxx" should assume the default value
            Name isXxx = getNames().fromString( "$is" + ManStringUtil.capitalize( methParam.name.toString() ) );
            JCVariableDecl param = make.VarDef( make.Modifiers( Flags.PARAMETER ), isXxx, make.TypeIdent( TypeTag.BOOLEAN ), null );
            param.pos = make.pos;
            ctorParams = ctorParams.append( param );

            JCVariableDecl field = (JCVariableDecl)fields.get( i );
            JCExpression init = field.init;
            field.init = null;

            // this.p1 = isP1 ? p1 : this.p1;
            assignStmt = make.Exec( make.Assign( make.Select( make.Ident( getNames()._this ), methParam.name ),
              make.Conditional( make.Ident( isXxx ), make.Ident( methParam.name ), init ) ) );
          }
          else
          {
            // this.p1 = p1;
            assignStmt = make.Exec( make.Assign( make.Select( make.Ident( getNames()._this ), methParam.name ),
              make.Ident( methParam.name ) ) );
          }
          ctorStmts = ctorStmts.append( assignStmt );
          JCExpression type = (JCExpression)copier.copy( methParam.getType() );
          JCVariableDecl param = make.VarDef( make.Modifiers( Flags.PARAMETER ), methParam.name, type, null );
          param.pos = make.pos;
          ctorParams = ctorParams.append( param );
        }
        JCBlock body = make.Block( 0L, ctorStmts );
        Name ctorName = getNames().init;
        ctor = make.MethodDef( make.Modifiers( Flags.PUBLIC ), ctorName, make.TypeIdent( TypeTag.VOID ), List.nil(), ctorParams, List.nil(), body, null );
      }
      return make.ClassDef( modifiers, name, typeParams, null, List.nil(), fields.append( ctor ) );
    }

    // make an overload of the method with a single parameter that is the params class we generate here,
    // and forward to the original method:
    //   String foo(String name, int age = 100) {...}
    //   String foo($foo args) {return foo(args.name, args.age);}
    // where $foo is the params class generated above,
    // so we can call it using tuples for named args and optional params:
    // foo((name:"Scott"));
    private JCMethodDecl makeParamsMethod( JCMethodDecl targetMethod, JCClassDecl paramsClass, TreeMaker make, TreeCopier<?> copier )
    {
      // Method name & modifiers
      Name name = targetMethod.getName();
      long mods = targetMethod.getModifiers().flags;
      if( (mods & Flags.STATIC) == 0 &&
        (classDecl().mods.flags & Flags.INTERFACE) != 0 )
      {
        mods |= Flags.DEFAULT;
      }
      JCModifiers modifiers = make.Modifiers( mods ); //todo: annotations? hard to know which ones should be reflected.

      // Throws
      List<JCExpression> thrown = copier.copy( targetMethod.thrown );

      // Type params
      List<JCTypeParameter> typeParams = copier.copy( targetMethod.getTypeParameters() );

      // Params
      Name paramName = getNames().fromString( "args" );
      JCVariableDecl param = make.VarDef( make.Modifiers( Flags.PARAMETER ), paramName,
        paramsClass.getTypeParameters().isEmpty()
          ? make.Ident( paramsClass.name )
          : make.TypeApply( make.Ident( paramsClass.name ),
             List.from( paramsClass.getTypeParameters().stream()
               .map( tp -> make.Ident( tp.name ) )
               .collect( Collectors.toList() ) ) ), null );

      // Return type
      final JCExpression resType = (JCExpression)copier.copy( targetMethod.getReturnType() );
      
      // body
      JCMethodInvocation forwardCall;
      if( targetMethod.name.equals( getNames().init ) )
      {
        forwardCall = make.Apply( List.nil(), make.Ident( getNames()._this ),
          List.from( targetMethod.params.stream().map( e -> make.Select( make.Ident( paramName ), e.name ) )
            .collect( Collectors.toList() ) ) );
      }
      else
      {
        forwardCall = make.Apply( List.nil(), make.Ident( targetMethod.name ),
          List.from( targetMethod.params.stream().map( e -> make.Select( make.Ident( paramName ), e.name ) )
            .collect( Collectors.toList() ) ) );
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
      JCBlock body = make.Block( 0L, List.of( stmt ) );

      return make.MethodDef( modifiers, name, resType, typeParams, List.of( param ), thrown, body, null );
    }
  }

  private JCLiteral paramsString( Stream<String> paramNames )
  {
    return getTreeMaker().Literal( paramNames.collect( Collectors.joining( ", ") ) );
  }

  class Analyze_Finish extends TreeTranslator
  {
    private Stack<JCClassDecl> _classDecls = new Stack<>();

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

      for( JCVariableDecl param : methodDecl.params )
      {
        if( param.init != null )
        {
          checkIsOverride( methodDecl.sym, param.init );
        }
      }

      if( methodDecl.mods.getAnnotations().stream().anyMatch(
        anno -> anno.getAnnotationType().type.tsym.getQualifiedName().toString().equals( manifold_params.class.getTypeName() ) ) )
      {
        checkDuplication( methodDecl );
      }
    }

    private void checkDuplication( JCMethodDecl telescopeMethod )
    {
      Set<Symbol.MethodSymbol> methods = new LinkedHashSet<>();
      ManTypes.getAllMethods( classDecl().type, m -> m != null && m.name.equals( telescopeMethod.name ), methods );

      Symbol.MethodSymbol psiMethod = findTargetMethod( telescopeMethod.sym, methods );

      for( Symbol.MethodSymbol msym : new ArrayList<Symbol.MethodSymbol>(){{methods.forEach( m -> add( (Symbol.MethodSymbol)m ) );}} )
      {
        if( msym != telescopeMethod.sym && getTypes().overrideEquivalent( msym.type, telescopeMethod.type ) )
        {
          String paramNames = psiMethod.params.stream().map( p -> p.name.toString() ).collect( Collectors.joining( ", " ) );
          JCMethodDecl paramsMethodDecl = getTargetMethod( telescopeMethod.name, paramNames );;
          if( msym.owner == classDecl().sym )
          {
            Symbol.MethodSymbol paramsMethodFromMsym = findTargetMethod( msym, methods );
            if( paramsMethodFromMsym != null )
            {
              String signature = telescopeMethod.params.stream()
                .map( p -> p.type.tsym.getSimpleName() )
                .collect( Collectors.joining( ", " ) );
              reportError( paramsMethodDecl,
                "'" + psiMethod + "' clashes with '" + msym + "' using sub-signature '(" + signature + ")'" );
            }
            else
            {
              reportError( paramsMethodDecl, "Optional parameter method: " +
                "'" + psiMethod + "' indirectly clashes with '" + msym + "'" );
            }
          }
          else if( !msym.isConstructor() && !msym.isPrivate() && !msym.isStatic() )
          {
            reportWarning( paramsMethodDecl, "Optional parameter method: " +
              "'" + psiMethod + "' indirectly overrides method '" + msym + "' in class '" +
              (msym.owner == null ? "<unknown>" : msym.owner.getSimpleName()) + "'" );
          }
        }
      }
    }

    private Symbol.MethodSymbol findTargetMethod( Symbol.MethodSymbol telescopeMethod, Set<Symbol.MethodSymbol> methods )
    {
      Symbol.MethodSymbol tm = null;
      manifold_params anno = telescopeMethod.getAnnotation( manifold_params.class );
      if( anno != null )
      {
        tm = getTargetMethod( anno, methods );
      }
      return tm;
    }

    Symbol.MethodSymbol getTargetMethod( manifold_params anno, Set<Symbol.MethodSymbol> methods )
    {
      return methods.stream()
        .filter( m ->
          anno.value().equals( m.params.stream().map( p -> p.name.toString() ).collect( Collectors.joining( ", ") ) ) )
        .findFirst().orElse( null );
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


    private void checkIsOverride( Symbol.MethodSymbol msym, JCExpression init )
    {
      boolean canOverride = !msym.isConstructor() && !msym.isPrivate();
      Check check = Check.instance( getContext() );
      if( canOverride && (boolean)ReflectUtil.method( check, "isOverrider", Symbol.class ).invoke( msym ) )
      {
        reportError( init, MSG_OVERRIDE_DEFAULT_VALUES_NOT_ALLOWED.get() );
      }
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
}
