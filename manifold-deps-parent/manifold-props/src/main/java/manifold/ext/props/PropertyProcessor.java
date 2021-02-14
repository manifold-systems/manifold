/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.ext.props;

import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import com.sun.tools.javac.util.List;
import manifold.api.type.ContributorKind;
import manifold.api.type.ICompilerComponent;
import manifold.ext.ExtensionManifold;
import manifold.ext.ExtensionTransformer;
import manifold.ext.props.rt.api.*;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavacPlugin;
import manifold.internal.javac.ManAttr;
import manifold.internal.javac.TypeProcessor;
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.Stack;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.LocklessLazyVar;

import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.reflect.Modifier.*;
import static manifold.ext.props.PropIssueMsg.*;

public class PropertyProcessor implements ICompilerComponent, TaskListener
{
  private TypeProcessor _tp;
  private BasicJavacTask _javacTask;
  private Stack<Pair<JCClassDecl, ArrayList<JCTree>>> _propertyStatements;
  private Map<ClassSymbol, Set<VarSymbol>> _propMap;
  private Map<ClassSymbol, Set<VarSymbol>> _backingMap;
  private Map<ClassSymbol, Set<VarSymbol>> _nonbackingMap;
  private TaskEvent _taskEvent;
  private LocklessLazyVar<ExtensionTransformer> _extensionTransformer = LocklessLazyVar.make( () -> {
    ExtensionManifold extensionManifold = (ExtensionManifold)JavacPlugin.instance().getHost().getSingleModule()
      .getTypeManifolds().stream()
      .filter( e -> e instanceof ExtensionManifold )
      .findFirst().orElse( null );
    return new ExtensionTransformer( extensionManifold, _tp );
  } );

  @Override
  public void init( BasicJavacTask javacTask, TypeProcessor typeProcessor )
  {
    _tp = typeProcessor;
    _javacTask = javacTask;
    _propertyStatements = new Stack<>();
    _propMap = new HashMap<>();
    _backingMap = new HashMap<>();
    _nonbackingMap = new HashMap<>();

    javacTask.addTaskListener( this );
  }

  @Override
  public void tailorCompiler()
  {
    replaceClassReaderCompleter();
  }

  /**
   * Replace the {@code ClassReader.thisCompleter} with our own so that after a .class file loads we can restore the
   * property fields to their original declared setting. In the case of a property having a backing field, the field's
   * {@code private} access modifier is changed back to whatever it was declared to be in source. If a property field
   * is not a backing field, it does not exist in the .class file, therefore this is where we recreate it.
   * <p/>
   * Note the .class file remains untouched; the changes made here are only to the compiler's ClassSymbol.
   */
  private void replaceClassReaderCompleter()
  {
    ReflectUtil.LiveFieldRef thisCompleterField;
    if( JreUtil.isJava8() )
    {
      ClassReader classReader = ClassReader.instance( _javacTask.getContext() );
      thisCompleterField = ReflectUtil.field( classReader, "thisCompleter" );
    }
    else
    {
      Object classFinder = ReflectUtil.method( "com.sun.tools.javac.code.ClassFinder", "instance", Context.class )
        .invokeStatic( _javacTask.getContext() );
      thisCompleterField = ReflectUtil.field( classFinder, "thisCompleter" );
    }

    Symbol.Completer thisCompleter = (Symbol.Completer)thisCompleterField.get();
    if( !(thisCompleter instanceof MyCompleter) )
    {
      Symbol.Completer myCompleter = new MyCompleter( thisCompleter );
      thisCompleterField.set( myCompleter );
    }
  }

  private class MyCompleter implements Symbol.Completer
  {
    private final Symbol.Completer _thisCompleter;

    public MyCompleter( Symbol.Completer thisCompleter )
    {
      _thisCompleter = thisCompleter;
    }

    @Override
    public void complete( Symbol sym ) throws Symbol.CompletionFailure
    {
      _thisCompleter.complete( sym );
      Names names = Names.instance( _javacTask.getContext() );
      if( sym instanceof ClassSymbol && sym.name != names.package_info )
      {
        if( !restorePropFields( (ClassSymbol)sym, names ) )
        {
          // It may be that the class hasn't finished adding annotations, try again after annotations complete
          Annotate.instance( _javacTask.getContext() )
            .normal( () -> restorePropFields( (ClassSymbol)sym, names ) );
        }
      }
    }

    /**
     * Restore the user defined property field, either by recreating it from @propgen info, or by resetting the
     * access modifier. Note the field is not really in the bytecode of the class, this is just the VarSymbol the
     * compiler needs to resolve refs.
     */
    private boolean restorePropFields( ClassSymbol classSym, Names names )
    {
      boolean handled = false;

      // Restore originally declared access on backing fields
      //
      for( Symbol sym: IDynamicJdk.instance().getMembers( classSym, false ) )
      {
        if( sym instanceof VarSymbol )
        {
          Attribute.Compound propgen = getAnnotationMirror( sym, propgen.class );
          if( propgen != null )
          {
            sym.flags_field = sym.flags_field & ~PRIVATE | getFlags( propgen );
            handled = true;
          }
        }
      }

      // Recreate non-backing property fields based on @propgen annotations on corresponding getter/setter
      //
      outer:
      for( Symbol sym: IDynamicJdk.instance().getMembers( classSym, false ) )
      {
        if( sym instanceof MethodSymbol )
        {
          Attribute.Compound propgenAnno = getAnnotationMirror( sym, propgen.class );
          if( propgenAnno != null )
          {
            Name fieldName = names.fromString( getName( propgenAnno ) );
            for( Symbol existing: IDynamicJdk.instance().getMembersByName( classSym, fieldName, false ) )
            {
              if( existing instanceof VarSymbol )
              {
                // prop field already exists
                continue outer;
              }
            }

            // Create and enter the prop field

            MethodSymbol meth = (MethodSymbol)sym;
            Type t = meth.getParameters().isEmpty()
              ? meth.getReturnType()
              : meth.getParameters().get( 0 ).type;
            VarSymbol propField = new VarSymbol( getFlags( propgenAnno ), fieldName, t, classSym );

            // add the @prop, @get, @set annotations
            propField.appendAttributes( List.from( propgenAnno.values.stream()
              .filter( e -> e.snd instanceof Attribute.Array )
              .map( e -> (Attribute.Compound)((Attribute.Array)e.snd).values[0] )
              .collect( Collectors.toList() ) ) );

            // reflectively call:  classSym.members_field.enter( propField );
            ReflectUtil.method( ReflectUtil.field( classSym, "members_field" ).get(),
              "enter", Symbol.class ).invoke( propField );

            handled = true;
          }
        }
      }

      return handled;
    }

    private String getName( Attribute.Compound anno )
    {
      for( MethodSymbol methSym: anno.getElementValues().keySet() )
      {
        if( methSym.getSimpleName().toString().equals( "name" ) )
        {
          return (String)anno.getElementValues().get( methSym ).getValue();
        }
      }
      throw new IllegalStateException();
    }
  }
  
  @Override
  public void started( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.ENTER &&
        e.getKind() != TaskEvent.Kind.GENERATE )
    {
      return;
    }

