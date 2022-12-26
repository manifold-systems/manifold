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

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.*;
import manifold.api.type.ICompilerComponent;
import manifold.api.util.JavacDiagnostic;
import manifold.ext.ExtensionTransformer;
import manifold.ext.props.rt.api.*;
import manifold.ext.props.rt.api.tags.enter_finish;
import manifold.ext.props.rt.api.tags.enter_start;
import manifold.internal.javac.*;
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.Stack;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;

import static com.sun.tools.javac.code.TypeTag.NONE;
import static java.lang.reflect.Modifier.*;
import static manifold.ext.props.PropIssueMsg.*;
import static manifold.ext.props.Util.*;

public class PropertyProcessor implements ICompilerComponent, TaskListener
{
  private BasicJavacTask _javacTask;
  private Context _context;
  private Stack<Pair<JCClassDecl, ArrayList<JCTree>>> _propertyStatements;
  private Map<ClassSymbol, Set<VarSymbol>> _propMap;
  private Map<ClassSymbol, Set<VarSymbol>> _backingMap;
  private Map<ClassSymbol, Set<VarSymbol>> _nonbackingMap;
  private PropertyInference _propInference;
  private Set<ClassSymbol> _inferLater;
  private TaskEvent _taskEvent;
  private ParentMap _parents;

  @Override
  public void init( BasicJavacTask javacTask, TypeProcessor typeProcessor )
  {
    _javacTask = javacTask;
    _context = _javacTask.getContext();
    _propertyStatements = new Stack<>();
    _propMap = new HashMap<>();
    _backingMap = new HashMap<>();
    _nonbackingMap = new HashMap<>();
    _inferLater = new HashSet<>();
    _parents = new ParentMap( () -> getCompilationUnit() );

    if( JavacPlugin.instance() == null )
    {
      // does not function at runtime
      return;
    }

    // Ensure TypeProcessor follows this in the listener list e.g., so that properties integrate with structural
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
    _propInference = new PropertyInference( field -> addToBackingFields( field ), () -> getContext() );
    ClassReaderCompleter.replaceCompleter( this );
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
    // needed for case where ClassReader completer does properties outside the scope of TaskListener
    return JavacPlugin.instance() != null
      ? JavacPlugin.instance().getTypeProcessor().getCompilationUnit()
      : null;
  }

  void inferPropertiesFromClassReader( ClassSymbol classSym )
  {
    _propInference.inferProperties( classSym );
    for( Symbol elem: classSym.getEnclosedElements() )
    {
      if( elem instanceof ClassSymbol )
      {
        inferPropertiesFromClassReader( (ClassSymbol)elem );
      }
    }
  }

