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
import com.sun.tools.javac.comp.Annotate;
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
import manifold.ext.props.api.*;
import manifold.internal.javac.IDynamicJdk;
import manifold.internal.javac.JavacPlugin;
import manifold.internal.javac.ManAttr;
import manifold.internal.javac.TypeProcessor;
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.Stack;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.LocklessLazyVar;

import javax.tools.Diagnostic;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static com.sun.tools.javac.code.Flags.FINAL;
import static java.lang.reflect.Modifier.*;

public class PropertyProcessor implements ICompilerComponent, TaskListener
{
  private TypeProcessor _tp;
  private BasicJavacTask _javacTask;
  private Stack<Pair<JCClassDecl, ArrayList<JCTree>>> _propertyStatements;
  private Map<ClassSymbol, Set<VarSymbol>> _propMap;
  private Map<ClassSymbol, Set<VarSymbol>> _backingMap;
  private Map<ClassSymbol, Set<VarSymbol>> _nonbackingMap;
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
    ClassReader reader = ClassReader.instance( _javacTask.getContext() );
    ReflectUtil.LiveFieldRef thisCompleterField = ReflectUtil.field( reader, "thisCompleter" );
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
     * Restore the original access modifier to the property field.
     * Note the field is not really in the bytecode of the class.
     */
    private boolean restorePropFields( ClassSymbol classSym, Names names )
    {
      boolean handled = false;

      // Restore originally declared access on backing fields
      //
      for( Symbol sym: classSym.members_field.getElements() )
      {
        if( sym instanceof VarSymbol )
        {
          propgen anno = sym.getAnnotation( propgen.class );
          if( anno != null )
          {
            sym.flags_field = sym.flags_field & ~PRIVATE | anno.flags();
            handled = true;
          }
        }
      }

      // Recreate non-backing property fields based on @propgen annotations on corresponding getter/setter
      //
      for( Symbol sym: classSym.members_field.getElements() )
      {
        if( sym instanceof MethodSymbol )
        {
          Attribute.Compound anno = getPropGenAnnoMirror( sym );
          if( anno != null )
          {
            Name fieldName = names.fromString( getName( anno ) );
            Scope.Entry existingField = classSym.members_field.lookup( fieldName, e -> e instanceof VarSymbol );
            if( existingField.sym != null )
            {
              continue;
            }

            // Create and enter the prop field

            MethodSymbol meth = (MethodSymbol)sym;
            Type t = meth.getReturnType() == Symtab.instance( _javacTask.getContext() ).voidType
              ? meth.getParameters().get( 0 ).type
              : meth.getReturnType();
            VarSymbol propField = new VarSymbol( getFlags( anno ), fieldName, t, classSym );

            // add the @prop, @get, @set annotations
            propField.appendAttributes( List.from( anno.values.stream()
              .filter( e -> e.snd instanceof Attribute.Array )
              .map( e -> (Attribute.Compound)((Attribute.Array)e.snd).values[0] )
              .collect( Collectors.toList() ) ) );

            classSym.members_field.enter( propField );

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

    private Attribute.Compound getPropGenAnnoMirror( Symbol sym )
    {
      for( Attribute.Compound anno: sym.getAnnotationMirrors() )
      {
        if( propgen.class.getTypeName().equals( anno.type.tsym.getQualifiedName().toString() ) )
        {
          return anno;
        }
      }
      return null;
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

  @Override
  public void finished( TaskEvent e )
  {
    if( e.getKind() != TaskEvent.Kind.ENTER &&
        e.getKind() != TaskEvent.Kind.ANALYZE &&
        e.getKind() != TaskEvent.Kind.GENERATE )
    {
      return;
    }

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
        JCAnnotation prop = getAnnotation( tree, prop.class );
        JCAnnotation get = getAnnotation( tree, get.class );
        JCAnnotation set = getAnnotation( tree, set.class );

        if( prop == null && get == null && set == null )
        {
          // not a property field
          return;
        }

        if( classDecl.getKind() == Tree.Kind.INTERFACE && (modifiers & (PUBLIC|PROTECTED|PRIVATE)) == 0 )
        {
          // must explicitly default @prop fields to PUBLIC
          tree.getModifiers().flags |= PUBLIC;
        }

        // add getter and/or setter

        Pair<JCClassDecl, ArrayList<JCTree>> pair = _propertyStatements.peek();
        if( set == null || get != null )
        {
          boolean getAbstract = isAbstract( classDecl, get == null ? prop.args : get.args );
          boolean getFinal = hasOption( get == null ? prop.args : get.args, PropOption.Final );
          PropOption getAccess = getAccess( classDecl, get == null ? prop.args : get.args );

          JCMethodDecl getter = makeGetter( classDecl, tree, getAbstract, getFinal, getAccess );
          if( getter != null )
          {
            pair.snd.add( getter );
          }
        }
        if( (get == null || set != null) && !Modifier.isFinal( modifiers  ) )
        {
          boolean setAbstract = isAbstract( classDecl, set == null ? prop.args : set.args );
          boolean setFinal = hasOption( set == null ? prop.args : set.args, PropOption.Final );
          PropOption setAccess = getAccess( classDecl, set == null ? prop.args : set.args );

          JCMethodDecl setter = makeSetter( classDecl, tree, setAbstract, setFinal, setAccess );
          if( setter != null )
          {
            pair.snd.add( setter );
          }
        }
      }
    }

    private boolean isAbstract( JCClassDecl classDecl, List<JCExpression> args )
    {
      if( classDecl.getKind() == Tree.Kind.INTERFACE )
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
      if( classDecl.getKind() == Tree.Kind.INTERFACE )
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
      return args.stream().anyMatch( e -> ((JCLiteral)e).value == option );
    }

    private JCAnnotation getAnnotation( JCVariableDecl tree, Class anno )
    {
      return tree.getModifiers().getAnnotations().stream()
        .filter( e -> anno.getSimpleName().equals( e.annotationType.toString() ) )
        .findFirst().orElse( null );
    }

    //  @propgen(name = "foo", 1)
    //  public String getFoo() {
    //    return this.foo;
    //  }
    private JCMethodDecl makeGetter( JCClassDecl classDecl, JCVariableDecl propField,
                                     boolean propAbstract, boolean propFinal, PropOption propAccess )
    {
      Context context = _javacTask.getContext();
      TreeMaker make = TreeMaker.instance( context );
      long flags = propField.getModifiers().flags;
      List<JCAnnotation> annos = List.of( addPropGenAnnotation( propField ) );
      JCModifiers access = getGetterSetterModifiers( make, propAbstract, propFinal, (flags & STATIC) != 0,
        propAccess, (int)flags, annos, propField.pos );
      Name name = Names.instance( context ).fromString( getGetterName( propField, true ) );
      JCExpression resType = (JCExpression)propField.vartype.clone();
      JCReturn ret = make.Return( make.Ident( propField.name ).setPos( propField.pos ) );
      JCBlock block = propAbstract ? null : (JCBlock)make.Block( 0, List.of( ret ) ).setPos( propField.pos );

      JCMethodDecl getter = (JCMethodDecl)make.MethodDef(
        access, name, resType, List.nil(), List.nil(), List.nil(), block, null ).setPos( propField.pos );
      return exists( classDecl, getter ) ? null : getter;
    }

    //  @propgen(name = "foo", 1)
    //  public void setFoo(String value) {
    //    this.foo = value;
    //  }
    private JCMethodDecl makeSetter( JCClassDecl classDecl, JCVariableDecl propField,
                                     boolean propAbstract, boolean propFinal, PropOption propAccess )
    {
      Context context = _javacTask.getContext();
      TreeMaker make = TreeMaker.instance( context );
      long flags = propField.getModifiers().flags;
      List<JCAnnotation> annos = List.of( addPropGenAnnotation( propField ) );
      JCModifiers access = getGetterSetterModifiers( make, propAbstract, propFinal, (flags & STATIC) != 0,
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
      return exists( classDecl, setter ) ? null : setter;
    }

    private boolean exists( JCClassDecl classDecl, JCMethodDecl accessor )
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
          List<JCVariableDecl> treeParams = accessor.params;
          for( int i = 0; i < accessor.params.size(); i++ )
          {
            JCVariableDecl accessorParam = accessorParams.get( i );
            JCVariableDecl treeParam = treeParams.get( i );
            if( !accessorParam.vartype.toString().equals( treeParam.vartype.toString() ) )
            {
              //todo: more reliable type compare, maybe using erasure e.g., clip up to first '<', handle varargs, etc.
              continue outer;
            }
          }
          // method already exists
          return true;
        }
      }
      return false;
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

    private JCAnnotation addPropGenAnnotation( JCVariableDecl field )
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
  }

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

      JCClassDecl cls = _classes.peek();
      if( cls.getKind() != Tree.Kind.INTERFACE )
      {
        return;
      }

      if( isPropertyField( tree.sym ) )
      {
        return;
      }

      // Remove FINAL modifier from property fields in interfaces to enable assignment
      // (note these fields do not exist in bytecode)
      tree.sym.flags_field &= ~FINAL;
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

      if( isPropertyField( tree.sym ) )
      {
        return;
      }

      // replace foo.bar with foo.getBar()

      MethodSymbol getMethod = resolveGetMethod( tree.selected.type, tree.sym );

      if( getMethod != null )
      {
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

        JCMethodDecl methodDecl = _methodDefs.peek();
        if( methodDecl != null && methodDecl.sym == getMethod )
        {
          // don't rewrite with getter inside the getter
          _backingSymbols.peek().snd.add( (VarSymbol)tree.sym );
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
        _tp.report( tree, Diagnostic.Kind.ERROR,
          PropIssueMsg.MSG_CANNOT_ACCESS_PROPERTY.get( tree.sym.flatName().toString() ) );
      }
    }

    @Override
    public void visitIdent( JCIdent tree )
    {
      super.visitIdent( tree );

      if( isPropertyField( tree.sym ) )
      {
        return;
      }

      // replace with bar with this.getBar()

      MethodSymbol getMethod = resolveGetMethod( _backingSymbols.peek().fst.type, tree.sym );

      if( getMethod != null )
      {
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

        JCMethodDecl methodDecl = _methodDefs.peek();
        if( methodDecl != null && methodDecl.sym == getMethod )
        {
          // don't rewrite with getter inside the getter, backing symbol required
          _backingSymbols.peek().snd.add( (VarSymbol)tree.sym );

          if( _methodDefs.peek().sym.isDefault() )
          {
            // Cannot reference property in default interface accessor
            _tp.report( tree, Diagnostic.Kind.ERROR,
              PropIssueMsg.MSG_PROPERTY_IS_ABSTRACT.get( tree.sym.flatName().toString() ) );
          }
          
          return;
        }

        TreeMaker make = _tp.getTreeMaker();

        JCTree.JCMethodInvocation methodCall;
        JCExpression receiver = make.This( _backingSymbols.peek().fst.type ).setPos( tree.pos );
        methodCall = make.Apply( List.nil(), make.Select( receiver, getMethod ).setPos( tree.pos ), List.nil() );
        methodCall = configMethod( tree, getMethod, methodCall );

        result = methodCall;
      }
      else
      {
        _tp.report( tree, Diagnostic.Kind.ERROR,
          PropIssueMsg.MSG_CANNOT_ACCESS_PROPERTY.get( tree.sym.flatName().toString() ) );
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
        lhsSelected = make.This( lhsSelectedType ).setPos( t.pos );
      }
      else
      {
        return;
      }

      if( isPropertyField( lhsSym ) )
      {
        return;
      }

      // replace  foo.bar = baz  with  foo.setBar(baz)

      Context ctx = _javacTask.getContext();

      MethodSymbol setMethod = resolveSetMethod( lhsSelectedType, lhsSym, Types.instance( ctx ) );

      if( setMethod != null )
      {
        JCMethodDecl methodDecl = _methodDefs.peek();
        if( methodDecl != null && methodDecl.sym == setMethod )
        {
          // don't rewrite with setter inside the setter
          _backingSymbols.peek().snd.add( (VarSymbol)lhsSym );
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
        _tp.report( tree, Diagnostic.Kind.ERROR,
          PropIssueMsg.MSG_CANNOT_MODIFY_PROPERTY.get( lhsSym.flatName().toString() ) );
      }
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

      MethodSymbol getMethod = ManAttr.getMethodSymbol( types, type, field.type, getGetterName( field, true ), (ClassSymbol)type.tsym, 0 );
      if( getMethod == null )
      {
        getMethod = ManAttr.getMethodSymbol( types, type, field.type, getGetterName( field, false ), (ClassSymbol)type.tsym, 0 );
      }
      if( getMethod != null )
      {
        Type elemType = getMethod.type.isErroneous()
          ? getMethod.type
          : types.memberType( type, getMethod ).getReturnType();

        MethodSymbol setMethod = ManAttr.getMethodSymbol( types, type, field.type, getSetterName( field.name ), (ClassSymbol)type.tsym, 1 );
        if( setMethod != null )
        {
          Type param2 = types.memberType( type, setMethod ).getParameterTypes().get( 0 );
          if( types.isAssignable( elemType, param2 ) || ManAttr.isAssignableWithGenerics( types, elemType, param2 ) )
          {
            return setMethod;
          }
        }
      }
      return null;
    }
  }

  // Make the field PRIVATE, or delete it if no backing field is necessary
  // If a backing field, add annotation:  @propgen(name, flags), so we can change it when loaded from .class file
  // Note, @propgen is also added to getter/setter methods so that a non-backing field can be recreated on .class load
  class Generate_Start
  {
    private void handleClass( ClassSymbol classSym )
    {
      classSym.members_field.getElements( e -> e instanceof ClassSymbol )
        .forEach( c -> handleClass( (ClassSymbol)c ) );

      classSym.members_field.getElements( e -> e instanceof VarSymbol )
        .forEach( varSym -> handleField( classSym, (VarSymbol)varSym ) );
    }

    private void handleField( ClassSymbol classSym, VarSymbol fieldSym )
    {
      long modifiers = fieldSym.flags_field;

      if( isPropertyField( fieldSym ) )
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
        // make the field a backing field (private), make it public again during Generate:finish

        fieldSym.flags_field = fieldSym.flags_field & ~(PUBLIC | PROTECTED) | PRIVATE;

        // Add annotation:  @propgen(name, flags), so we can change it when loaded from .class file

        Names names = Names.instance( _javacTask.getContext() );
        Symtab symtab = Symtab.instance( _javacTask.getContext() );

//todo: add the args for prop, get, and set
//
//          Attribute.Compound propAnno = new Attribute.Compound( propSym.type,
//            List.of( new Pair<>( valueMeth, new Attribute.Array( PropOption[].class, new Attribute.Enum( PropOption.class, whatever ) ),
//              new Pair<>( flagsMeth, new Attribute.Constant( symtab.longType, modifiers ) ) ) );

        ClassSymbol propgenSym = IDynamicJdk.instance().getTypeElement( _javacTask.getContext(),
          _tp.getCompilationUnit(), propgen.class.getTypeName() );
        MethodSymbol nameMeth = (MethodSymbol)propgenSym.members().lookup( names.fromString( "name" ) ).sym;
        MethodSymbol flagsMeth = (MethodSymbol)propgenSym.members().lookup( names.fromString( "flags" ) ).sym;
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

//          ArrayList<JCTree> newDefs = new ArrayList<>( classDecl.defs );
//          newDefs.remove( tree );
//          classDecl.defs = List.from( newDefs );

        classSym.members().remove( fieldSym );
        Set<VarSymbol> nonbacking = _nonbackingMap.computeIfAbsent( classSym, e -> new HashSet<>() );
        nonbacking.add( fieldSym );
      }
    }
  }

  class Generate_Finish
  {
    public void handleClass( ClassSymbol classSym )
    {
      classSym.members_field.getElements( e -> e instanceof ClassSymbol )
        .forEach( c -> handleClass( (ClassSymbol)c ) );

      // handle backing fields
      //

      // put original modifiers back e.g. PUBLIC
      Set<VarSymbol> backingFields = _propMap.get( classSym );
      if( backingFields != null )
      {
        for( VarSymbol varSym : backingFields )
        {
          propgen anno = varSym.getAnnotation( propgen.class );
          if( anno != null )
          {
            // remove PRIVATE, restore original access modifiers
            varSym.flags_field = varSym.flags_field & ~PRIVATE | anno.flags();
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
          classSym.members_field.enter( sym );
//        varSym.sym = sym;
//
//          ArrayList<JCTree> newDefs = new ArrayList<>( classDecl.defs );
//          newDefs.add( varSym );
//          classDecl.defs = List.from( newDefs );
        }
      }
      _nonbackingMap.remove( classSym );
    }
  }

  public boolean isPropertyField( Symbol sym )
  {
    // Not a property field
    return sym.getAnnotation( prop.class ) == null &&
      sym.getAnnotation( get.class ) == null &&
      sym.getAnnotation( set.class ) == null;
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
