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
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeCopier;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import manifold.api.type.ICompilerComponent;
import manifold.internal.javac.JavacPlugin;
import manifold.internal.javac.ParentMap;
import manifold.internal.javac.TypeProcessor;
import manifold.rt.api.Null;
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.Stack;

import java.util.ArrayList;
import java.util.stream.Collectors;

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

  @Override
  public InitOrder initOrder( ICompilerComponent compilerComponent )
  {
    // ParamsProcessor must be processed Before PropertiesProcessor
    return compilerComponent.getClass().getName().equals( "manifold.ext.props.PropertyProcessor" )
      ? InitOrder.Before
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

  // Add structural interfaces and forwarding methods
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
          break;
        }
      }
    }

    private void handleOptionalParams( JCMethodDecl optParamsMeth )
    {
      TreeMaker make = getTreeMaker();
      make.at( optParamsMeth.pos );

      TreeCopier<?> copier = new TreeCopier<>( make );

//      JCClassDecl structuralInterface = makeStructuralInterface( optParamsMeth, make, copier );
//      ArrayList<JCTree> defs = _newDefs.peek().snd;
//      defs.add( structuralInterface );

      JCClassDecl paramsClass = makeParamsClass( optParamsMeth, make, copier );
      ArrayList<JCTree> defs = _newDefs.peek().snd;
      defs.add( paramsClass );

      JCMethodDecl forwardingMeth = makeForwardingMethod( optParamsMeth, paramsClass, make, copier );
      defs.add( forwardingMeth );
    }

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
      JCTree.JCModifiers modifiers = make.Modifiers( Flags.PUBLIC | Flags.STATIC );
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
        JCVariableDecl field = make.VarDef( make.Modifiers( 0L ), methParam.name, type, methParam.init );
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
        for( JCVariableDecl methParam: methParams )
        {
          JCStatement assignStmt;
          if( methParam.init != null )
          {
            // default param "xxx" requires an "isXxx" param indicating whether the "xxx" should assume the default value
            Name isXxx = getNames().fromString( "$is" + ManStringUtil.capitalize( methParam.name.toString() ) );
            JCVariableDecl param = make.VarDef( make.Modifiers( Flags.PARAMETER ), isXxx, make.TypeIdent( TypeTag.BOOLEAN ), null );
            param.pos = make.pos;
            ctorParams = ctorParams.append( param );

            // this.p1 = isP1 ? p1 : this.p1;
            assignStmt = make.Exec( make.Assign( make.Select( make.Ident( getNames()._this ), methParam.name ),
                make.Conditional( make.Ident( isXxx ), make.Ident( methParam.name ), make.Select( make.Ident( getNames()._this ), methParam.name ) ) ) );
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
        Name ctorName = getNames().fromString( "<init>" );
        ctor = make.MethodDef( make.Modifiers( Flags.PUBLIC ), ctorName, make.TypeIdent( TypeTag.VOID ), List.nil(), ctorParams, List.nil(), body, null );
      }
      return make.ClassDef( modifiers, name, typeParams, null, List.nil(), fields.append( ctor ) );
    }

    // make an overload of the method we're forwarding to:
    //   String foo(String name, int age = 100) {...}
    //   String foo($foo args) {return foo(args.name, args.age);}
    // where $foo is the structural interface generated above,
    // so we can call it using tuples for named args and optional params:
    // foo((name:"Scott"));
    private JCMethodDecl makeForwardingMethod( JCMethodDecl targetMethod, JCClassDecl paramsClass, TreeMaker make, TreeCopier<?> copier )
    {
      // Method name & modifiers
      Name name = targetMethod.getName();
      long mods = targetMethod.getModifiers().flags;
      if( (mods & Flags.STATIC) == 0 &&
        (classDecl().mods.flags & Flags.INTERFACE) != 0 )
      {
        mods |= Flags.DEFAULT;
      }
      JCTree.JCModifiers modifiers = make.Modifiers( mods ); //todo: annotations? hard to know which ones should be reflected.

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

  class Analyze_Finish extends TreeTranslator
  {
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

  private JCTree.JCAnnotation makeAnnotation( Class<?> anno )
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
}