  @Override
  public void started( TaskEvent e )
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
          else if( e.getKind() == TaskEvent.Kind.ANALYZE )
          {
            classDecl.accept( new Analyze_Start() );
          }
          else if( e.getKind() == TaskEvent.Kind.GENERATE )
          {
            new Generate_Start().handleClass( e.getTypeElement() );
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
          else if( e.getKind() == TaskEvent.Kind.GENERATE )
          {
            new Generate_Finish().handleClass( e.getTypeElement() );
          }
        }
      }
    }
    finally
    {
      _taskEvent = null;
    }
  }

  // Make getter/setter methods corresponding with @var, @get, @set fields
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
        if( getAnnotation( tree, enter_start.class ) != null )
        {
          // already processed, probably an annotation processing round
          return;
        }

        JCAnnotation var = getAnnotation( tree, var.class );
        JCAnnotation val = getAnnotation( tree, val.class );
        JCAnnotation get = getAnnotation( tree, get.class );
        JCAnnotation set = getAnnotation( tree, set.class );

        if( var == null && val == null && get == null && set == null )
        {
          // not a property field
          return;
        }

        TreeMaker make = TreeMaker.instance( _context );

        // tag the field as processed to avoid processing it again (during annotation processing)
        JCExpression enter_start = memberAccess( make, enter_start.class.getName() );
        addAnnotations( tree, List.of( make.Annotation( enter_start, List.nil() ) ) );

        if( (modifiers & (PUBLIC | PROTECTED | PRIVATE)) == 0 )
        {
          // default @var fields to PUBLIC, they must use PropOption.Package if they really want it
          tree.getModifiers().flags |= PUBLIC;
        }

        boolean isAbstract = getAnnotation( tree, Abstract.class ) != null;
        boolean isFinal = getAnnotation( tree, Final.class ) != null;

        if( (modifiers & ABSTRACT) != 0 )
        {
          // remove 'abstract' modifier, applies to getter/setter methods
          tree.getModifiers().flags &= ~ABSTRACT;
          if( !isAbstract )
          {
            // add @Abstract
            JCExpression abstractType = memberAccess( make, Abstract.class.getName() );
            addAnnotations( tree, List.of( make.Annotation( abstractType, List.nil() ) ) );
            isAbstract = true;
          }
        }
        if( (modifiers & FINAL) != 0 )
        {
          // remove 'final' modifier, applies to getter/setter methods
          tree.getModifiers().flags &= ~FINAL;
          if( !isFinal )
          {
            // add @Final
            JCExpression abstractType = memberAccess( make, Final.class.getName() );
            addAnnotations( tree, List.of( make.Annotation( abstractType, List.nil() ) ) );
            isFinal = true;
          }
        }

        if( (val != null || (get != null && set == null && var == null)) && !isAbstract && !isInterface( classDecl ) )
        {
          List<JCExpression> args = get == null ? val.args : get.args;
          if( !hasOption( args, PropOption.Abstract ) )
          {
            // add `final` for @val with backing field; this flag is removed if there is no backing field
            tree.getModifiers().flags |= FINAL;
          }
        }

        if( isAbstract && !isInterface( classDecl ) && !Modifier.isAbstract( (int)classDecl.getModifiers().flags ) )
        {
          reportError( tree, MSG_ABSTRACT_PROPERTY_IN_NONABSTRACT_CLASS.get() );
          return;
        }

        if( isFinal && isAbstract )
        {
          reportError( tree, MSG_FINAL_NOT_ALLOWED_ON_ABSTRACT.get() );
          return;
        }
        else if( isFinal && isStatic( tree ) )
        {
          reportError( tree, MSG_FINAL_NOT_ALLOWED_ON_STATIC.get() );
          return;
        }

        if( isInterface( classDecl ) && ((int)tree.getModifiers().flags & STATIC) != 0 )
        {
          // preserve the explicit static declaration so we can distinguish static/instance fields in interfaces
          addAnnotations( tree, List.of( makeStaticAnnotation() ) );
        }

        // add getter and/or setter
        // or, if a PRIVATE property and no user-defined accessors exist, no benefit from property, so treat as field

        JCMethodDecl generatedGetter = null;
        JCMethodDecl generatedSetter = null;
        boolean shouldMakeProperty = !isPrivate( (int)tree.getModifiers().flags );

        Pair<JCClassDecl, ArrayList<JCTree>> pair = _propertyStatements.peek();
        if( var != null || val != null || get != null )
        {
          List<JCExpression> args = get == null ? val == null ? var.args : val.args : get.args;
          boolean getAbstract = isAbstract( classDecl, tree ) || hasOption( args, PropOption.Abstract );
          boolean getFinal = isFinal || hasOption( args, PropOption.Final );
          PropOption getAccess = getAccess( classDecl, args );

          if( !isInterface( classDecl ) && isWeakerAccess( getAccess, getAccess( modifiers ) ) )
          {
            reportError( get != null ? get : var, MSG_ACCESSOR_WEAKER.get( "get",
              PropOption.fromModifier( getAccess( modifiers ) ).name().toLowerCase() ) );
          }

          if( getFinal && getAbstract )
          {
            reportError( tree, MSG_FINAL_NOT_ALLOWED_ON_ABSTRACT.get() );
          }
          else if( getFinal && isStatic( tree ) )
          {
            reportError( tree, MSG_FINAL_NOT_ALLOWED_ON_STATIC.get() );
          }
          else if( getAbstract && !isInterface( classDecl ) && !Modifier.isAbstract( (int)classDecl.getModifiers().flags ) )
          {
            reportError( tree, MSG_ABSTRACT_PROPERTY_IN_NONABSTRACT_CLASS.get() );
          }
          else
          {
            JCAnnotation anno = get == null ? val == null ? var : val : get;
            generatedGetter = makeGetter( classDecl, tree, getAbstract, getFinal, getAccess, anno );
            if( generatedGetter == null )
            {
              shouldMakeProperty = true;

              // remove `final` for user-defined getter (todo: keep final if user-defined getter refs backing field)
              tree.getModifiers().flags &= ~FINAL;
            }
          }
        }

        if( var != null || set != null )
        {
          List<JCExpression> args = set == null ? var.args : set.args;
          boolean setAbstract = isAbstract( classDecl, tree ) || isInterface( classDecl ) || hasOption( args, PropOption.Abstract );
          boolean setFinal = isFinal || hasOption( args, PropOption.Final );
          PropOption setAccess = getAccess( classDecl, args );

          if( tree.init != null && setAbstract )
          {
            reportError( tree.init, MSG_WRITABLE_ABSTRACT_PROPERTY_CANNOT_HAVE_INITIALIZER.get( tree.name ) );
          }

          if( !isInterface( classDecl ) && isWeakerAccess( setAccess, getAccess( modifiers ) ) )
          {
            reportError( set != null ? set : var, MSG_ACCESSOR_WEAKER.get( "set",
              PropOption.fromModifier( getAccess( modifiers ) ).name().toLowerCase() ) );
          }

          if( setFinal && setAbstract )
          {
            reportError( tree, MSG_FINAL_NOT_ALLOWED_ON_ABSTRACT.get() );
          }
          else if( setFinal && isStatic( tree ) )
          {
            reportError( tree, MSG_FINAL_NOT_ALLOWED_ON_STATIC.get() );
          }
          else if( setAbstract && !isInterface( classDecl ) && !Modifier.isAbstract( (int)classDecl.getModifiers().flags ) )
          {
            reportError( tree, MSG_ABSTRACT_PROPERTY_IN_NONABSTRACT_CLASS.get() );
          }
          else
          {
            generatedSetter = makeSetter( classDecl, tree, setAbstract, setFinal, setAccess, set != null ? set : var );
            if( generatedSetter == null )
            {
              shouldMakeProperty = true;
            }
          }
        }

        if( shouldMakeProperty )
        {
          if( (generatedGetter != null || generatedSetter != null) &&
            isInterface( classDecl ) && isStatic( tree ) )
          {
            reportError( tree, MSG_INTERFACE_FIELD_BACKED_PROPERTY_NOT_SUPPORTED.get() );
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
          // remove @var etc. to treat it as a raw field

          ArrayList<JCAnnotation> annos = new ArrayList<>( tree.getModifiers().getAnnotations() );
          annos.remove( var );
          annos.remove( val );
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
    @SuppressWarnings( "CommentedOutCode" )
    private JCMethodDecl makeGetter( JCClassDecl classDecl, JCVariableDecl propField,
                                     boolean propAbstract, boolean propFinal, PropOption propAccess, JCAnnotation anno )
    {
      Context context = _context;
      TreeMaker make = TreeMaker.instance( context );
      long flags = propField.getModifiers().flags;
      JCAnnotation propgenAnno = makePropGenAnnotation( propField );
      List<JCAnnotation> annos = List.of( propgenAnno );
      JCModifiers access = getGetterSetterModifiers( make, isInterface( classDecl ), propAbstract, propFinal, isStatic( propField ),
        propAccess, (int)flags, annos, propField.pos );
      Name name = getNames().fromString( getGetterName( propField, true ) );
      JCExpression resType = (JCExpression)propField.vartype.clone();
      JCBlock block = null;
      if( !propAbstract )
      {
        JCReturn ret = isInterface( classDecl )
          ? make.Return( propField.init )
          : make.Return( make.Ident( propField.name ).setPos( propField.pos ) );
        block = (JCBlock)make.Block( 0, List.of( ret ) ).setPos( propField.pos );
      }
      JCMethodDecl getter = (JCMethodDecl)make.MethodDef(
        access, name, resType, List.nil(), List.nil(), List.nil(), block, null ).setPos( propField.pos );
      JCMethodDecl existingGetter = findExistingAccessor( propField, classDecl, getter );
      if( existingGetter != null )
      {
        addAnnotations( existingGetter, List.of( propgenAnno ) );
        addAnnotations( existingGetter, getAnnotations( anno, "annos" ) );
        return null;
      }
      else if( isInterface( classDecl ) && isStatic( propField ) && propField.init == null )
      {
        // interface: static non-initialized property MUST provide user-defined getter
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
    @SuppressWarnings( "CommentedOutCode" )
    private JCMethodDecl makeSetter( JCClassDecl classDecl, JCVariableDecl propField,
                                     boolean propAbstract, boolean propFinal, PropOption propAccess, JCAnnotation anno )
    {
      TreeMaker make = getTreeMaker();
      long flags = propField.getModifiers().flags;
      JCAnnotation propgenAnno = makePropGenAnnotation( propField );
      List<JCAnnotation> annos = List.of( propgenAnno );
      JCModifiers access = getGetterSetterModifiers( make, isInterface( classDecl ), propAbstract, propFinal, isStatic( propField ),
        propAccess, (int)flags, annos, propField.pos );
      Names names = getNames();
      Name name = names.fromString( getSetterName( propField.name ) );
      JCVariableDecl param = (JCVariableDecl)make.VarDef(
        make.Modifiers( FINAL | Flags.PARAMETER ), names.fromString( "$value" ),
        (JCExpression)propField.vartype.clone(), null ).setPos( propField.pos );
      JCExpression resType = make.Type( getSymtab().voidType ).setPos( propField.pos );
      JCExpressionStatement assign = (JCExpressionStatement)make.Exec( make.Assign(
        make.Ident( propField.name ).setPos( propField.pos ),
        make.Ident( names.fromString( "$value" ) ).setPos( propField.pos ) ).setPos( propField.pos ) )
        .setPos( propField.pos );
      JCBlock block = propAbstract ? null : (JCBlock)make.Block( 0, List.of( assign ) ).setPos( propField.pos );
      JCMethodDecl setter = (JCMethodDecl)make.MethodDef(
        access, name, resType, List.nil(), List.of( param ), List.nil(), block, null )
        .setPos( propField.pos );
      JCMethodDecl existingSetter = findExistingAccessor( propField, classDecl, setter );
      if( existingSetter != null )
      {
        addAnnotations( existingSetter, List.of( propgenAnno ) );
        return null;
      }
      else if( isInterface( classDecl ) && isStatic( propField ) && propField.init == null )
      {
        // interface: static non-initialized property MUST provide user-defined setter
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

    private JCMethodDecl findExistingAccessor( JCVariableDecl propField, JCClassDecl classDecl, JCMethodDecl accessor )
    {
      outer:
      for( JCTree def : classDecl.defs )
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
      if( iClose < type.length() - 1 )
      {
        erasedType += type.substring( iClose + 1 );
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

    private void addAnnotations( JCVariableDecl varDecl, List<JCAnnotation> propgenAnno )
    {
      ArrayList<JCAnnotation> newAnnos = new ArrayList<>( varDecl.getModifiers().annotations );
      newAnnos.addAll( propgenAnno );
      varDecl.getModifiers().annotations = List.from( newAnnos );
    }

    private List<JCAnnotation> getAnnotations( JCAnnotation anno, String target )
    {
      for( JCExpression arg : anno.args )
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

    private JCModifiers getGetterSetterModifiers( TreeMaker make, boolean isInterface,
                                                  boolean propAbstract, boolean propFinal, boolean propStatic,
                                                  PropOption propAccess, long flags, List<JCAnnotation> annos, int pos )
    {
      int iflags = (int)flags;
      long access = propAccess == null
        ? isPublic( iflags ) ? PUBLIC : isProtected( iflags ) ? PROTECTED : isPrivate( iflags ) ? PRIVATE : 0
        : propAccess.getModifier();
      if( isInterface && !propAbstract && !propStatic )
      {
        access |= Flags.DEFAULT;
      }
      access |= (propAbstract ? ABSTRACT : 0);
      access |= (propFinal ? FINAL : 0);
      access |= (propStatic ? STATIC : 0);
      return (JCModifiers)make.Modifiers( access, annos ).setPos( pos );
    }

    private JCAnnotation makePropGenAnnotation( JCVariableDecl field )
    {
      TreeMaker make = TreeMaker.instance( getContext() );
      Names names = Names.instance( getContext() );

      ArrayList<JCAssign> args = new ArrayList<>();
      args.add( make.Assign( make.Ident( names.fromString( "name" ) ), make.Literal( field.name.toString() ) ) );
      args.add( make.Assign( make.Ident( names.fromString( "flags" ) ), make.Literal( field.getModifiers().flags ) ) );
      maybeAddAnnotation( field, args, var.class );
      maybeAddAnnotation( field, args, val.class );
      maybeAddAnnotation( field, args, get.class );
      maybeAddAnnotation( field, args, set.class );
      maybeAddAnnotation( field, args, Abstract.class );
      maybeAddAnnotation( field, args, Final.class );
      JCExpression propgenType = memberAccess( make, propgen.class.getName() );
      return make.Annotation( propgenType, List.from( args ) );
    }

    private JCAnnotation makeStaticAnnotation()
    {
      TreeMaker make = TreeMaker.instance( getContext() );
      JCExpression staticAnno = memberAccess( make, Static.class.getName() );
      return make.Annotation( staticAnno, List.nil() );
    }

    private void maybeAddAnnotation( JCVariableDecl field, ArrayList<JCAssign> args, Class<? extends Annotation> cls )
    {
      TreeMaker make = TreeMaker.instance( getContext() );
      Names names = Names.instance( getContext() );

      for( JCAnnotation anno : field.getModifiers().getAnnotations() )
      {
        if( cls.getSimpleName().equals( anno.annotationType.toString() ) ||
          cls.getTypeName().equals( anno.annotationType.toString() ) )
        {
          args.add( make.Assign( make.Ident( names.fromString( cls.getSimpleName() ) ), anno ) );
        }
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

    // does the accessor method have weaker access than the var field?
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

  // - Remove FINAL modifier from property fields in interfaces to enable assignment
  // (note these fields do not exist in bytecode)
  // - verify property and methods agree
  // - verify property override
  class Enter_Finish extends TreeTranslator
  {
    private final Stack<JCClassDecl> _classes = new Stack<>();

    @Override
    public void visitClassDef( JCClassDecl classDecl )
    {
      _classes.push( classDecl );
      try
      {
        super.visitClassDef( classDecl );

        if( classDecl.sym != null )
        {
          // perform property inference after Enter stage is done (first thing in Analyze_Start)
          _inferLater.add( classDecl.sym );
        }
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

      if( getAnnotationMirror( tree.sym, enter_finish.class ) != null )
      {
        // already processed, probably an annotation processing round
        return;
      }
      addAnnotation( tree.sym, enter_finish.class );

      verifyPropertyMethodsAgree( tree );

      verifyPropertyOverride( tree );

      JCClassDecl cls = _classes.peek();
      if( isInterface( cls ) )
      {
        // Remove FINAL modifier from property fields in interfaces to enable assignment
        // (note these fields do not exist in bytecode)
        tree.sym.flags_field &= ~FINAL;
      }
    }

    private void verifyPropertyOverride( JCVariableDecl tree )
    {
      Names names = Names.instance( _context );

      boolean readableProperty = isReadableProperty( tree.sym );
      boolean writableProperty = isWritableProperty( tree.sym );

      JCAnnotation override = getAnnotation( tree, override.class );
      if( override != null )
      {
        if( tree.sym.isStatic() )
        {
          reportError( tree, PropIssueMsg.MSG_DOES_NOT_OVERRIDE_ANYTHING.get( tree.name ) );
        }
        else if( readableProperty )
        {
          MethodSymbol superReadable = getSuperReadable( tree, names );
          if( superReadable == null )
          {
            if( !writableProperty )
            {
              reportError( tree, PropIssueMsg.MSG_DOES_NOT_OVERRIDE_ANYTHING.get( tree.name ) );
            }
            else
            {
              MethodSymbol superWritable = getSuperWritable( tree, names );
              if( superWritable == null )
              {
                reportError( tree, PropIssueMsg.MSG_DOES_NOT_OVERRIDE_ANYTHING.get( tree.name ) );
              }
              else if( superWritable.isStatic() )
              {
                reportError( tree, MSG_CANNOT_OVERRIDE_STATIC.get( tree.name, superWritable.name ) );
              }
            }
          }
          else if( superReadable.isStatic() )
          {
            reportError( tree, MSG_CANNOT_OVERRIDE_STATIC.get( tree.name, superReadable.name ) );
          }
          else
          {
            Types types = Types.instance( getContext() );
            Type getterReturnType = types.memberType( tree.sym.enclClass().type, superReadable ).getReturnType();
            if( !types.isCastable( getterReturnType, tree.sym.type ) )
            {
              reportError( tree, MSG_PROPERTY_CLASH_RETURN.get( tree.name, tree.sym.enclClass().className(), superReadable.enclClass().className() ) );
            }
          }
        }
        else if( writableProperty )
        {
          MethodSymbol superWritable = getSuperWritable( tree, names );
          if( superWritable == null )
          {
            reportError( tree, PropIssueMsg.MSG_DOES_NOT_OVERRIDE_ANYTHING.get( tree.name ) );
          }
          else if( superWritable.isStatic() )
          {
            reportError( tree, MSG_CANNOT_OVERRIDE_STATIC.get( tree.name, superWritable.name ) );
          }
        }
      }
      else if( !tree.sym.isStatic() )
      {
        if( readableProperty )
        {
          MethodSymbol superReadable = getSuperReadable( tree, names );
          if( superReadable != null )
          {
            if( superReadable.isStatic() )
            {
              reportError( tree, MSG_CANNOT_OVERRIDE_STATIC.get( tree.name, superReadable.name ) );
            }
            else
            {
              reportError( tree, PropIssueMsg.MSG_MISSING_OVERRIDE.get( tree.name ) );
            }
            return;
          }
        }

        if( writableProperty )
        {
          MethodSymbol superWritable = getSuperWritable( tree, names );
          if( superWritable != null )
          {
            if( superWritable.isStatic() )
            {
              reportError( tree, MSG_CANNOT_OVERRIDE_STATIC.get( tree.name, superWritable.name ) );
            }
            else
            {
              reportError( tree, PropIssueMsg.MSG_MISSING_OVERRIDE.get( tree.name ) );
            }
          }
        }
      }
    }

    private MethodSymbol getSuperWritable( JCVariableDecl tree, Names names )
    {
      Name setterName = names.fromString( getSetterName( tree.sym.name ) );
      Types types = Types.instance( _javacTask.getContext() );
      return checkAncestry( (TypeSymbol)tree.sym.owner, superSym -> {
        for( Symbol member : IDynamicJdk.instance().getMembersByName( (ClassSymbol)superSym, setterName ) )
        {
          if( member instanceof MethodSymbol && (!superSym.isInterface() || !member.isStatic()) &&
            ((MethodSymbol)member).params().size() == 1 )
          {
            Type setterParamType = ((Type.MethodType)types.memberType( tree.sym.enclClass().type, member )).argtypes.get( 0 );
            if( types.isSameType( tree.sym.type, setterParamType ) )
            {
              return (MethodSymbol)member;
            }
          }
        }
        return null;
      } );
    }

    private MethodSymbol getSuperReadable( JCVariableDecl tree, Names names )
    {
      Name getterName = names.fromString( getGetterName( tree.sym, true ) );
      return checkAncestry( (TypeSymbol)tree.sym.owner, superSym -> {
        for( Symbol member : IDynamicJdk.instance().getMembersByName( (ClassSymbol)superSym, getterName ) )
        {
          if( member instanceof MethodSymbol && (!superSym.isInterface() || !member.isStatic()) &&
            ((MethodSymbol)member).params().isEmpty() )
          {
// let caller determine when to detect clashes
//            Type getterReturnType = types.memberType( tree.sym.enclClass().type, member ).getReturnType();
//            if( types.isCastable( getterReturnType, tree.sym.type ) )
//            {
              return (MethodSymbol)member;
//            }
          }
        }
        return null;
      } );
    }

    private MethodSymbol checkAncestry( TypeSymbol ts, Function<Symbol, MethodSymbol> check )
    {
      if( !(ts instanceof ClassSymbol) )
      {
        return null;
      }

      ClassSymbol sym = (ClassSymbol)ts;

      Type superclass = sym.getSuperclass();
      if( superclass != null && superclass.tsym != null )
      {
        MethodSymbol method = check.apply( superclass.tsym );
        if( method != null )
        {
          return method;
        }
        method = checkAncestry( superclass.tsym, check );
        if( method != null )
        {
          return method;
        }
      }
      for( Type iface : sym.getInterfaces() )
      {
        MethodSymbol method = check.apply( iface.tsym );
        if( method != null )
        {
          return method;
        }
        method = checkAncestry( iface.tsym, check );
        if( method != null )
        {
          return method;
        }
      }
      return null;
    }

    private void verifyPropertyMethodsAgree( JCVariableDecl varDecl )
    {
      JCAnnotation var = getAnnotation( varDecl, var.class );
      JCAnnotation val = getAnnotation( varDecl, val.class );
      JCAnnotation get = getAnnotation( varDecl, get.class );
      JCAnnotation set = getAnnotation( varDecl, set.class );

      if( var == null && val == null && get == null && set == null )
      {
        // not a property field
        throw new IllegalStateException();
      }

      verifyGetter( varDecl, var, val, get );
      verifySetter( varDecl, var, set );
    }

    private void verifyGetter( JCVariableDecl varDecl, JCAnnotation var, JCAnnotation val, JCAnnotation get )
    {
      MethodSymbol getMethod = resolveGetMethod( varDecl.sym.owner.type, varDecl.sym );
      if( var != null || val != null || get != null )
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

        List<JCExpression> args = get == null ? val == null ? var.args : val.args : get.args;
        boolean getAbstract = isAbstract( classDecl, varDecl ) || hasOption( args, PropOption.Abstract );
        boolean getFinal = getAnnotation( varDecl, Final.class ) != null || hasOption( args, PropOption.Final );
        PropOption getAccess = getAccess( classDecl, args );

        if( getAbstract != Modifier.isAbstract( (int)getMethod.flags() ) )
        {
          if( classDecl.getKind() != Tree.Kind.INTERFACE )
          {
            reportError( varDecl, MSG_PROPERTY_METHOD_CONFLICT.get(
              varDecl.sym.flatName(), getMethod.flatName(), "Abstract" ) );
          }
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
        if( getMethod.owner == varDecl.sym.owner )
        {
          reportError( varDecl, MSG_GETTER_DEFINED_FOR_WRITEONLY.get(
            getMethod.flatName(), varDecl.sym.flatName() ) );
        }
        else
        {
          reportError( varDecl, PropIssueMsg.MSG_WRITEONLY_CANNOT_OVERRIDE_READABLE.get(
            getMethod.flatName(), varDecl.sym.flatName() ) );
        }
      }
    }

    private void verifySetter( JCVariableDecl varDecl, JCAnnotation var, JCAnnotation set )
    {
      JCClassDecl classDecl = _classes.peek();

      MethodSymbol setMethod = resolveSetMethod( varDecl.sym.owner.type, varDecl.sym,
        Types.instance( _context ) );

      if( var != null || set != null )
      {
        // Property is defined to have 'set' access
        //

        if( setMethod != null )
        {
          // check that the method and property are both static/non-static, otherwise issue compiler error
          setMethod = checkStatic( classDecl, varDecl, varDecl.sym, setMethod );
        }

        if( setMethod == null )
        {
          return;
        }

        List<JCExpression> args = set == null ? var.args : set.args;
        boolean setAbstract = isAbstract( classDecl, varDecl ) || hasOption( args, PropOption.Abstract );
        boolean setFinal = getAnnotation( varDecl, Final.class ) != null || hasOption( args, PropOption.Final );
        PropOption setAccess = getAccess( classDecl, args );
        boolean setGenerated = getAnnotation( varDecl, propgen.class ) != null;

        if( setAbstract != Modifier.isAbstract( (int)setMethod.flags() ) )
        {
          if( classDecl.getKind() != Tree.Kind.INTERFACE )
          {
            reportError( varDecl, MSG_PROPERTY_METHOD_CONFLICT.get(
              varDecl.sym.flatName(), setMethod.flatName(), "Abstract" ) );
          }
        }

        if( setFinal && isInterface( classDecl ) )
        {
          reportError( varDecl, MSG_FINAL_NOT_ALLOWED_ON_ABSTRACT.get() );
        }

        if( setFinal != Modifier.isFinal( (int)setMethod.flags() ) )
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

        if( (setMethod.flags() & accessModifier) != accessModifier )
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
        if( setMethod.owner == varDecl.sym.owner )
        {
          reportError( varDecl, MSG_SETTER_DEFINED_FOR_READONLY.get(
            setMethod.flatName(), varDecl.sym.flatName() ) );
        }
        else
        {
          reportError( varDecl, MSG_READONLY_CANNOT_OVERRIDE_WRITABLE.get(
            setMethod.flatName(), varDecl.sym.flatName() ) );
        }
      }
    }

    private MethodSymbol checkStatic( JCClassDecl classDecl, JCVariableDecl propDecl, Symbol field, MethodSymbol method )
    {
      if( method == null )
      {
        return null;
      }

      boolean isInterface = isInterface( classDecl );
      boolean isPropStatic = isInterface
        ? ((int)propDecl.getModifiers().flags & STATIC) != 0
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

  // Recursively infer properties of source-based class symbols gathered during Enter_Finish
  //
  class Analyze_Start extends TreeTranslator
  {
    private final Set<ClassSymbol> _visited = new HashSet<>();

    @Override
    public void visitClassDef( JCClassDecl classDecl )
    {
      super.visitClassDef( classDecl );
      inferProperties();
    }

    private void inferProperties()
    {
      if( _inferLater.isEmpty() )
      {
        return;
      }

      for( ClassSymbol classSym : new HashSet<>( _inferLater ) )
      {
        inferProperties( classSym );
      }
    }

    private void inferProperties( TypeSymbol tsym )
    {
      if( !(tsym instanceof ClassSymbol) )
      {
        return;
      }

      ClassSymbol classSym = (ClassSymbol)tsym;

      if( _visited.contains( classSym ) )
      {
        return;
      }
      _visited.add( classSym );

      Type superclass = classSym.getSuperclass();
      if( superclass instanceof Type.ClassType )
      {
        inferProperties( superclass.tsym );
      }
      classSym.getInterfaces().forEach( iface -> inferProperties( iface.tsym ) );

      _propInference.inferProperties( classSym );
      _inferLater.remove( classSym );
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
    private final Stack<JCVariableDecl> _propDefs = new Stack<>();
    private final Stack<JCMethodDecl> _methodDefs = new Stack<>();
    private final Stack<Pair<JCClassDecl, Set<VarSymbol>>> _backingSymbols = new Stack<>();

    @Override
    public void visitClassDef( JCClassDecl classDecl )
    {
      _backingSymbols.push( new Pair<>( classDecl, new HashSet<>() ) );
      try
      {
        super.visitClassDef( classDecl );
        _backingMap.computeIfAbsent( classDecl.sym, e -> new HashSet<>() )
          .addAll( _backingSymbols.peek().snd );
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

      JCClassDecl classDecl = _backingSymbols.peek().fst;
      if( !isPropertyField( tree.sym ) || keepRefToField( tree, tree.sym, classDecl ) )
      {
        return;
      }

      // don't process here if the field access is an l-value
      //
      Tree parent = getParent( tree );
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
      result = repalceWithGetter( tree );
    }

    public JCExpression repalceWithGetter( JCFieldAccess tree )
    {
      // replace foo.bar with foo.getBar()
      //
      MethodSymbol getMethod = isReadableProperty( tree.sym )
        ? resolveGetMethod( tree.selected.type, tree.sym )
        : null;

      if( getMethod != null )
      {
        JCMethodDecl methodDecl =_methodDefs.isEmpty() ? null : _methodDefs.peek();
        if( methodDecl != null && methodDecl.sym == getMethod )
        {
          // don't rewrite with getter inside the getter
          _backingSymbols.peek().snd.add( (VarSymbol)tree.sym );
          return tree;
        }

        if( !verifyAccess( tree, tree.sym, getMethod, "Read" ) )
        {
          // the getter is not accessible from the use site
          return tree;
        }

        TreeMaker make = getTreeMaker();

        JCMethodInvocation methodCall;
        JCExpression receiver = tree.selected;
        methodCall = make.Apply( List.nil(), make.Select( receiver, getMethod ), List.nil() );
        methodCall = configMethod( tree, getMethod, methodCall );

        return methodCall;
      }
      else
      {
        reportError( tree, MSG_CANNOT_ACCESS_WRITEONLY_PROPERTY.get( tree.sym.flatName() ) );
      }
      return tree;
    }

    @Override
    public void visitIdent( JCIdent tree )
    {
      super.visitIdent( tree );

      JCClassDecl classDecl = _backingSymbols.peek().fst;
      if( !isPropertyField( tree.sym ) || keepRefToField( tree, tree.sym, classDecl ) )
      {
        return;
      }

      // don't process here if ident is an l-value
      //
      Tree parent = getParent( tree );
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
      result = replaceWithGetter( tree );
    }

    private JCExpression replaceWithGetter( JCIdent tree )
    {
      // replace bar with this.getBar()
      //
      MethodSymbol getMethod = isReadableProperty( tree.sym )
        ? resolveGetMethod( _backingSymbols.peek().fst.type, tree.sym )
        : null;
      if( getMethod != null )
      {
        JCMethodDecl methodDecl = _methodDefs.isEmpty() ? null : _methodDefs.peek();
        if( methodDecl != null && (methodDecl.sym == getMethod || overrides( methodDecl.sym, getMethod )) )
        {
          // - don't rewrite with getter inside the getter
          // - backing symbol required, add to set of backing symbols
          _backingSymbols.peek().snd.add( (VarSymbol)tree.sym );

          if( !_methodDefs.isEmpty() && _methodDefs.peek().sym.isDefault() )
          {
            // Cannot reference property in default interface accessor
            reportError( tree, MSG_PROPERTY_IS_ABSTRACT.get( tree.sym.flatName() ) );
          }

          return tree;
        }

        if( !verifyAccess( tree, tree.sym, getMethod, "Read" ) )
        {
          // the getter is not accessible from the use site
          return tree;
        }

        TreeMaker make = getTreeMaker();

        JCMethodInvocation methodCall;
        Attribute.Compound staticAnno = getAnnotationMirror( tree.sym, Static.class );
        JCExpression receiver = (tree.sym.owner.type.isInterface() ? staticAnno != null : tree.sym.isStatic())
          ? make.Type( tree.sym.owner.type )
          : make.This( _backingSymbols.peek().fst.type ).setPos( tree.pos );
        methodCall = make.Apply( List.nil(), make.Select( receiver, getMethod ).setPos( tree.pos ), List.nil() );
        methodCall = configMethod( tree, getMethod, methodCall );

        return methodCall;
      }
      else
      {
        reportError( tree, MSG_CANNOT_ACCESS_WRITEONLY_PROPERTY.get( tree.sym.flatName() ) );
      }
      return tree;
    }

    private int tempVarIndex;
    @Override
    public void visitAssign( JCAssign tree )
    {
      super.visitAssign( tree );

      TreeMaker make = getTreeMaker();

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
      else if( tree.lhs instanceof JCIdent )
      {
        Symbol sym = ((JCIdent)tree.lhs).sym;
        if( sym == null )
        {
          // sym is null if the property is directly in a top level class, but it's not the class of the file name
          reportError( tree, "Manifold @var properties are not supported in sidecar classes, consider nesting the class instead" );
          return;
        }

        if( sym.owner instanceof ClassSymbol )
        {
          JCIdent ident = (JCIdent)tree.lhs;
          lhs = ident;
          lhsSelectedType = _backingSymbols.peek().fst.type;
          if( lhsSelectedType == null )
          {
            return;
          }
          lhsSym = ident.sym;
          Attribute.Compound staticAnno = getAnnotationMirror( lhsSym, Static.class );
          lhsSelected = (lhsSym.owner.type.isInterface() ? staticAnno != null : lhsSym.isStatic())
            ? make.Type( lhsSym.owner.type )
            : make.This( lhsSelectedType ).setPos( tree.pos );
        }
        else
        {
          return;
        }
      }
      else
      {
        return;
      }

      JCClassDecl classDecl = _backingSymbols.peek().fst;
      if( !isPropertyField( lhsSym ) || keepRefToField( lhs, lhsSym, classDecl ) )
      {
        return;
      }

      JCMethodDecl methodDecl = _methodDefs.isEmpty() ? null : _methodDefs.peek();

      // replace  foo.bar = baz  with  foo.setBar(baz)

      MethodSymbol setMethod = isWritableProperty( lhsSym )
        ? resolveSetMethod( lhsSelectedType, lhsSym, Types.instance( _context ) )
        : null;

      if( setMethod != null )
      {
        if( methodDecl != null && (methodDecl.sym == setMethod ||
          (tree.lhs instanceof JCIdent && overrides( methodDecl.sym, setMethod ))) )
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

        List<JCTree> tempVars = List.nil();
        if( !(getParent( tree ) instanceof JCExpressionStatement) )
        {
          tempVarIndex++;
          JCTree[] rhsTemp = ExtensionTransformer.tempify( false, tree, make, rhs, _context,
            ExtensionTransformer.getEnclosingSymbol( tree, _context, child -> getParent( child ) ), "setPropRhsTempVar" + tempVarIndex, tempVarIndex );
          if( rhsTemp != null )
          {
            tempVars = tempVars.append( rhsTemp[0] );
            rhs = (JCExpression)rhsTemp[1];
          }
        }

        JCTree.JCMethodInvocation setCall = make.Apply( List.nil(), make.Select( lhsSelected, setMethod ).setPos( tree.pos ), List.of( rhs ) );
        setCall = configMethod( lhs, setMethod, setCall );

        if( !(getParent( tree ) instanceof JCExpressionStatement) )
        {
          // not really a var decl stmt, this is sneaking in a method call statement (turns out the LetExpr doesn't really require JCVarDecl, score!)
          tempVars = tempVars.append( make.Exec( setCall ).setPos( tree.pos ) );
          result = ILetExpr.makeLetExpr( make, tempVars, rhs, rhs.type, tree.pos );
        }
        else
        {
          result = setCall;
        }
      }
      else // errant
      {
        handleErrantAssignment( tree, lhsSym, lhsSelected, methodDecl );
      }
    }

    public void handleErrantAssignment( JCTree tree, Symbol lhsSym, JCExpression lhsSelected, JCMethodDecl methodDecl )
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
        // - no setter, allow the read-only property to initialize in its constructor
        // - backing symbol required, add to set of backing symbols
        _backingSymbols.peek().snd.add( (VarSymbol)lhsSym );
      }
// @val is same as final field, but lets us apply 'final' to getter/setter methods as not overridable.
// Essentially, ALL the modifiers on a property apply to the accessors.
// So, it's not just saying "there's only a getter", it also means the class can't update the backing field
// in places other than the initializer and the constructor. If you want to make it behave like "just a getter"
// on a private field, do this:  @val @set(Private) String name;
//      else if( _backingSymbols.peek().fst.sym.outermostClass() == lhsSym.outermostClass() )
//      {
//        // - no setter, allow the read-only prop field to be directly assigned inside the class file
//        // - backing symbol required, add to set of backing symbols
//        _backingSymbols.peek().snd.add( (VarSymbol)lhsSym );
//      }
      else
      {
        reportError( tree, MSG_CANNOT_ASSIGN_READONLY_PROPERTY.get( lhsSym.flatName() ) );
      }
    }

    @Override
    public void visitUnary( JCUnary tree )
    {
      super.visitUnary( tree );

      switch( tree.getTag() )
      {
        case PREINC:
        case PREDEC:
        case POSTINC:
        case POSTDEC:
          genUnaryIncDec( tree );
          break;
      }
    }

    private void genUnaryIncDec( JCTree.JCUnary tree )
    {
      TreeMaker make = getTreeMaker();

      JCExpression lhs;
      Type lhsSelectedType;
      Symbol lhsSym;
      JCExpression lhsSelected;
      if( tree.arg instanceof JCTree.JCFieldAccess )
      {
        JCFieldAccess fieldAccess = (JCTree.JCFieldAccess)tree.arg;
        lhs = fieldAccess;
        lhsSelectedType = fieldAccess.selected.type;
        lhsSym = fieldAccess.sym;
        lhsSelected = fieldAccess.selected;
      }
      else if( tree.arg instanceof JCIdent && ((JCIdent)tree.arg).sym.owner instanceof ClassSymbol )
      {
        JCIdent ident = (JCIdent)tree.arg;
        lhs = ident;
        lhsSelectedType = _backingSymbols.peek().fst.type;
        lhsSym = ident.sym;
        Attribute.Compound staticAnno = getAnnotationMirror( lhsSym, Static.class );
        lhsSelected = (lhsSym.owner.type.isInterface() ? staticAnno != null : lhsSym.isStatic())
          ? make.Type( lhsSym.owner.type )
          : make.This( lhsSelectedType ).setPos( tree.pos );
      }
      else
      {
        return;
      }

      JCClassDecl classDecl = _backingSymbols.peek().fst;
      if( !isPropertyField( lhsSym ) || keepRefToField( lhs, lhsSym, classDecl ) )
      {
        return;
      }

      JCMethodDecl methodDecl = _methodDefs.isEmpty() ? null : _methodDefs.peek();

      // replace  foo.bar++  with  foo.setBar( foo.getBar() + 1 )

      MethodSymbol setMethod = isWritableProperty( lhsSym )
        ? resolveSetMethod( lhsSelectedType, lhsSym, Types.instance( _context ) )
        : null;

      if( setMethod != null )
      {
        if( methodDecl != null && (methodDecl.sym == setMethod ||
          (tree.arg instanceof JCIdent && overrides( methodDecl.sym, setMethod ))) )
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


        JCExpression arg;
        List<JCTree> tempVars = List.nil();

        // tempify lhsSelected because it is used both in the getter call and the setter call
        tempVarIndex++;
        JCTree[] argTemp = ExtensionTransformer.tempify( false, tree, make, lhsSelected, _context,
          ExtensionTransformer.getEnclosingSymbol( tree, _context, child -> getParent( child ) ), "setPropArgTempVar" + tempVarIndex, tempVarIndex );
        if( argTemp != null )
        {
          tempVars = tempVars.append( argTemp[0] );
          lhsSelected = (JCExpression)argTemp[1];

          // rewrite the arg to use the temp var
          Type t = tree.arg.type;
          tree.arg = lhs = make.Select( lhsSelected, lhsSym );
          tree.arg.pos = tree.pos;
          tree.arg.type = t;
        }

        // replace the arg with a getter call
        arg = tree.arg instanceof JCIdent
          ? replaceWithGetter( (JCIdent)tree.arg )
          : repalceWithGetter( (JCFieldAccess)tree.arg );
        arg.setPos( tree.pos );

        // now, tempify the arg, since it must be preserved as the result of the post inc/dec
        tempVarIndex++;
        argTemp = ExtensionTransformer.tempify( false, tree, make, arg, _context,
          ExtensionTransformer.getEnclosingSymbol( tree, _context, child -> getParent( child ) ), "setPropArgTempVar" + tempVarIndex, tempVarIndex );
        if( argTemp != null )
        {
          tempVars = tempVars.append( argTemp[0] );
          arg = (JCExpression)argTemp[1];
        }

        // make the getXxx() + 1 call
        JCExpression rhs = makeUnaryIncDecCall( tree, make, arg );

        // tempify the rhs so it can be returned for pre-inc/dec
        tempVarIndex++;
        argTemp = ExtensionTransformer.tempify( false, tree, make, rhs, _context,
          ExtensionTransformer.getEnclosingSymbol( tree, _context, child -> getParent( child ) ), "setPropArgTempVar" + tempVarIndex, tempVarIndex );
        tempVars = tempVars.append( argTemp[0] );
        rhs = (JCExpression)argTemp[1];

        // make the setXxx(rhs) call
        JCTree.JCMethodInvocation setCall = make.Apply( List.nil(), make.Select( lhsSelected, setMethod ).setPos( tree.pos ), List.of( rhs ) );
        setCall = (JCMethodInvocation)configMethod( lhs, setMethod, setCall ).setPos( tree.pos );

        // not really a var decl stmt, this is sneaking in a method call statement (turns out the LetExpr doesn't really require JCVarDecl, score!)
        tempVars = tempVars.append( make.Exec( setCall ).setPos( tree.pos ) );
        result = ILetExpr.makeLetExpr( make, tempVars, tree.getTag().isPostUnaryOp() ? arg : rhs, arg.type, tree.pos );
      }
      else // errant
      {
        handleErrantAssignment( tree, lhsSym, lhsSelected, methodDecl );
      }
    }

//    private boolean verifyAccess( JCExpression ref, Symbol propSym, MethodSymbol accessorMethod, String accessKind )
//    {
//      if( !sameAccess( accessorMethod, propSym ) )
//      {
//        JCClassDecl classDecl = _backingSymbols.peek().fst;
//        Resolve resolve = Resolve.instance( _context );
//        AttrContext attrContext = new AttrContext();
//        Env<AttrContext> env = new AttrContextEnv( ref, attrContext );
//        env.toplevel = getCompilationUnit();
//        env.enclClass = classDecl;
//
////alternative way, just as bad
////        JavacTrees trees = JavacTrees.instance( _context );
////        TreePath path = trees.getPath( getCompilationUnit(), ref );
////        boolean accessible = trees.isAccessible( trees.getScope( path ), accessorMethod, (DeclaredType)classDecl.type );
//
////this doesn't really work all that well, for now rely on indirect errors from the accessors, maybe intercept those errors and rewrite them in terms of properties?
//        if( !resolve.isAccessible( env, classDecl.type, accessorMethod, true ) )
//        {
//          reportError( ref, MSG_PROPERTY_NOT_ACCESSIBLE.get( accessKind, propSym.flatName(),
//            PropOption.fromModifier( getAccess( accessorMethod ) ).name().toLowerCase() ) );
//          return false;
//        }
//      }
//      return true;
//    }

    // for now...
    @SuppressWarnings( "unused" )
    private boolean verifyAccess( JCExpression ref, Symbol propSym, MethodSymbol accessorMethod, String accessKind )
    {
      if( !sameAccess( accessorMethod, propSym ) )
      {
        JCClassDecl classDecl = _backingSymbols.peek().fst;
        if( !isAccessible( accessorMethod, classDecl ) )
        {
          reportError( ref, MSG_PROPERTY_NOT_ACCESSIBLE.get( accessKind, propSym.flatName(),
            PropOption.fromModifier( getAccess( accessorMethod ) ).name().toLowerCase() ) );
          return false;
        }
      }
      return true;
    }
    // retarded check until Resolve.isAccessible/env is better understood
    private boolean isAccessible( Symbol.MethodSymbol member, JCTree.JCClassDecl siteClass )
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

    private boolean overrides( MethodSymbol enclosingMethod, MethodSymbol getMethod )
    {
      return enclosingMethod.name.equals( getMethod.name ) &&
        enclosingMethod.overrides( getMethod, getMethod.enclClass(), Types.instance( _context ), false );
    }

    /**
     * Keep field refs to *auto* prop fields as-is when they have access to the existing field as it was originally
     * declared. Basically, auto-properties are for the convenience of *consumers* of the declaring class. If the author
     * of the class wants to access stuff inside his implementation using property syntax, he should explicitly declare
     * properties.
     */
    private boolean keepRefToField( JCExpression tree, Symbol sym, JCClassDecl classDecl )
    {
      if( ExtensionTransformer.isJailbreakReceiver( tree ) )
      {
        return true;
      }

      Attribute.Compound auto = getAnnotationMirror( sym, auto.class );
      if( auto == null )
      {
        return false;
      }

      int declaredAccess = getDeclaredAccess( auto );
      switch( declaredAccess )
      {
        case PRIVATE:
          // same class as field
          return classDecl.sym.outermostClass() == sym.outermostClass();
        case 0: // PACKAGE
          // same package as field's class
          return classDecl.sym.packge() == sym.packge();
        case PROTECTED:
          // sublcass of field's class
          return classDecl.sym.isSubClass( sym.enclClass(), Types.instance( _context ) );
        case PUBLIC:
          // field is public, no dice
          return true;
        case -1: // indicates no existing field
          if( tree instanceof JCIdent && !_methodDefs.isEmpty() && _methodDefs.peek().sym.enclClass().isAnonymous() )
          {
            // can't reference an inferred property from within the declaring class (or inner class within same top-level class)
            reportError( tree, MSG_NASTY_INFERRED_PROPERTY_REF.get( sym.name ) );
          }
          return false;
        default:
          throw new IllegalStateException( "Unknown or invalid access privilege: " + declaredAccess );
      }
    }

    private JCTree.JCExpression makeUnaryIncDecCall( JCTree.JCUnary tree, TreeMaker make, JCExpression operand )
    {
      Type unboxedType = getTypes().unboxedType( operand.type );
      if( unboxedType != null && !unboxedType.hasTag( NONE ) )
      {
        operand = ExtensionTransformer.unbox( getTypes(), getTreeMaker(), Names.instance( getContext() ),
          getContext(), getCompilationUnit(), operand, unboxedType );
      }

      JCTree.JCExpression one = make.Literal( 1 );
      one.pos = tree.pos;
      one = make.TypeCast( operand.type, one );
      one.pos = tree.pos;
      JCTree.JCBinary binary = make.Binary(
        tree.getTag() == JCTree.Tag.PREINC || tree.getTag() == JCTree.Tag.POSTINC
          ? JCTree.Tag.PLUS
          : JCTree.Tag.MINUS, operand, one );
      binary.pos = tree.pos;
      binary.type = operand.type;
      Env<AttrContext> env = new AttrContextEnv( tree, new AttrContext() );
      env.toplevel = (JCTree.JCCompilationUnit)getCompilationUnit();
      env.enclClass = ExtensionTransformer.getEnclosingClass( tree, child -> getParent( child ) );
      if( JreUtil.isJava8() )
      {
        binary.operator = ExtensionTransformer
          .resolveMethod( getContext(), getCompilationUnit(), tree.pos(),
            Names.instance( getContext() ).fromString( binary.getTag() == Tag.PLUS ? "+" : "-" ),
            getSymtab().predefClass.type, List.of( binary.lhs.type, binary.rhs.type ) );
      }
      else
      {
        //reflective: binary.operator = Operators.instance( _tp.getContext ).resolveBinary( ... );
        Object operators = ReflectUtil.method( "com.sun.tools.javac.comp.Operators", "instance", Context.class )
          .invokeStatic( getContext() );
        ReflectUtil.field( binary, "operator" )
          .set( ReflectUtil.method( operators, "resolveBinary", JCDiagnostic.DiagnosticPosition.class, JCTree.Tag.class, Type.class, Type.class )
            .invoke( tree.pos(), binary.getTag(), binary.lhs.type, binary.rhs.type ) );
      }

      return binary;

      // maybe unbox here?
    }

    private JCTree.JCMethodInvocation configMethod( JCTree.JCExpression tree, MethodSymbol methodSym, JCTree.JCMethodInvocation methodTree )
    {
      methodTree.setPos( tree.pos );
      methodTree.type = methodSym.getReturnType();

      //!! not needed, the TypeProcessor processes the ast after properties
      // If methodCall is an extension method, rewrite it
//      methodTree = ExtensionTransformer.maybeReplaceWithExtensionMethod( methodTree );

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
    private void handleClass( TypeElement typeSym )
    {
      if( !(typeSym instanceof ClassSymbol) )
      {
        return;
      }

      ClassSymbol classSym = (ClassSymbol)typeSym;

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
        // make the field a backing field with PRIVATE access, or restore the @auto prop field's declared access

        int access = PRIVATE;
        Attribute.Compound auto = getAnnotationMirror( fieldSym, auto.class );
        if( auto != null )
        {
          int declaredAccess = getDeclaredAccess( auto );
          if( declaredAccess >= 0 )
          {
            access = declaredAccess;
          }
        }

        fieldSym.flags_field = fieldSym.flags_field & ~(PUBLIC | PROTECTED | PRIVATE) | access;

        // store the original access modifier in @propgen(name, flags) so we can restore the field's access upon
        // loading from .class file

        if( fieldSym.getRawAttributes().stream() // skip if already annotated (from multiple rounds)
          .noneMatch( c -> c.getAnnotationType().toString().contains( propgen.class.getSimpleName() ) ) )
        {
          Names names = Names.instance( _context );
          Symtab symtab = Symtab.instance( _context );
          ClassSymbol propgenSym = IDynamicJdk.instance().getTypeElement( _context,
            getCompilationUnit(), propgen.class.getTypeName() );
          MethodSymbol nameMeth = (MethodSymbol)IDynamicJdk.instance().getMembersByName( propgenSym, names.fromString( "name" ) ).iterator().next();
          MethodSymbol flagsMeth = (MethodSymbol)IDynamicJdk.instance().getMembersByName( propgenSym, names.fromString( "flags" ) ).iterator().next();
          Attribute.Compound propGenAnno = new Attribute.Compound( propgenSym.type,
            List.of( new Pair<>( nameMeth, new Attribute.Constant( symtab.stringType, fieldSym.name.toString() ) ),
              new Pair<>( flagsMeth, new Attribute.Constant( symtab.longType, modifiers ) ) ) );
          fieldSym.appendAttributes( List.of( propGenAnno ) );

          Set<VarSymbol> props = _propMap.computeIfAbsent( classSym, e -> new HashSet<>() );
          props.add( fieldSym );
        }
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
    public void handleClass( TypeElement typeSym )
    {
      if( !(typeSym instanceof ClassSymbol) )
      {
        return;
      }

      ClassSymbol classSym = (ClassSymbol)typeSym;

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
          sym.appendAttributes( varSym.getAnnotationMirrors() ); // add the @var etc. annotations

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
    Types types = getTypes();

    if( type instanceof Type.TypeVar )
    {
      type = types.erasure( type );
    }

    if( !(type.tsym instanceof ClassSymbol) )
    {
      return null;
    }

    Type fieldType = types.memberType( type, field ); // the type of the field as a member of `type` e.g., a field  of type List<T> inside Bar<T> as seen from class Foo that extends Bar<String> ...
    MethodSymbol method = ManAttr.getMethodSymbol( types, type, fieldType, getGetterName( field, true ), (ClassSymbol)type.tsym, 0 );
    if( method == null )
    {
      method = ManAttr.getMethodSymbol( types, type, fieldType, getGetterName( field, false ), (ClassSymbol)type.tsym, 0 );
      if( method == null )
      {
        // If class is a record, try getting property from conventional, user-defined getter method
        if( field.owner != null && field.owner.getKind().name().equals( "RECORD" ) )
        {
          method = ManAttr.getMethodSymbol( types, type, fieldType, getGetterName( field, false, false ), (ClassSymbol)type.tsym, 0 );;
        }

      }
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

    Type fieldType = types.memberType( type, field ); // the type of the field as a member of `type` e.g., a field  of type List<T> inside Bar<T> as seen from class Foo that extends Bar<String> ...
    MethodSymbol setter = ManAttr.getMethodSymbol( types, type, fieldType, getSetterName( field.name ), (ClassSymbol)type.tsym, 1 );

    // handle property where isXxx is both field name and getter name isXxx(), look for setter by name of xxx.
    if( setter == null && isIsName( field.name.toString() ) && resolveGetMethod( type, field ) != null )
    {
      Names names = Names.instance( getContext() );
      Name name = names.fromString( ManStringUtil.uncapitalize( field.name.toString().substring( 2 ) ) );
      setter = ManAttr.getMethodSymbol( types, type, fieldType, getSetterName( name ), (ClassSymbol)type.tsym, 1 );
    }
    return setter;
  }

  private boolean isIsName( String name )
  {
    return name.length() > 2 && name.startsWith( "is" ) && Character.isUpperCase( name.charAt( 2 ) );
  }

  void addToBackingFields( VarSymbol propField )
  {
    _backingMap.computeIfAbsent( propField.enclClass(), key -> new HashSet<>() )
      .add( propField );
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

  private String getGetterName( Symbol field, boolean isOk )
  {
    return getGetterName( field, isOk, true );
  }
  private String getGetterName( Symbol field, boolean isOk, boolean supportRecordComps )
  {
    String name = field.name.toString();
    if( supportRecordComps &&
      field.owner != null && field.owner.getKind().name().equals( "RECORD" ) )
    {
      return name;
    }

    if( isOk && field.type == Symtab.instance( _context ).booleanType )
    {
      if( startsWithIs( name ) )
      {
        return name;
      }
      return "is" + ManStringUtil.capitalize( name );
    }
    return "get" + ManStringUtil.capitalize( name );
  }

  private String getGetterName( JCVariableDecl tree, @SuppressWarnings( "SameParameterValue" ) boolean isOk )
  {
    String name = tree.name.toString();
    if( isOk && tree.vartype.toString().equals( "boolean" ) )
    {
      if( startsWithIs( name ) )
      {
        return name;
      }
      return "is" + ManStringUtil.capitalize( name );
    }
    return "get" + ManStringUtil.capitalize( name );
  }

  private boolean startsWithIs( String name )
  {
    return name.length() > 2 && name.startsWith( "is" ) && Character.isUpperCase( name.charAt( 2 ) );
  }

  private String getSetterName( Name name )
  {
    return "set" + ManStringUtil.capitalize( name.toString() );
  }

  private void addAnnotation( VarSymbol fieldSym, @SuppressWarnings( "SameParameterValue" ) Class<? extends Annotation> annoClass )
  {
    ClassSymbol annoSym = IDynamicJdk.instance().getTypeElement( _context,
      getCompilationUnit(), annoClass.getTypeName() );
    Attribute.Compound anno = new Attribute.Compound( annoSym.type,List.nil() );
    fieldSym.appendAttributes( List.of( anno ) );
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
}