    _taskEvent = e;
    try
    {
      for( Tree tree : e.getCompilationUnit().getTypeDecls() )
      {
        if( tree instanceof JCClassDecl &&
          shouldProcess( e.getCompilationUnit().getPackageName() + "." + ((JCClassDecl)tree).name, e ) )
        {
          JCClassDecl classDecl = (JCClassDecl)tree;
          if( e.getKind() == TaskEvent.Kind.ENTER )
          {
            classDecl.accept( new Enter_Start() );
          }
          else if( e.getKind() == TaskEvent.Kind.GENERATE )
          {
            new Generate_Start().handleClass( classDecl.sym );
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
        e.getKind() != TaskEvent.Kind.ANALYZE &&
        e.getKind() != TaskEvent.Kind.GENERATE )
    {
      return;
    }

    _taskEvent = e;
    try
    {
      for( Tree tree : e.getCompilationUnit().getTypeDecls() )
      {
        if( tree instanceof JCClassDecl &&
          shouldProcess( e.getCompilationUnit().getPackageName() + "." + ((JCClassDecl)tree).name, e ) )
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
          else if( e.getKind() == TaskEvent.Kind.GENERATE )
          {
            new Generate_Finish().handleClass( classDecl.sym );
          }
        }
      }
    }
    finally
    {
      _taskEvent = null;
    }
  }

  public boolean shouldProcess( String fqn, TaskEvent e )
  {
    if( e.getKind() == TaskEvent.Kind.ENTER )
    {
      // ensure JavacPlugin is initialized, particularly for Enter since the order of TaskListeners is evidently not
      // maintained by JavaCompiler i.e., this TaskListener is added after JavacPlugin, but is notified earlier
      JavacPlugin.instance().initialize( e );
    }

    return JavacPlugin.instance().getHost().getSingleModule().findTypeManifoldsFor( fqn )
      .stream().map( ee -> ee.getContributorKind() )
        .noneMatch( k -> k == ContributorKind.Supplemental );
  }

  // Make getter/setter methods corresponding with @prop, @get, @set fields
  //
  private class Enter_Start extends TreeTranslator
  {
    @Override
    public void visitClassDef( JCClassDecl classDecl )
    {
      _propertyStatements.push( new Pair<>( classDecl, new ArrayList<>() ) );
      try
      {
        // create accessors
        super.visitClassDef( classDecl );
        // add them to defs
        ArrayList<JCTree> addedDefs = _propertyStatements.peek().snd;
        if( !addedDefs.isEmpty() )
        {
          ArrayList<JCTree> newDefs = new ArrayList<>( classDecl.defs );
          newDefs.addAll( addedDefs );
          classDecl.defs = List.from( newDefs );
        }
      }
      finally
      {
        _propertyStatements.pop();
      }
    }

    @Override
    public void visitVarDef( JCVariableDecl tree )
    {
      super.visitVarDef( tree );

      int modifiers = (int)tree.getModifiers().flags;

      JCClassDecl classDecl = _propertyStatements.peek().fst;
      if( classDecl.defs.contains( tree ) )
      {
        JCAnnotation prop = getAnnotation( tree, manifold.ext.props.rt.api.prop.class );
        JCAnnotation get = getAnnotation( tree, manifold.ext.props.rt.api.get.class );
        JCAnnotation set = getAnnotation( tree, manifold.ext.props.rt.api.set.class );

        if( prop == null && get == null && set == null )
        {
          // not a property field
          return;
        }

        if( isInterface( classDecl ) && (modifiers & (PUBLIC|PROTECTED|PRIVATE)) == 0 )
        {
          // must explicitly default @prop fields to PUBLIC inside interfaces
          tree.getModifiers().flags |= PUBLIC;
        }

        // add getter and/or setter
        // or, if a PRIVATE property and no user-defined accessors exist, no benefit from property, so treat as field

        JCMethodDecl generatedGetter = null;
        JCMethodDecl generatedSetter = null;
        boolean shouldMakeProperty = !isPrivate( (int)tree.getModifiers().flags );

        Pair<JCClassDecl, ArrayList<JCTree>> pair = _propertyStatements.peek();
        if( set == null || get != null )
        {
          boolean getAbstract = isAbstract( classDecl, tree, get == null ? prop.args : get.args );
          boolean getFinal = hasOption( get == null ? prop.args : get.args, PropOption.Final );
          PropOption getAccess = getAccess( classDecl, get == null ? prop.args : get.args );

          if( !isInterface( classDecl ) && isWeakerAccess( getAccess, getAccess( modifiers ) ) )
          {
            reportError( get != null ? get : prop, MSG_ACCESSOR_WEAKER.get( "get",
              PropOption.fromModifier( getAccess( modifiers ) ).name().toLowerCase() ) );
          }

          generatedGetter = makeGetter( classDecl, tree, getAbstract, getFinal, getAccess, get != null ? get : prop );
          if( generatedGetter == null )
          {
            shouldMakeProperty = true;
          }
        }

        if( get == null || set != null )
        {
          boolean setAbstract = isAbstract( classDecl, tree, set == null ? prop.args : set.args );
          boolean setFinal = hasOption( set == null ? prop.args : set.args, PropOption.Final );
          PropOption setAccess = getAccess( classDecl, set == null ? prop.args : set.args );

          if( set != null && isFinal( classDecl, tree ) )
          {
            reportError( set, MSG_SET_WITH_FINAL.get() );
          }
          else if( !isInterface( classDecl ) && isWeakerAccess( setAccess, getAccess( modifiers ) ) )
          {
            reportError( set != null ? set : prop, MSG_ACCESSOR_WEAKER.get( "set",
              PropOption.fromModifier( getAccess( modifiers ) ).name().toLowerCase() ) );
          }
          else if( !isFinal( classDecl, tree ) )
          {
            generatedSetter = makeSetter( classDecl, tree, setAbstract, setFinal, setAccess, set != null ? set : prop );
            if( generatedSetter == null )
            {
              shouldMakeProperty = true;
            }
          }
        }

        if( shouldMakeProperty )
        {
          if( (generatedGetter != null || generatedSetter != null) &&
            isInterface( classDecl ) && isStatic( classDecl, tree ) )
          {
            reportError( tree, MSG_INTERFACE_FIELD_BACKED_PROPERTY_NOT_SUPPORTED.get( tree.name ) );
          }
          else
          {
            // add the generated accessors

            if( generatedGetter != null )
            {
              pair.snd.add( generatedGetter );
            }
            if( generatedSetter != null )
            {
              pair.snd.add( generatedSetter );
            }
          }
        }
        else
        {
          // remove @prop etc. to treat it as a raw field

          ArrayList<JCAnnotation> annos = new ArrayList<>( tree.getModifiers().getAnnotations() );
          annos.remove( prop );
          annos.remove( get );
          annos.remove( set );
          tree.getModifiers().annotations = List.from( annos );
        }
      }
    }

    //  @propgen(name = "foo", 1)
    //  public String getFoo() {
    //    return this.foo;
    //  }
    private JCMethodDecl makeGetter( JCClassDecl classDecl, JCVariableDecl propField,
                                     boolean propAbstract, boolean propFinal, PropOption propAccess, JCAnnotation anno )
    {
      Context context = _javacTask.getContext();
      TreeMaker make = TreeMaker.instance( context );
      long flags = propField.getModifiers().flags;
      JCAnnotation propgenAnno = makePropGenAnnotation( propField );
      List<JCAnnotation> annos = List.of( propgenAnno );
      JCModifiers access = getGetterSetterModifiers( make, propAbstract, propFinal, isStatic( classDecl, propField ),
        propAccess, (int)flags, annos, propField.pos );
      Name name = Names.instance( context ).fromString( getGetterName( propField, true ) );
      JCExpression resType = (JCExpression)propField.vartype.clone();
      JCReturn ret = make.Return( make.Ident( propField.name ).setPos( propField.pos ) );
      JCBlock block = propAbstract ? null : (JCBlock)make.Block( 0, List.of( ret ) ).setPos( propField.pos );

      JCMethodDecl getter = (JCMethodDecl)make.MethodDef(
        access, name, resType, List.nil(), List.nil(), List.nil(), block, null ).setPos( propField.pos );
      JCMethodDecl existingGetter = findExistsingAccessor( propField, classDecl, getter );
      if( existingGetter != null )
      {
        addAnnotations( existingGetter, List.of( propgenAnno ) );
        addAnnotations( existingGetter, getAnnotations( anno, "annos" ) );
        return null;
      }
      else if( isInterface( classDecl ) && isStatic( classDecl, propField ) && !isFinal( classDecl, propField ) )
      {
        // interface: static non-final property MUST provide user-defined getter
        reportError( propField, MSG_MISSING_INTERFACE_STATIC_PROPERTY_ACCESSOR.get(
          classDecl.name, name + "() : " + propField.vartype.toString(), propField.name ) );
      }
      else
      {
        addAnnotations( getter, getAnnotations( anno, "annos" ) );
      }
      return getter;
    }

    //  @propgen(name = "foo", 1)
    //  public Thing setFoo(String value) {
    //    this.foo = value;
    //    return this;
    //  }
    private JCMethodDecl makeSetter( JCClassDecl classDecl, JCVariableDecl propField,
                                     boolean propAbstract, boolean propFinal, PropOption propAccess, JCAnnotation anno )
    {
      Context context = _javacTask.getContext();
      TreeMaker make = TreeMaker.instance( context );
      long flags = propField.getModifiers().flags;
      JCAnnotation propgenAnno = makePropGenAnnotation( propField );
      List<JCAnnotation> annos = List.of( propgenAnno );
      JCModifiers access = getGetterSetterModifiers( make, propAbstract, propFinal, isStatic( classDecl, propField ),
        propAccess, (int)flags, annos, propField.pos );
      Names names = Names.instance( context );
      Name name = names.fromString( getSetterName( propField.name ) );
      JCVariableDecl param = (JCVariableDecl)make.VarDef(
        make.Modifiers( FINAL | Flags.PARAMETER ), names.fromString( "value" ),
        (JCExpression)propField.vartype.clone(), null ).setPos( propField.pos );
      JCExpression resType = make.Type( Symtab.instance( context ).voidType ).setPos( propField.pos );
      JCExpressionStatement assign = (JCExpressionStatement)make.Exec( make.Assign(
        make.Ident( propField.name ).setPos( propField.pos ),
        make.Ident( names.fromString( "value" ) ).setPos( propField.pos ) ).setPos( propField.pos ) )
        .setPos( propField.pos );
      JCBlock block = propAbstract ? null : (JCBlock)make.Block( 0, List.of( assign ) ).setPos( propField.pos );
      JCMethodDecl setter = (JCMethodDecl)make.MethodDef(
        access, name, resType, List.nil(), List.of( param ), List.nil(), block, null )
        .setPos( propField.pos );
      JCMethodDecl existingSetter = findExistsingAccessor( propField, classDecl, setter );
      if( existingSetter != null )
      {
        addAnnotations( existingSetter, List.of( propgenAnno ) );
        return null;
      }
      else if( isInterface( classDecl ) && isStatic( classDecl, propField ) && !isFinal( classDecl, propField ) )
      {
        // interface: static non-final property MUST provide user-defined setter
        reportError( propField, MSG_MISSING_INTERFACE_STATIC_PROPERTY_ACCESSOR.get(
          classDecl.name, name + "(" + propField.vartype.toString() + ")", propField.name ) );
      }
      else
      {
        addAnnotations( setter, getAnnotations( anno, "annos" ) );
        addAnnotations( param, getAnnotations( anno, "param" ) );
      }
      return setter;
    }

    private JCMethodDecl findExistsingAccessor( JCVariableDecl propField, JCClassDecl classDecl, JCMethodDecl accessor )
    {
      outer:
      for( JCTree def: classDecl.defs )
      {
        if( !(def instanceof JCMethodDecl) )
        {
          continue;
        }
        JCMethodDecl tree = (JCMethodDecl)def;
        if( accessor.name == tree.name &&
          accessor.params.length() == tree.params.length() )
        {
          List<JCVariableDecl> accessorParams = accessor.params;
          List<JCVariableDecl> treeParams = tree.params;
          for( int i = 0; i < accessor.params.size(); i++ )
          {
            JCVariableDecl accessorParam = accessorParams.get( i );
            JCVariableDecl treeParam = treeParams.get( i );
            if( !isSameType( tree, propField.getName(), accessorParam.vartype.toString(), treeParam.vartype.toString() ) )
            {
              continue outer;
            }
          }
          // method already exists
          return tree;
        }
      }
      return null;
    }

    private boolean isSameType( JCMethodDecl tree, Name propName, String expected, String found )
    {
      if( expected.equals( found ) )
      {
        return true;
      }
      else if( ghettoErasure( expected ).equals( ghettoErasure( found ) ) )
      {
        // We have to match the erasure of the setter parameter due to Java's generic type erasure, so warn about that
        reportWarning( tree, MSG_SETTER_TYPE_CONFLICT.get( found, propName, expected ) );
        return true;
      }
      return false;
    }

    private String ghettoErasure( String type )
    {
      int iOpen = type.indexOf( '<' );
      if( iOpen < 0 )
      {
        return type;
      }
      String erasedType = type.substring( 0, iOpen );
      int iClose = type.lastIndexOf( '>' );
      if( iClose < 0 )
      {
        return erasedType;
      }
      if( iClose < type.length()-1 )
      {
        erasedType += type.substring( iClose+1 );
        if( !erasedType.endsWith( "[]" ) )
        {
          throw new IllegalStateException( "Expecting an array type" );
        }
      }
      return erasedType;
    }

    private void addAnnotations( JCMethodDecl accessor, List<JCAnnotation> propgenAnno )
    {
      ArrayList<JCAnnotation> newAnnos = new ArrayList<>( accessor.getModifiers().annotations );
      newAnnos.addAll( propgenAnno );
      accessor.getModifiers().annotations = List.from( newAnnos );
    }
    private void addAnnotations( JCVariableDecl setterParam, List<JCAnnotation> propgenAnno )
    {
      ArrayList<JCAnnotation> newAnnos = new ArrayList<>( setterParam.getModifiers().annotations );
      newAnnos.addAll( propgenAnno );
      setterParam.getModifiers().annotations = List.from( newAnnos );
    }

    private List<JCAnnotation> getAnnotations( JCAnnotation anno, String target )
    {
      for( JCExpression arg: anno.args )
      {
        if( arg instanceof JCAssign )
        {
          JCAssign assign = (JCAssign)arg;
          if( assign.lhs.toString().equals( target ) )
          {
            if( assign.rhs instanceof JCAnnotation )
            {
              return List.of( (JCAnnotation)assign.rhs );
            }
            else if( assign.rhs instanceof JCNewArray )
            {
              //noinspection unchecked
              return List.from( (List)((JCNewArray)assign.rhs).elems );
            }
          }
        }
      }
      return List.nil();
    }

    private JCModifiers getGetterSetterModifiers( TreeMaker make, boolean propAbstract, boolean propFinal, boolean propStatic,
                                                  PropOption propAccess, int flags, List<JCAnnotation> annos, int pos )
    {
      int access = propAccess == null
        ? isPublic( flags ) ? PUBLIC : isProtected( flags ) ? PROTECTED : isPrivate( flags ) ? PRIVATE : 0
        : propAccess.getModifier();
      access |= (propAbstract ? ABSTRACT : 0);
      access |= (propFinal ? FINAL : 0);
      access |= (propStatic ? STATIC : 0);
      return (JCModifiers)make.Modifiers( access, annos ).setPos( pos );
    }

    private JCAnnotation makePropGenAnnotation( JCVariableDecl field )
    {
      JavacPlugin javacPlugin = JavacPlugin.instance();
      TreeMaker make = javacPlugin.getTreeMaker();
      JavacElements javacElems = javacPlugin.getJavacElements();
      Names names = Names.instance( javacPlugin.getContext() );

      ArrayList<JCAssign> args = new ArrayList<>();
      args.add( make.Assign( make.Ident( names.fromString( "name" ) ), make.Literal( field.name.toString() ) ) );
      args.add( make.Assign( make.Ident( names.fromString( "flags" ) ), make.Literal( field.getModifiers().flags ) ) );
      // add args for prop, get, and set
      field.getModifiers().getAnnotations().stream().filter(
        e -> prop.class.getSimpleName().equals( e.annotationType.toString() ) )
        .findFirst().ifPresent( anno -> args.add( make.Assign( make.Ident( names.fromString( "prop" ) ), anno ) ) );
      field.getModifiers().getAnnotations().stream().filter(
        e -> get.class.getSimpleName().equals( e.annotationType.toString() ) )
        .findFirst().ifPresent( anno -> args.add( make.Assign( make.Ident( names.fromString( "get" ) ), anno ) ) );
      field.getModifiers().getAnnotations().stream().filter(
        e -> set.class.getSimpleName().equals( e.annotationType.toString() ) )
        .findFirst().ifPresent( anno -> args.add( make.Assign( make.Ident( names.fromString( "set" ) ), anno ) ) );
      JCExpression propgenType = memberAccess( make, javacElems, propgen.class.getName() );
      return make.Annotation( propgenType, List.from( args ) );
    }

    private JCTree.JCExpression memberAccess( TreeMaker make, JavacElements javacElems, String path )
    {
      return memberAccess( make, javacElems, path.split( "\\." ) );
    }

    private JCTree.JCExpression memberAccess( TreeMaker make, JavacElements node, String... components )
    {
      JCTree.JCExpression expr = make.Ident( node.getName( components[0] ) );
      for( int i = 1; i < components.length; i++ )
      {
        expr = make.Select( expr, node.getName( components[i] ) );
      }
      return expr;
    }

    // does the accessor method have weaker access than the prop field?
    private boolean isWeakerAccess( PropOption accessorOpt, int propAccess )
    {
      if( accessorOpt == null )
      {
        return false;
      }
      int accessorAccess = accessorOpt.getModifier();
      return accessorAccess == PUBLIC && propAccess != PUBLIC ||
        accessorAccess == PROTECTED && (propAccess == 0 || propAccess == PRIVATE) ||
        accessorAccess == 0 && propAccess == PRIVATE;
    }
  }

  private boolean isInterface( JCClassDecl classDecl )
  {
    return classDecl.getKind() == Tree.Kind.INTERFACE;
  }

  // Remove FINAL modifier from property fields in interfaces to enable assignment
  // (note these fields do not exist in bytecode)
  class Enter_Finish extends TreeTranslator
  {
    private Stack<JCClassDecl> _classes = new Stack<>();

    @Override
    public void visitClassDef( JCClassDecl classDecl )
    {
      _classes.push( classDecl );
      try
      {
        super.visitClassDef( classDecl );
      }
      finally
      {
        _classes.pop();
      }
    }

    @Override
    public void visitVarDef( JCVariableDecl tree )
    {
      super.visitVarDef( tree );

      if( !isPropertyField( tree.sym ) )
      {
        return;
      }

      verifyPropertyMethodsAgree( tree );

      JCClassDecl cls = _classes.peek();
      if( isInterface( cls ) )
      {
        // Remove FINAL modifier from property fields in interfaces to enable assignment
        // (note these fields do not exist in bytecode)
        tree.sym.flags_field &= ~FINAL;
      }
    }

    private void verifyPropertyMethodsAgree( JCVariableDecl varDecl )
    {
      JCAnnotation prop = getAnnotation( varDecl, manifold.ext.props.rt.api.prop.class );
      JCAnnotation get = getAnnotation( varDecl, manifold.ext.props.rt.api.get.class );
      JCAnnotation set = getAnnotation( varDecl, manifold.ext.props.rt.api.set.class );

      if( prop == null && get == null && set == null )
      {
        // not a property field
        throw new IllegalStateException();
      }

      boolean[] finalErrorHanlded = {false};

      verifyGetter( varDecl, prop, get, set, finalErrorHanlded );
      verifySetter( varDecl, prop, get, set, finalErrorHanlded[0] );
    }

    private void verifyGetter( JCVariableDecl varDecl, JCAnnotation prop, JCAnnotation get, JCAnnotation set,
                               boolean[] finalErrorHandled )
    {
      MethodSymbol getMethod = resolveGetMethod( varDecl.sym.owner.type, varDecl.sym );
      if( set == null || get != null )
      {
        // Property is defined to have 'get' access
        //

        JCClassDecl classDecl = _classes.peek();

        // check that the method and property are both static/non-static, otherwise issue compiler error
        getMethod = checkStatic( classDecl, varDecl, varDecl.sym, getMethod );
        if( getMethod == null )
        {
          return;
        }

        boolean getAbstract = isAbstract( classDecl, varDecl, get == null ? prop.args : get.args );
        boolean getFinal = hasOption( get == null ? prop.args : get.args, PropOption.Final );
        PropOption getAccess = PropertyProcessor.this.getAccess( classDecl, get == null ? prop.args : get.args );

        if( getAbstract != Modifier.isAbstract( (int)getMethod.flags() ) )
        {
          if( classDecl.getKind() != Tree.Kind.INTERFACE )
          {
            reportError( varDecl, MSG_PROPERTY_METHOD_CONFLICT.get(
                varDecl.sym.flatName(), getMethod.flatName(), "Abstract" ) );
          }
        }

        if( getFinal && isInterface( classDecl ) )
        {
          reportError( varDecl, MSG_FINAL_NOT_ALLOWED_IN_INTERFACE.get() );
          finalErrorHandled[0] = get == null;
        }

        if( getFinal != Modifier.isFinal( (int)getMethod.flags() ) )
        {
          reportError( varDecl, MSG_PROPERTY_METHOD_CONFLICT.get(
              varDecl.sym.flatName(), getMethod.flatName(), "Final" ) );
        }

        int accessModifier = getAccess == null
          ? getAccess( classDecl, (int)varDecl.getModifiers().flags )
          : getAccess.getModifier();

        if( (getMethod.flags() & accessModifier) != accessModifier )
        {
          reportError( varDecl, MSG_PROPERTY_METHOD_CONFLICT.get( varDecl.sym.flatName(),
            getMethod.flatName(), PropOption.fromModifier( accessModifier ).name() ) );
        }
      }
      else if( getMethod != null ) // just @set
      {
        reportWarning( varDecl, MSG_GETTER_DEFINED_FOR_WRITEONLY.get(
            getMethod.flatName(), varDecl.sym.flatName() ) );
      }
    }

    private void verifySetter( JCVariableDecl varDecl, JCAnnotation prop, JCAnnotation get, JCAnnotation set,
                               boolean finalErrorHanlded )
    {
      JCClassDecl classDecl = _classes.peek();

      MethodSymbol setMethod = resolveSetMethod( varDecl.sym.owner.type, varDecl.sym,
        Types.instance( _javacTask.getContext() ) );

      if( setMethod != null && isFinal( classDecl, varDecl ) )
      {
        reportWarning( varDecl, MSG_SETTER_DEFINED_FOR_FINAL_PROPERTY.get(
          setMethod.flatName(), varDecl.name ) );
      }

      if( get == null || set != null )
      {
        // Property is defined to have 'set' access
        //

        if( setMethod != null )
        {
          // check that the method and property are both static/non-static, otherwise issue compiler error
          setMethod = checkStatic( classDecl, varDecl, varDecl.sym, setMethod );
          if( setMethod == null )
          {
            return;
          }
        }

        boolean setAbstract = isAbstract( classDecl, varDecl, set == null ? prop.args : set.args );
        boolean setFinal = hasOption( set == null ? prop.args : set.args, PropOption.Final );
        PropOption setAccess = PropertyProcessor.this.getAccess( classDecl, set == null ? prop.args : set.args );
        boolean setGenerated = getAnnotation( varDecl, propgen.class ) != null;

        if( setMethod == null && !isFinal( classDecl, varDecl ) )
        {
          throw new IllegalStateException( "Setter should exist, if not user-defined, it should have been generated" );
        }

        if( setMethod != null && setAbstract != Modifier.isAbstract( (int)setMethod.flags() ) )
        {
          if( classDecl.getKind() != Tree.Kind.INTERFACE )
          {
            reportError( varDecl, MSG_PROPERTY_METHOD_CONFLICT.get(
                varDecl.sym.flatName(), setMethod.flatName(), "Abstract" ) );
          }
        }

        if( !finalErrorHanlded && setFinal && isInterface( classDecl ) )
        {
          reportError( varDecl, MSG_FINAL_NOT_ALLOWED_IN_INTERFACE.get() );
        }

        if( setMethod != null && setFinal != Modifier.isFinal( (int)setMethod.flags() ) )
        {
          if( setGenerated )
          {
            throw new IllegalStateException( "generated method should match property" );
          }

          reportError( varDecl, MSG_PROPERTY_METHOD_CONFLICT.get(
              varDecl.sym.flatName(), setMethod.flatName(), "Final" ) );
        }

        int accessModifier = setAccess == null
          ? getAccess( classDecl, (int)varDecl.getModifiers().flags )
          : setAccess.getModifier();

        if( setMethod != null && (setMethod.flags() & accessModifier) != accessModifier )
        {
          if( setGenerated )
          {
            throw new IllegalStateException( "generated method should match property" );
          }

          reportError( varDecl, MSG_PROPERTY_METHOD_CONFLICT.get( varDecl.sym.flatName(),
            setMethod.flatName(), PropOption.fromModifier( accessModifier ).name() ) );
        }
      }
      else if( setMethod != null ) // just @set
      {
        reportWarning( varDecl, MSG_SETTER_DEFINED_FOR_READONLY.get(
            setMethod.flatName(), varDecl.sym.flatName() ) );
      }
    }

    private int getAccess( JCClassDecl classDecl, int flags )
    {
      return isPublic( flags )
        ? PUBLIC
        : isProtected( flags )
          ? PROTECTED
          : isPrivate( flags )
            ? PRIVATE
            : isInterface( classDecl ) ? PUBLIC : 0;
    }

    private MethodSymbol checkStatic( JCClassDecl classDecl, JCVariableDecl propDecl, Symbol field, MethodSymbol method )
    {
      if( method == null )
      {
        return null;
      }

      boolean isInterface = isInterface( classDecl );
      boolean isPropStatic = isInterface
        ? propDecl.init != null || ((int)propDecl.getModifiers().flags & STATIC) != 0
        : ((int)field.flags_field & STATIC) != 0;
      boolean isMethodStatic = ((int)method.flags_field & STATIC) != 0;
      if( isPropStatic != isMethodStatic )
      {
        if( isMethodStatic )
        {
          reportError( propDecl, MSG_STATIC_MISMATCH.get( method.name, field.name ) );
        }
        else
        {
          reportError( propDecl, MSG_NONSTATIC_MISMATCH.get( method.name, field.name ) );
        }
        method = null;
      }
      return method;
    }
  }

  // Replace field refs with getter/setter calls:
  //
  // foo.bar          ==>  foo.getBar()
  // bar              ==>  this.getBar()
  // foo.bar = value  ==>  foo.setBar(value)
  // bar = value      ==>  this.setBar(value)
  //
  class Analyze_Finish extends TreeTranslator
  {
    private Stack<JCVariableDecl> _propDefs = new Stack<>();
    private Stack<JCMethodDecl> _methodDefs = new Stack<>();
    private Stack<Pair<JCClassDecl, Set<VarSymbol>>> _backingSymbols = new Stack<>();

    @Override
    public void visitClassDef( JCClassDecl classDecl )
    {
      _backingSymbols.push( new Pair<>( classDecl, new HashSet<>() ) );
      try
      {
        super.visitClassDef( classDecl );
        Set<VarSymbol> props = _backingMap.computeIfAbsent( classDecl.sym, e -> new HashSet<>() );
        props.addAll( _backingSymbols.peek().snd );
      }
      finally
      {
        _backingSymbols.pop();
      }
    }

    @Override
    public void visitVarDef( JCVariableDecl tree )
    {
      boolean isProp = isPropertyField( tree.sym );
      if( isProp )
      {
        _propDefs.push( tree );
      }
      try
      {
        super.visitVarDef( tree );
      }
      finally
      {
        if( isProp )
        {
          _propDefs.pop();
        }
      }
    }

    @Override
    public void visitMethodDef( JCMethodDecl tree )
    {
      _methodDefs.push( tree );
      try
      {
        super.visitMethodDef( tree );
      }
      finally
      {
        _methodDefs.pop();
      }
    }

    @Override
    public void visitSelect( JCFieldAccess tree )
    {
      super.visitSelect( tree );

      if( !isPropertyField( tree.sym ) )
      {
        return;
      }

      // don't process here if the field access is an l-value
      //
      Tree parent = _tp.getParent( tree );
      if( parent instanceof JCTree.JCAssign && ((JCTree.JCAssign)parent).lhs == tree )
      {
        return;
      }
      if( parent instanceof JCTree.JCAssignOp && ((JCTree.JCAssignOp)parent).lhs == tree )
      {
        return;
      }
      if( parent instanceof JCTree.JCUnary )
      {
        switch( ((JCTree.JCUnary)parent).getTag() )
        {
          case POSTDEC:
          case POSTINC:
          case PREDEC:
          case PREINC:
            return;
        }
      }

      // replace foo.bar with foo.getBar()
      //
      MethodSymbol getMethod = isReadableProperty( tree.sym )
        ? resolveGetMethod( tree.selected.type, tree.sym )
        : null;

      if( getMethod != null )
      {
        JCMethodDecl methodDecl = _methodDefs.peek();
        if( methodDecl != null && methodDecl.sym == getMethod )
        {
          // don't rewrite with getter inside the getter
          _backingSymbols.peek().snd.add( (VarSymbol)tree.sym );
          return;
        }

        if( !verifyAccess( tree, tree.sym, getMethod, "Read" ) )
        {
          // the getter is not accessible from the use site
          return;
        }

        TreeMaker make = _tp.getTreeMaker();

        JCTree.JCMethodInvocation methodCall;
        JCExpression receiver = tree.selected;
        methodCall = make.Apply( List.nil(), make.Select( receiver, getMethod ), List.nil() );
        methodCall = configMethod( tree, getMethod, methodCall );

        result = methodCall;
      }
      else
      {
        reportError( tree, MSG_CANNOT_ACCESS_WRITEONLY_PROPERTY.get( tree.sym.flatName() ) );
      }
    }

    @Override
    public void visitIdent( JCIdent tree )
    {
      super.visitIdent( tree );

      if( !isPropertyField( tree.sym ) )
      {
        return;
      }

      // don't process here if ident is an l-value
      //
      Tree parent = _tp.getParent( tree );
      if( parent instanceof JCTree.JCAssign && ((JCTree.JCAssign)parent).lhs == tree )
      {
        return;
      }
      if( parent instanceof JCTree.JCAssignOp && ((JCTree.JCAssignOp)parent).lhs == tree )
      {
        return;
      }
      if( parent instanceof JCTree.JCUnary )
      {
        switch( ((JCTree.JCUnary)parent).getTag() )
        {
          case POSTDEC:
          case POSTINC:
          case PREDEC:
          case PREINC:
            return;
        }
      }

      // replace bar with this.getBar()
      //
      MethodSymbol getMethod = isReadableProperty( tree.sym )
        ? resolveGetMethod( _backingSymbols.peek().fst.type, tree.sym )
        : null;
      if( getMethod != null )
      {
        JCMethodDecl methodDecl = _methodDefs.peek();
        if( methodDecl != null && methodDecl.sym == getMethod )
        {
          // - don't rewrite with getter inside the getter
          // - backing symbol required, add to set of backing symbols
          _backingSymbols.peek().snd.add( (VarSymbol)tree.sym );

          if( _methodDefs.peek().sym.isDefault() )
          {
            // Cannot reference property in default interface accessor
            reportError( tree, MSG_PROPERTY_IS_ABSTRACT.get( tree.sym.flatName() ) );
          }
          
          return;
        }

        if( !verifyAccess( tree, tree.sym, getMethod, "Read" ) )
        {
          // the getter is not accessible from the use site
          return;
        }

        TreeMaker make = _tp.getTreeMaker();

        JCTree.JCMethodInvocation methodCall;
        JCExpression receiver = tree.sym.isStatic()
          ? make.Type( tree.sym.owner.type )
          : make.This( _backingSymbols.peek().fst.type ).setPos( tree.pos );
        methodCall = make.Apply( List.nil(), make.Select( receiver, getMethod ).setPos( tree.pos ), List.nil() );
        methodCall = configMethod( tree, getMethod, methodCall );

        result = methodCall;
      }
      else
      {
        reportError( tree, MSG_CANNOT_ACCESS_WRITEONLY_PROPERTY.get( tree.sym.flatName() ) );
      }
    }

    @Override
    public void visitExec( JCExpressionStatement tree )
    {
      super.visitExec( tree );

      handlePropertyAssignment( tree );
    }

    private void handlePropertyAssignment( JCExpressionStatement t )
    {
      JCExpression expr = t.expr;
      if( !(expr instanceof JCAssign) )
      {
        return;
      }

      TreeMaker make = _tp.getTreeMaker();

      JCAssign tree = (JCAssign)expr;

      JCExpression lhs;
      Type lhsSelectedType;
      Symbol lhsSym;
      JCExpression lhsSelected;
      if( tree.lhs instanceof JCTree.JCFieldAccess )
      {
        JCFieldAccess fieldAccess = (JCTree.JCFieldAccess)tree.lhs;
        lhs = fieldAccess;
        lhsSelectedType = fieldAccess.selected.type;
        lhsSym = fieldAccess.sym;
        lhsSelected = fieldAccess.selected;
      }
      else if( tree.lhs instanceof JCIdent && ((JCIdent)tree.lhs).sym.owner instanceof ClassSymbol )
      {
        JCIdent ident = (JCIdent)tree.lhs;
        lhs = ident;
        lhsSelectedType = _backingSymbols.peek().fst.type;
        lhsSym = ident.sym;
        lhsSelected = lhsSym.isStatic()
          ? make.Type( lhsSym.owner.type )
          : make.This( lhsSelectedType ).setPos( t.pos );
      }
      else
      {
        return;
      }

      if( !isPropertyField( lhsSym ) )
      {
        return;
      }

      JCMethodDecl methodDecl = _methodDefs.peek();

      // replace  foo.bar = baz  with  foo.setBar(baz)

      Context ctx = _javacTask.getContext();

      MethodSymbol setMethod = isWritableProperty( lhsSym )
        ? resolveSetMethod( lhsSelectedType, lhsSym, Types.instance( ctx ) )
        : null;

      if( setMethod != null )
      {
        if( methodDecl != null && methodDecl.sym == setMethod )
        {
          // - don't rewrite with setter inside the setter
          // - backing symbol required, add to set of backing symbols
          _backingSymbols.peek().snd.add( (VarSymbol)lhsSym );
          return;
        }

        if( !verifyAccess( tree, lhsSym, setMethod, "Write" ) )
        {
          // the setter is not accessible from the use site
          return;
        }

        JCExpression rhs = tree.rhs;

//        tempVarIndex++;
//        List<JCTree.JCVariableDecl> tempVars = List.nil();
//        JCTree[] rhsTemp = ExtensionTransformer.tempify( false, tree, make, rhs, ctx,
//          ExtensionTransformer.getEnclosingSymbol( tree, ctx, _tp ), "setPropRhsTempVar" + tempVarIndex, tempVarIndex );
//        if( rhsTemp != null )
//        {
//          tempVars = tempVars.append( (JCTree.JCVariableDecl)rhsTemp[0] );
//          rhs = (JCExpression)rhsTemp[1];
//        }

        JCTree.JCMethodInvocation setCall;
        Type parameterizedMethod = _tp.getTypes().memberType( lhsSelectedType, setMethod );
        while( parameterizedMethod instanceof Type.ForAll )
        {
          parameterizedMethod = parameterizedMethod.asMethodType();
        }

        setCall = make.Apply( List.nil(), make.Select( lhsSelected, setMethod ).setPos( t.pos ), List.of( rhs ) );
        setCall = configMethod( lhs, setMethod, setCall );
        t.expr = setCall;
//## todo: this does not work if the setXxx() call return void because the LetExpr only lets us have temp var assign stmts
//
//      JCTree[] setCallTemp = ExtensionTransformer.tempify( true, tree, make, setCall, ctx,
//        ExtensionTransformer.getEnclosingSymbol( tree, ctx, _tp ), "$setPropCallTempVar" + tempVarIndex, tempVarIndex );
//      //noinspection ConstantConditions
//      tempVars = tempVars.append( (JCTree.JCVariableDecl)setCallTemp[0] );
//
//      // Need let expr so that we can return the RHS value as required by java assignment op.
//      // Note, the setXxx() method can return whatever it wants, it is ignored here,
//      // this allows us to support eg. List.set(int, T) where this method returns the previous value
//      JCTree.LetExpr letExpr = (JCTree.LetExpr)ReflectUtil.method( make, "LetExpr",
//        List.class, JreUtil.isJava8() ? JCTree.class : JCExpression.class )
//        .invoke( tempVars, rhs );
//      letExpr.type = rhs.type;
//
//      result = letExpr;
      }
      else
      {
        if( !_propDefs.isEmpty() && _propDefs.peek().sym == lhsSym )
        {
          // - no setter, allow the property to initialize in its declaration
          // - backing symbol required, add to set of backing symbols
          _backingSymbols.peek().snd.add( (VarSymbol)lhsSym );
        }
        else if( methodDecl.sym.isConstructor() &&
          lhsSelected.toString().equals( "this" ) &&
          lhsSym.owner == _backingSymbols.peek().fst.sym )
        {
          // - no setter, allow the read-only (final or non-final) property to initialize in its constructor
          // - backing symbol required, add to set of backing symbols
          _backingSymbols.peek().snd.add( (VarSymbol)lhsSym );
        }
        else if( !Modifier.isFinal( (int)lhsSym.flags_field ) &&
          _backingSymbols.peek().fst.sym.outermostClass() == lhsSym.outermostClass() )
        {
          // no setter, allow the read-only (non-final) prop field to be directly assigned inside the class file
          // - backing symbol required, add to set of backing symbols
          _backingSymbols.peek().snd.add( (VarSymbol)lhsSym );
        }
        else
        {
          reportError( tree, MSG_CANNOT_ASSIGN_READONLY_PROPERTY.get( lhsSym.flatName() ) );
        }
      }
    }

    private boolean verifyAccess( JCExpression ref, Symbol propSym, MethodSymbol accessorMethod, String accessKind )
    {
      if( !sameAccess( accessorMethod, propSym ) )
      {
        JCClassDecl classDecl = _backingSymbols.peek().fst;
        Resolve resolve = Resolve.instance( _javacTask.getContext() );
        AttrContext attrContext = new AttrContext();
        Env<AttrContext> env = new AttrContextEnv( ref, attrContext );
        env.toplevel = (JCCompilationUnit)_tp.getCompilationUnit();
        env.enclClass = classDecl;

        if( !resolve.isAccessible( env, classDecl.type, accessorMethod ) )
        {
          reportError( ref, MSG_PROPERTY_NOT_ACCESSIBLE.get( accessKind, propSym.flatName(),
            PropOption.fromModifier( getAccess( accessorMethod ) ).name().toLowerCase() ) );
          return false;
        }
      }
      return true;
    }

    private JCTree.JCMethodInvocation configMethod( JCTree.JCExpression tree, MethodSymbol methodSym, JCTree.JCMethodInvocation methodTree )
    {
      methodTree.setPos( tree.pos );
      methodTree.type = methodSym.getReturnType();

      // If methodCall is an extension method, rewrite it
      //noinspection ConstantConditions
      methodTree = _extensionTransformer.get().maybeReplaceWithExtensionMethod( methodTree );

      // Concrete type set in attr
      methodTree.type = tree.type;
      return methodTree;
    }
  }

  // Make the field PRIVATE, or delete it if no backing field is necessary
  // If a backing field, add annotation:  @propgen(name, flags), so we can change it when loaded from .class file
  // Note, @propgen is also added to getter/setter methods so that a non-backing field can be recreated on .class load
  class Generate_Start
  {
    private void handleClass( ClassSymbol classSym )
    {
      IDynamicJdk.instance().getMembers( classSym, e -> e instanceof ClassSymbol, false )
        .forEach( c -> handleClass( (ClassSymbol)c ) );

      IDynamicJdk.instance().getMembers( classSym, e -> e instanceof VarSymbol, false )
        .forEach( varSym -> handleField( classSym, (VarSymbol)varSym ) );
    }

    private void handleField( ClassSymbol classSym, VarSymbol fieldSym )
    {
      long modifiers = fieldSym.flags_field;

      if( !isPropertyField( fieldSym ) )
      {
        return;
      }

      // if the field is a backing field, make it PRIVATE and tag it with @propgen, then make it public again during Generate:finish, or
      // erase it and put it back during Generate:finish

      // remove the prop field here if a backing field is not needed e.g., where the getter/setter methods don't ref it
      //
      Set<VarSymbol> backingSymbols = _backingMap.get( classSym );
      if( backingSymbols.contains( fieldSym ) )
      {
        // make the field a backing field with PRIVATE access

        fieldSym.flags_field = fieldSym.flags_field & ~(PUBLIC | PROTECTED) | PRIVATE;

        // store the original access modifier in @propgen(name, flags) so we can restore the field's access upon
        // loading from .class file

        Names names = Names.instance( _javacTask.getContext() );
        Symtab symtab = Symtab.instance( _javacTask.getContext() );
        ClassSymbol propgenSym = IDynamicJdk.instance().getTypeElement( _javacTask.getContext(),
          _tp.getCompilationUnit(), propgen.class.getTypeName() );
        MethodSymbol nameMeth = (MethodSymbol)IDynamicJdk.instance().getMembersByName( propgenSym, names.fromString( "name" ) ).iterator().next();
        MethodSymbol flagsMeth = (MethodSymbol)IDynamicJdk.instance().getMembersByName( propgenSym, names.fromString( "flags" ) ).iterator().next();
        Attribute.Compound propGenAnno = new Attribute.Compound( propgenSym.type,
          List.of( new Pair<>( nameMeth, new Attribute.Constant( symtab.stringType, fieldSym.name.toString() ) ),
            new Pair<>( flagsMeth, new Attribute.Constant( symtab.longType, modifiers ) ) ) );
        fieldSym.appendAttributes( List.of( propGenAnno ) );

        Set<VarSymbol> props = _propMap.computeIfAbsent( classSym, e -> new HashSet<>() );
        props.add( fieldSym );
      }
      else
      {
        // erase the field, put it back on Generate:finish

        // reflectively call: classSym.members().remove( fieldSym );
        ReflectUtil.method(
          ReflectUtil.method( classSym, "members" )
            .invoke(), "remove", Symbol.class )
          .invoke( fieldSym );

        Set<VarSymbol> nonbacking = _nonbackingMap.computeIfAbsent( classSym, e -> new HashSet<>() );
        nonbacking.add( fieldSym );
      }
    }
  }

  class Generate_Finish
  {
    public void handleClass( ClassSymbol classSym )
    {
      IDynamicJdk.instance().getMembers( classSym, e -> e instanceof ClassSymbol, false )
        .forEach( c -> handleClass( (ClassSymbol)c ) );

      // handle backing fields
      //

      // put original modifiers back e.g. PUBLIC
      Set<VarSymbol> backingFields = _propMap.get( classSym );
      if( backingFields != null )
      {
        for( VarSymbol varSym : backingFields )
        {
          Attribute.Compound propgenAnno = getAnnotationMirror( varSym, propgen.class );
          if( propgenAnno != null )
          {
            // remove PRIVATE, restore original access modifiers
            varSym.flags_field = varSym.flags_field & ~PRIVATE | getFlags( propgenAnno );
          }
        }
        _propMap.remove( classSym );
      }

      // handle non-backing fields, recreate the prop field
      //
      Set<VarSymbol> nonbackingFields = _nonbackingMap.get( classSym );
      if( nonbackingFields != null )
      {
        for( VarSymbol varSym : nonbackingFields )
        {
          Type t = varSym.type;
          VarSymbol sym = new VarSymbol( varSym.flags_field, varSym.name, t, classSym );
          sym.appendAttributes( varSym.getAnnotationMirrors() ); // add the @prop etc. annotations

          // reflectively call:  classSym.members_field.enter( sym );
          ReflectUtil.method( ReflectUtil.field( classSym, "members_field" ).get(),
            "enter", Symbol.class ).invoke( sym );
        }
      }
      _nonbackingMap.remove( classSym );
    }
  }

  private MethodSymbol resolveGetMethod( Type type, Symbol field )
  {
    Types types = _tp.getTypes();

    if( type instanceof Type.TypeVar )
    {
      type = types.erasure( type );
    }

    if( !(type.tsym instanceof ClassSymbol) )
    {
      return null;
    }

    MethodSymbol method = ManAttr.getMethodSymbol( types, type, field.type, getGetterName( field, true ), (ClassSymbol)type.tsym, 0 );
    if( method == null )
    {
      method = ManAttr.getMethodSymbol( types, type, field.type, getGetterName( field, false ), (ClassSymbol)type.tsym, 0 );
    }
    return method;
  }

  private MethodSymbol resolveSetMethod( Type type, Symbol field, Types types )
  {
    if( type instanceof Type.TypeVar )
    {
      type = types.erasure( type );
    }

    if( !(type.tsym instanceof ClassSymbol) )
    {
      return null;
    }

    return ManAttr.getMethodSymbol( types, type, field.type, getSetterName( field.name ), (ClassSymbol)type.tsym, 1 );
  }

  private boolean isAbstract( JCClassDecl classDecl, JCVariableDecl propField, List<JCExpression> args )
  {
    if( isInterface( classDecl ) && !isStatic( classDecl, propField ) && !isFinal( classDecl, propField ) )
    {
      // generated methods are always abstract in interfaces
      return true;
    }
    else if( Modifier.isAbstract( (int)classDecl.getModifiers().flags ) )
    {
      // abstract class can have abstract methods

      return hasOption( args, PropOption.Abstract );
    }
    return false;
  }

  private PropOption getAccess( JCClassDecl classDecl, List<JCExpression> args )
  {
    if( isInterface( classDecl ) )
    {
      // generated methods are always abstract in interfaces
      return PropOption.Public;
    }
    return hasOption( args, PropOption.Public )
      ? PropOption.Public
      : hasOption( args, PropOption.Protected )
      ? PropOption.Protected
      : hasOption( args, PropOption.Package )
      ? PropOption.Package
      : hasOption( args, PropOption.Private )
      ? PropOption.Private
      : null;
  }

  private boolean hasOption( List<JCExpression> args, PropOption option )
  {
    if( args == null )
    {
      return false;
    }
    return args.stream().anyMatch( e -> isOption( option, e ) );
  }

  private boolean isOption( PropOption option, JCExpression e )
  {
    if( e instanceof JCLiteral )
    {
      return ((JCLiteral)e).getValue() == option;
    }
    // whatever, it works
    return e.toString().contains( option.name() );
  }

  private JCAnnotation getAnnotation( JCVariableDecl tree, Class anno )
  {
    return tree.getModifiers().getAnnotations().stream()
      .filter( e -> anno.getSimpleName().equals( e.annotationType.toString() ) )
      .findFirst().orElse( null );
  }

  public boolean isPropertyField( Symbol sym )
  {
    return sym != null &&
      !sym.isLocal() &&
      (getAnnotationMirror( sym, prop.class ) != null ||
       getAnnotationMirror( sym, get.class ) != null ||
       getAnnotationMirror( sym, set.class ) != null);
  }

  public boolean isReadableProperty( Symbol sym )
  {
    return sym != null &&
      (getAnnotationMirror( sym, prop.class ) != null && getAnnotationMirror( sym, set.class ) == null ||
       getAnnotationMirror( sym, get.class ) != null);
  }
  public boolean isWritableProperty( Symbol sym )
  {
    return sym != null &&
      !Modifier.isFinal( (int)sym.flags_field ) &&
      (getAnnotationMirror( sym, prop.class ) != null && getAnnotationMirror( sym, get.class ) == null ||
       getAnnotationMirror( sym, set.class ) != null);
  }

  private boolean isStatic( JCClassDecl classDecl, JCVariableDecl propField )
  {
    boolean isInterface = isInterface( classDecl );
    long flags = propField.getModifiers().flags;
    return isInterface
      ? (propField.init != null) || (flags & STATIC) != 0
      : (flags & STATIC) != 0;
  }

  private boolean isFinal( JCClassDecl classDecl, JCVariableDecl propField )
  {
    boolean isInterface = isInterface( classDecl );
    long flags = propField.getModifiers().flags;
    return isInterface
      ? (propField.init != null)
      : (flags & FINAL) != 0;
  }

  private long getFlags( Attribute.Compound anno )
  {
    for( MethodSymbol methSym: anno.getElementValues().keySet() )
    {
      if( methSym.getSimpleName().toString().equals( "flags" ) )
      {
        return ((Number)anno.getElementValues().get( methSym ).getValue()).longValue();
      }
    }
    throw new IllegalStateException();
  }

  private Attribute.Compound getAnnotationMirror( Symbol sym, Class<? extends Annotation> annoClass )
  {
    for( Attribute.Compound anno: sym.getAnnotationMirrors() )
    {
      if( annoClass.getTypeName().equals( anno.type.tsym.getQualifiedName().toString() ) )
      {
        return anno;
      }
    }
    return null;
  }

  private boolean sameAccess( Symbol sym1, Symbol sym2 )
  {
    return sameAccess( (int)sym1.flags_field, (int)sym2.flags_field );
  }
  private boolean sameAccess( int flags1, int flags2 )
  {
    return getAccess( flags1 ) == getAccess( flags2 );
  }
  private int getAccess( Symbol sym )
  {
    return getAccess( (int)sym.flags_field );
  }
  private int getAccess( int flags )
  {
    return flags & (PUBLIC | PROTECTED | PRIVATE);
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
    _tp.report( _taskEvent.getSourceFile(), location, kind, message );
  }

  private String getGetterName( Symbol field, boolean isOk )
  {
    Symtab syms = Symtab.instance( _javacTask.getContext() );
    return (isOk && field.type == syms.booleanType
      ? "is"
      : "get") + ManStringUtil.capitalize( field.name.toString() );
  }

  private String getGetterName( JCVariableDecl tree, @SuppressWarnings( "SameParameterValue" ) boolean isOk )
  {
    return (isOk && tree.vartype.toString().equals( "boolean" )
      ? "is"
      : "get") + ManStringUtil.capitalize( tree.name.toString() );
  }

  private String getSetterName( Name name )
  {
    return "set" + ManStringUtil.capitalize( name.toString() );
  }
}
