/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.internal.javac;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.jvm.ClassReader;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.net.URI;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.sun.tools.javac.util.List;
import manifold.api.host.IModule;
import manifold.api.type.ITypeManifold;
import manifold.api.util.IssueMsg;
import manifold.internal.javac.AbstractBinder.Node;
import manifold.rt.api.FragmentValue;
import manifold.rt.api.Null;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.ManStringUtil;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

import javax.lang.model.element.ElementKind;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.TypeTag.CLASS;
import static com.sun.tools.javac.code.TypeTag.ERROR;
import static manifold.internal.javac.HostKind.DOUBLE_QUOTE_LITERAL;
import static manifold.internal.javac.HostKind.TEXT_BLOCK_LITERAL;
import static manifold.util.JreUtil.isJava8;

public interface ManAttr
{
  String AUTO_TYPE = "manifold.ext.rt.api.auto";

  Object Kind_TYP = JreUtil.isJava8()
    ? ReflectUtil.field( Kinds.class, "TYP" ).getStatic()
    : ReflectUtil.field( Kinds.class.getTypeName() + "$Kind", "TYP" ).getStatic();
  Object KindSelector_TYP = JreUtil.isJava8()
    ? ReflectUtil.field( Kinds.class, "TYP" ).getStatic()
    : ReflectUtil.field( Kinds.class.getTypeName() + "$KindSelector", "TYP" ).getStatic();
  Object KindSelector_PCK = JreUtil.isJava8()
    ? ReflectUtil.field( Kinds.class, "PCK" ).getStatic()
    : ReflectUtil.field( Kinds.class.getTypeName() + "$KindSelector", "PCK" ).getStatic();
  Object KindSelector_VAL = JreUtil.isJava8()
    ? ReflectUtil.field( Kinds.class, "VAL" ).getStatic()
    : ReflectUtil.field( Kinds.class.getTypeName() + "$KindSelector", "VAL" ).getStatic();
  Object KindSelector_MTH = JreUtil.isJava8()
    ? ReflectUtil.field( Kinds.class, "MTH" ).getStatic()
    : ReflectUtil.field( Kinds.class.getTypeName() + "$KindSelector", "MTH" ).getStatic();

  boolean JAILBREAK_PRIVATE_FROM_SUPERS = true;

  String COMPARE_TO = "compareTo";
  String COMPARE_TO_USING = "compareToUsing";
  String UNARY_MINUS = "unaryMinus";
  String NOT = "not";
  String COMPL = "inv";
  String INC = "inc";
  String DEC = "dec";
  Map<Tag, String> BINARY_OP_TO_NAME = new HashMap<Tag, String>()
  {{
    put( Tag.PLUS, "plus" );
    put( Tag.MINUS, "minus" );
    put( Tag.MUL, "times" );
    put( Tag.DIV, "div" );
    put( Tag.MOD, "rem" );

    put( Tag.BITAND, "and" );
    put( Tag.BITOR, "or" );
    put( Tag.BITXOR, "xor" );

    put( Tag.SL, "shl" );
    put( Tag.SR, "shr" );
    put( Tag.USR, "ushr" );
    // note ==, !=, >, >=, <, <=  are covered via ComparableUsing
  }};

  JCTree.JCMethodDecl peekMethodDef();

  JCTree.JCFieldAccess peekSelect();

  JCTree.JCAnnotatedType peekAnnotatedType();

  default Env getEnv()
  {
    return (Env)ReflectUtil.field( this, "env" ).get();
  }

  default Log getLogger()
  {
    return (Log)ReflectUtil.field( this, "log" ).get();
  }

  default Object resultInfo()
  {
    return ReflectUtil.field( this, "resultInfo" ).get();
  }

  default Check chk()
  {
    return (Check)ReflectUtil.field( this, "chk" ).get();
  }

  default Resolve rs()
  {
    return (Resolve)ReflectUtil.field( this, "rs" ).get();
  }

  default Names names()
  {
    return (Names)ReflectUtil.field( this, "names" ).get();
  }

  default Types types()
  {
    return (Types)ReflectUtil.field( this, "types" ).get();
  }

  default Object cfolder()
  {
    return ReflectUtil.field( this, "cfolder" ).get();
  }

  default Symtab syms()
  {
    return (Symtab)ReflectUtil.field( this, "syms" ).get();
  }

  default Object _pkind()
  {
    return ReflectUtil.method( this, "pkind" ).invoke();
  }

  default boolean handleFragmentStringLiteral( JCTree.JCLiteral tree )
  {
    if( tree.typetag == CLASS &&
            tree.value.toString().startsWith( FragmentProcessor.FRAGMENT_START ) &&
            tree.value.toString().contains( FragmentProcessor.FRAGMENT_END ) )
    {
      Tree parent = JavacPlugin.instance().getTypeProcessor().getParent( tree, getEnv().toplevel );
      if( !(parent instanceof JCTree.JCBinary) ) // fragments are not supported with '+' concatenation
      {
        Type type = getFragmentValueType( tree );
        if( type != null )
        {
          tree.type = type;
          ReflectUtil.field( this, "result" ).set( type );
          return true;
        }
      }
    }
    return false;
  }

  default Type getFragmentValueType( JCTree.JCLiteral tree )
  {
    int endPosition = tree.pos().getEndPosition( getEnv().toplevel.endPositions );
    if( endPosition < 0 )
    {
      // this is almost certainly harmless, since it is indicative of an intermediate compiler pass
      //getLogger().note( tree.pos().getStartPosition(), "proc.messager", "No end position for string literal manifold fragment: \n\"" + tree.getValue() + "\"" );
      return syms().stringType.constType( tree.value );
    }

    CharSequence source = ParserFactoryFiles.getSource( getEnv().toplevel.sourcefile );
    CharSequence chars = source.subSequence( tree.pos().getStartPosition(), endPosition );
    HostKind hostKind = chars.length() > 3 && chars.charAt( 1 ) == '"'
            ? TEXT_BLOCK_LITERAL
            : DOUBLE_QUOTE_LITERAL;
    if( ManAttr.checkConcatenation( tree, chars, hostKind, getLogger() ) )
    {
      // string concat not supported with fragments
      return syms().stringType.constType( tree.value );
    }

    FragmentProcessor.Fragment fragment = FragmentProcessor.instance().parseFragment( tree.pos().getStartPosition(), chars.toString(), hostKind );
    if( fragment != null )
    {
      String fragClass = getEnv().toplevel.packge.toString() + '.' + fragment.getName();
      Symbol.ClassSymbol fragSym = IDynamicJdk.instance().getTypeElement( JavacPlugin.instance().getContext(), getEnv().toplevel, fragClass );
      for( Attribute.Compound annotation: fragSym.getAnnotationMirrors() )
      {
        if( annotation.type.toString().equals( FragmentValue.class.getName() ) )
        {
          Type type = getFragmentValueType( annotation );
          if( type != null )
          {
            return type;
          }
        }
      }
      getLogger().rawWarning( tree.pos().getStartPosition(),
              "No @" + FragmentValue.class.getSimpleName() + " is provided for metatype '" + fragment.getExt() + "'. The resulting value remains a String literal." );
    }
    return syms().stringType.constType( tree.value );
  }

  default Type getFragmentValueType( Attribute.Compound attribute )
  {
    String type = null;
    for( com.sun.tools.javac.util.Pair<Symbol.MethodSymbol, Attribute> pair: attribute.values )
    {
      Name argName = pair.fst.getSimpleName();
      if( argName.toString().equals( "type" ) )
      {
        type = (String)pair.snd.getValue();
      }
    }

    if( type != null )
    {
      Symbol.ClassSymbol fragValueSym = IDynamicJdk.instance().getTypeElement( JavacPlugin.instance().getContext(), getEnv().toplevel, type );
      if( fragValueSym != null )
      {
        return fragValueSym.type;
      }
    }

    return null;
  }

  default void patchMethodType( JCTree.JCMethodInvocation tree, Set<JCTree.JCMethodInvocation> visited )
  {
    patchOperatorMethodType( tree );

    if( visited.contains( tree ) )
    {
      // not handling non-tail recursive types (for now)
      return;
    }
    visited.add( tree );
    try
    {
      patchAutoReturnType( tree );
    }
    finally
    {
      visited.remove( tree );
    }
  }

  default void patchOperatorMethodType( JCTree.JCMethodInvocation tree )
  {
    if( tree.meth.type == null )
    {
      if( tree.meth instanceof JCTree.JCIdent )
      {
        Symbol sym = ((JCTree.JCIdent)tree.meth).sym;
        if( sym != null )
        {
          tree.meth.type = sym.type;
        }
      }
      else if( tree.meth instanceof JCTree.JCFieldAccess )
      {
        Symbol sym = ((JCTree.JCFieldAccess)tree.meth).sym;
        if( sym != null )
        {
          tree.meth.type = sym.type;
        }
      }
    }
  }

  /**
   * If the field access returns `auto`, this indicates the field definition is not fully compiled yet (type attribution),
   * otherwise the `auto` type would be a real type inferred from the field's initializer. Therefore, we force the
   * field to attribute so that the field access can be properly typed (to access the members declared in the
   * inferred type).
   */
  default void patchAutoFieldType( JCTree.JCExpression tree )
  {
    Type type = tree.type;
    if( !isAutoType( type ) )
    {
      return;
    }
                      
    Symbol.VarSymbol vsym = null;
    if( tree instanceof JCTree.JCIdent &&
      ((JCTree.JCIdent)tree).sym instanceof Symbol.VarSymbol )
    {
      vsym = (Symbol.VarSymbol)((JCTree.JCIdent)tree).sym;
    }
    else if( tree instanceof JCTree.JCFieldAccess &&
      ((JCTree.JCFieldAccess)tree).sym instanceof Symbol.VarSymbol )
    {
      vsym = (Symbol.VarSymbol)((JCTree.JCFieldAccess)tree).sym;
    }

    if( vsym != null )
    {
      Symbol.ClassSymbol declaringClassSym = (Symbol.ClassSymbol)vsym.owner;
      JCTree.JCClassDecl enclosingClass = getEnclosingClass( tree );

      MyDiagnosticHandler diagHandler = new MyDiagnosticHandler( getLogger() );
      try
      {
        if( enclosingClass != null && enclosingClass.sym == declaringClassSym )
        {
          // the field is in the same class as the field access site,
          // force the FIELD to attribute, so we can get the actual field type
          JCTree.JCVariableDecl varDecl = findJCVariableDecl( enclosingClass, vsym );
          ((Attr)this).attribStat( varDecl, getEnv() );
        }
        else
        {
          // the field is not in the same class as the field access site,
          // force the CLASS to attribute, so we can get the actual field type
          ((Attr)this).attribClass( tree.pos(), declaringClassSym );
        }
      }
      finally
      {
        getLogger().popDiagnosticHandler( diagHandler );
      }

      // get the potentially attributed return type
      type = vsym.type;
      if( isAutoType( type ) )
      {
        // failed to attribute

        if( enclosingClass != null && enclosingClass.sym != declaringClassSym )
        {
          diagHandler = new MyDiagnosticHandler( getLogger() );
          try
          {
            // the field is not in the same class as the field access site, but the class failed to fully attribute
            // probably due to a reference cycle. Try to force just the FIELD to attribute.
            //noinspection unchecked
            Env<AttrContext> declClassEnv = (Env<AttrContext>)ReflectUtil.method(
                ReflectUtil.field( this, "typeEnvs" ).get(), "get", Symbol.TypeSymbol.class )
              .invoke( declaringClassSym );
            JCTree.JCClassDecl declaringClass = (JCTree.JCClassDecl)declClassEnv.tree;
            JCTree.JCVariableDecl varDecl = findJCVariableDecl( declaringClass, vsym );
            // not supporting non-tail recursive methods (for now)
//            ((Attr)this).attribStat( varDecl, declClassEnv );
            type = ((Attr)this).attribStat( varDecl, declClassEnv );
          }
          finally
          {
            getLogger().popDiagnosticHandler( diagHandler );
          }
        }
      }
      if( isAutoType( type ) )
      {
        // bad, the class did/could not attribute types
        IDynamicJdk.instance().logError( Log.instance( JavacPlugin.instance().getContext() ), tree.pos(),
          "proc.messager", IssueMsg.MSG_AUTO_UNABLE_TO_RESOLVE_TYPE.get() );
      }
      else
      {
        // good, the more specific type is there now, set `this.result = type`
        tree.type = type;
        ReflectUtil.field( this, "result" ).set( type );
      }
    }
  }
  /**
   * If the method call returns `auto`, this indicates the method definition is not fully compiled yet (type attribution),
   * otherwise the `auto` type would be a real type inferred from the return statements. Therefore, we force the
   * method to attribute types so that the method call can be properly typed (to access the members declared in the
   * inferred type).
   */
  default void patchAutoReturnType( JCTree.JCMethodInvocation tree )
  {
    Type type = tree.type;
    if( !isAutoType( type ) )
    {
      return;
    }

    Symbol.MethodSymbol msym = null;
    if( tree.meth instanceof JCTree.JCIdent &&
      ((JCTree.JCIdent)tree.meth).sym instanceof Symbol.MethodSymbol )
    {
      msym = (Symbol.MethodSymbol)((JCTree.JCIdent)tree.meth).sym;
    }
    else if( tree.meth instanceof JCTree.JCFieldAccess &&
      ((JCTree.JCFieldAccess)tree.meth).sym instanceof Symbol.MethodSymbol )
    {
      msym = (Symbol.MethodSymbol)((JCTree.JCFieldAccess)tree.meth).sym;
    }

    if( msym != null )
    {
      Symbol.ClassSymbol declaringClassSym = (Symbol.ClassSymbol)msym.owner;
      JCTree.JCClassDecl enclosingClass = getEnclosingClass( tree );

      MyDiagnosticHandler diagHandler = new MyDiagnosticHandler( getLogger() );
      try
      {
        if( enclosingClass != null && enclosingClass.sym == declaringClassSym )
        {
          // the called method is in the same class as the call site,
          // force the METHOD to attribute, so we can get the actual return type
          JCTree.JCMethodDecl methodDef = findJCMethodDef( enclosingClass, msym );
          ((Attr)this).attribStat( methodDef, getEnv() );
        }
        else
        {
          // the called method is not in the same class as the call site,
          // force the CLASS to attribute, so we can get the actual return type
          ((Attr)this).attribClass( tree.pos(), declaringClassSym );
        }
      }
      finally
      {
        getLogger().popDiagnosticHandler( diagHandler );
      }

      // get the potentially attributed return type
      type = msym.getReturnType();
      if( isAutoType( type ) )
      {
        // failed to attribute

        if( enclosingClass != null && enclosingClass.sym != declaringClassSym )
        {
          diagHandler = new MyDiagnosticHandler( getLogger() );
          try
          {
            // the called method is not in the same class as the call site, but the class failed to fully attribute
            // probably due to a reference cycle. Try to force just the called METHOD to attribute.
            //noinspection unchecked
            Env<AttrContext> declClassEnv = (Env<AttrContext>)ReflectUtil.method(
                ReflectUtil.field( this, "typeEnvs" ).get(), "get", Symbol.TypeSymbol.class )
              .invoke( declaringClassSym );
            JCTree.JCClassDecl declaringClass = (JCTree.JCClassDecl)declClassEnv.tree;
            JCTree.JCMethodDecl methodDef = findJCMethodDef( declaringClass, msym );
            if( methodDef != getEnv().enclMethod )
            {
              // not supporting non-tail recursive methods (for now)
              ((Attr)this).attribStat( methodDef, declClassEnv );
              type = msym.getReturnType();
            }
          }
          finally
          {
            getLogger().popDiagnosticHandler( diagHandler );
          }
        }
      }
      if( isAutoType( type ) )
      {
        // bad, the class did/could not attribute types
        IDynamicJdk.instance().logError( Log.instance( JavacPlugin.instance().getContext() ), tree.meth.pos(),
          "proc.messager", IssueMsg.MSG_AUTO_UNABLE_TO_RESOLVE_TYPE.get() );
      }
      else
      {
        // good, the more specific type is there now, set `this.result = type`
        tree.type = type;
        ReflectUtil.field( this, "result" ).set( type );

        // ensure the inferred type is assignable to the lhs type: Foo result = autoMeth(); where must be assignable to Foo
        ReflectUtil.method( ReflectUtil.field( this, "resultInfo" ).get(), "check", JCDiagnostic.DiagnosticPosition.class, Type.class )
          .invoke( tree.pos(), type );
      }
    }
  }

  default JCTree.JCMethodDecl findJCMethodDef( JCTree.JCClassDecl tree, Symbol.MethodSymbol msym )
  {
    for( JCTree def: tree.defs )
    {
      if( def instanceof JCTree.JCMethodDecl && ((JCTree.JCMethodDecl)def).sym == msym )
      {
        return (JCTree.JCMethodDecl)def;
      }
    }
    return null;
  }

  default JCTree.JCVariableDecl findJCVariableDecl( JCTree.JCClassDecl tree, Symbol.VarSymbol vsym )
  {
    for( JCTree def: tree.defs )
    {
      if( def instanceof JCTree.JCVariableDecl && ((JCTree.JCVariableDecl)def).sym == vsym )
      {
        return (JCTree.JCVariableDecl)def;
      }
    }
    return null;
  }

  default JCTree.JCClassDecl getEnclosingClass( Tree tree )
  {
    if( tree == null )
    {
      return null;
    }
    if( tree instanceof JCTree.JCClassDecl )
    {
      return (JCTree.JCClassDecl)tree;
    }
    return getEnclosingClass( JavacPlugin.instance().getTypeProcessor().getParent( tree, getEnv().toplevel ) );
  }

  default boolean isAutoType( Type type )
  {
    return type != null && type.tsym != null && type.tsym.getQualifiedName().toString().equals( AUTO_TYPE );
  }

  /**
   * Facilitates handling shadowing where an instance field shadows an inner class of the same name.
   * This is particularly useful in code gen where both a property and an inner type are derived from the same element.
   * Otherwise, use-cases such as: Person.address.builder(), don't work because address resolves as the field and not
   * the inner class.
   */
  default void restoreDiagnostics( JCTree.JCFieldAccess tree, DeferredAttrDiagHandler deferredAttrDiagHandler )
  {
    Queue<JCDiagnostic> diagnostics = deferredAttrDiagHandler.getDiagnostics();
    if( !diagnostics.isEmpty() )
    {
      if( ((tree.selected instanceof JCTree.JCIdent && isType( ((JCTree.JCIdent)tree.selected).sym )) ||
        (tree.selected instanceof JCTree.JCFieldAccess) && isType( ((JCTree.JCFieldAccess)tree.selected).sym )) &&
        tree.sym instanceof Symbol.VarSymbol &&
        tree.name.equals( tree.type.tsym.getSimpleName() ) )
      {
        getLogger().popDiagnosticHandler( deferredAttrDiagHandler );
        tree.sym = null;
        if( tree.selected instanceof JCTree.JCIdent )
        {
          ((JCTree.JCIdent)tree.selected).sym = null;
        }
        else
        {
          ((JCTree.JCFieldAccess)tree.selected).sym = null;
        }
        ReflectUtil.field( resultInfo(), "pkind" ).set( KindSelector_TYP );

        ReflectUtil.method( this, "visitSelect", JCTree.JCFieldAccess.class ).invokeSuper( tree );
//        super.visitSelect( tree );

        return;
      }

      deferredAttrDiagHandler.reportDeferredDiagnostics();
    }
    getLogger().popDiagnosticHandler( deferredAttrDiagHandler );
  }
  static boolean isType( Symbol sym )
  {
    return sym != null && Objects.equals( ReflectUtil.field( sym, "kind" ).get(), Kind_TYP );
  }
  default DeferredAttrDiagHandler suppressDiagnositics( JCTree.JCFieldAccess tree )
  {
    return new DeferredAttrDiagHandler( getLogger(), tree );
  }

  class DeferredDiagnosticHandler extends Log.DiagnosticHandler
  {
    private Queue<JCDiagnostic> deferred = new ListBuffer<>();
    private final Predicate<JCDiagnostic> filter;

    public DeferredDiagnosticHandler(Log log) {
      this(log, null);
    }

    public DeferredDiagnosticHandler(Log log, Predicate<JCDiagnostic> filter) {
      this.filter = filter;
      install(log);
    }

    @Override
    public void report(JCDiagnostic diag) {
      if (!diag.isFlagSet(JCDiagnostic.DiagnosticFlag.NON_DEFERRABLE) &&
        (filter == null || filter.test(diag))) {
        deferred.add(diag);
      } else {
        prev.report(diag);
      }
    }

    public Queue<JCDiagnostic> getDiagnostics() {
      return deferred;
    }

    /** Report all deferred diagnostics. */
    public void reportDeferredDiagnostics() {
      reportDeferredDiagnostics(EnumSet.allOf(JCDiagnostic.Kind.class));
    }

    /** Report selected deferred diagnostics. */
    public void reportDeferredDiagnostics(Set<JCDiagnostic.Kind> kinds) {
      JCDiagnostic d;
      while ((d = deferred.poll()) != null) {
        if (kinds.contains(d.getKind()))
          prev.report(d);
      }
      deferred = null; // prevent accidental ongoing use
    }
  }
  class DeferredAttrDiagHandler extends DeferredDiagnosticHandler
  {
    static class PosScanner extends TreeScanner
    {
      JCDiagnostic.DiagnosticPosition pos;
      boolean found = false;

      PosScanner( JCDiagnostic.DiagnosticPosition pos )
      {
        this.pos = pos;
      }

      @Override
      public void scan( JCTree tree )
      {
        if( tree != null &&
          tree.pos() == pos )
        {
          found = true;
        }
        super.scan( tree );
      }
    }
    DeferredAttrDiagHandler( Log log, JCTree newTree )
    {
      super( log, d -> {
        DeferredAttrDiagHandler.PosScanner posScanner = new DeferredAttrDiagHandler.PosScanner( d.getDiagnosticPosition() );
        posScanner.scan( newTree );
        return posScanner.found;
      } );
    }
  }

  /**
   * Handle properties in interfaces, which are non-static unless explicitly static.
   * This is necessary so that a non-static property can reference type variables in its type:  @var T element;
   */
  default void handleNonStaticInterfaceProperty( Env<AttrContext> env)
  {
    if( (((Scope)ReflectUtil.field( env.info, "scope" ).get()).owner.flags() & INTERFACE) != 0 )
    {
      if( env.tree instanceof JCTree.JCVariableDecl )
      {
        JCTree.JCModifiers mods = ((JCTree.JCVariableDecl)env.tree).mods;
        if( (mods.flags & Flags.STATIC) == 0 )
        {
          for( JCTree.JCAnnotation anno : mods.annotations )
          {
            if( isPropertyAnno( anno.annotationType ) )
            {
              // The env would have bumped up the staticLevel, so we simply
              // bump it back down when we know the property is non-static
              ReflectUtil.LiveFieldRef staticLevel = ReflectUtil.field( env.info, "staticLevel" );
              staticLevel.set( (int)staticLevel.get() - 1 );
            }
          }
        }
      }
    }
  }
  default boolean isPropertyAnno( JCTree annotationType )
  {
    String annoName = annotationType.toString();
    for( String anno: new String[] {"var", "val", "get", "set"} )
    {
      if( annoName.equals( anno ) || annoName.endsWith( "." + anno ) )
      {
        return true;
      }
    }
    return false;
  }

  default boolean handleOperatorOverloading( JCExpression tree, Type left, Type right )
  {
    // Handle operator overloading
    boolean swapped = false;
    Symbol.MethodSymbol overloadOperator = ManAttr.resolveOperatorMethod( types(), tree.getTag(), left, right );
    if( overloadOperator == null && ManAttr.isCommutative( tree.getTag() ) )
    {
      overloadOperator = ManAttr.resolveOperatorMethod( types(), tree.getTag(), right, left );
      swapped = true;
    }
    if( overloadOperator != null )
    {
      if( overloadOperator.name.toString().equals( COMPARE_TO ) )
      {
        // pose with boolean return to satisfy type checker, this call will be transformed in ext transformer
        Type.MethodType typePoseWithBooleanReturn = new Type.MethodType( overloadOperator.type.getParameterTypes(), syms().booleanType,
          overloadOperator.type.getThrownTypes(), syms().methodClass );
        overloadOperator = new OverloadOperatorSymbol( overloadOperator, typePoseWithBooleanReturn, swapped );
      }
      else
      {
        overloadOperator = new OverloadOperatorSymbol( overloadOperator, swapped );
      }
      IDynamicJdk.instance().setOperator( tree, (Symbol.OperatorSymbol)overloadOperator );
      Type owntype = overloadOperator.type.isErroneous()
                     ? overloadOperator.type
                     : swapped
                       ? types().memberType( right, overloadOperator ).getReturnType()
                       : types().memberType( left, overloadOperator ).getReturnType();
      setResult( tree, owntype );
      return true;
    }
    return false;
  }

  default boolean handleUnaryOverloading( JCTree.JCUnary tree )
  {
    // Attribute arguments
    ReflectUtil.LiveMethodRef checkNonVoid = ReflectUtil.method( chk(), "checkNonVoid", JCDiagnostic.DiagnosticPosition.class, Type.class );
    ReflectUtil.LiveMethodRef attribExpr = ReflectUtil.method( this, "attribExpr", JCTree.class, Env.class );
    Type exprType = tree.getTag().isIncOrDecUnaryOp()
      ? (Type)attribExpr.invoke( tree.arg, getEnv() )
      : (Type)checkNonVoid.invoke( tree.arg.pos(), attribExpr.invoke( tree.arg, getEnv() ) );

    // Handle operator overloading
    Symbol.MethodSymbol overloadOperator = ManAttr.resolveUnaryMethod( types(), tree.getTag(), exprType );
    if( overloadOperator != null )
    {
      overloadOperator = new OverloadOperatorSymbol( overloadOperator, false );
      Type owntype;
      if( overloadOperator.type.isErroneous() )
      {
        owntype = overloadOperator.type;
      }
      else
      {
        Type returnType = types().memberType( exprType, overloadOperator ).getReturnType();
        if( types().isAssignable( returnType, exprType ) )
        {
          owntype = returnType;
        }
        else
        {
          return false;
        }
      }
      IDynamicJdk.instance().setOperator( tree, (Symbol.OperatorSymbol)overloadOperator );
      setResult( tree, owntype );
      return true;
    }
    return false;
  }

  default boolean handleIndexedOverloading( JCTree.JCArrayAccess tree )
  {
    boolean handled = true;
    Type owntype = types().createErrorType(tree.type);
    ReflectUtil.LiveMethodRef attribExpr = ReflectUtil.method( this, "attribExpr", JCTree.class, Env.class );
    Type indexedType = (Type)attribExpr.invoke( tree.indexed, getEnv() );
    Type indexType = (Type)attribExpr.invoke( tree.index, getEnv() );
    if( types().isArray( indexedType ) )
    {
      owntype = types().elemtype( indexedType );
      if( !types().isAssignable( indexType, syms().intType ) )
      {
        IDynamicJdk.instance().logError( getLogger(), tree.pos(), "incomparable.types", indexType, syms().intType );
      }
      handled = false;
    }
    else if( !indexedType.hasTag( ERROR ) )
    {
      // Handle index operator overloading
      Symbol.MethodSymbol indexGetMethod = resolveIndexGetMethod( types(), indexedType, indexType );
      if( indexGetMethod != null )
      {
        owntype = indexGetMethod.type.isErroneous()
          ? indexGetMethod.type
          : types().memberType( indexedType, indexGetMethod ).getReturnType();
      }
      else
      {
        IDynamicJdk.instance().logError( getLogger(), tree.pos(), "array.req.but.found", indexedType );
        handled = false;
      }
    }

    setResult( tree, owntype, "VAR" );
    return handled;
  }

  default void ensureIndexedAssignmentIsWritable( JCTree.JCExpression lhs )
  {
    if( lhs instanceof JCTree.JCArrayAccess )
    {
      // Ensure there is a set() index operator method defined on a[b] in:  a[b] = c, where a[b] is not an array

      JCTree.JCArrayAccess arrayAccess = (JCTree.JCArrayAccess)lhs;
      if( !types().isArray( arrayAccess.indexed.type ) )
      {
        Symbol.MethodSymbol indexSetMethod = ManAttr.resolveIndexSetMethod( types(), arrayAccess.indexed.type, arrayAccess.index.type );
        if( indexSetMethod == null )
        {
          IDynamicJdk.instance().logError( getLogger(), arrayAccess, "array.req.but.found", arrayAccess.indexed.type );
        }
      }
    }
  }

  default void visitBindingExpression( JCTree.JCBinary tree )
  {
    Type owntype;

    if( IDynamicJdk.instance().getOperator( tree ) == null )
    {
      // replace the tree with JCBinary expressions reflecting the correct associativity and bindingOperator
      JCTree.JCBinary newTree = new JavacBinder( types() ).bind( getBindingOperands( tree, new ArrayList<>() ) );

      if( newTree == null )
      {
        getLogger().error( tree.lhs.pos,
          "proc.messager", "No reaction defined for types '" + tree.lhs.type + "' and '" + tree.rhs.type + "'" );
        return;
      }

      ReflectUtil.field( tree, "opcode" ).set( ReflectUtil.field( newTree, "opcode" ).get() );
      tree.lhs = newTree.lhs;
      tree.rhs = newTree.rhs;
      tree.type = newTree.type;
      IDynamicJdk.instance().setOperator( tree, (Symbol.OperatorSymbol)IDynamicJdk.instance().getOperator( newTree ) );
      owntype = newTree.type;
    }
    else
    {
      Symbol operator = IDynamicJdk.instance().getOperator( tree );
      owntype = operator.type.isErroneous() ? operator.type : operator.type.getReturnType();
    }

    setResult( tree, owntype );
  }

  // todo: maybe factor this out into a more efficient method from IDynamicJdk where Kind, KindSelector, and VAL can be referenced directly
  // although this is only called for bonding expressions, so is somewhat infrequent
  default void setResult( JCTree.JCExpression tree, Type owntype )
  {
    setResult( tree, owntype, "VAL" );
  }
  default void setResult( JCTree.JCExpression tree, Type owntype, String valVar )
  {
    if( isJava8() )
    {
      int VALorVAR = (int)ReflectUtil.field( "com.sun.tools.javac.code.Kinds", valVar ).getStatic();
      if( valVar.equals( "VAR" ) )
      {
        if( ((int)_pkind() & VALorVAR) == 0 )
        {
          owntype =  types().capture( owntype );
        }
      }
      ReflectUtil.field( this, "result" ).set( ReflectUtil.method( this, "check", JCTree.class, Type.class, int.class, ReflectUtil.type( Attr.class.getTypeName() + "$ResultInfo" ) )
        .invoke( tree, owntype, VALorVAR, ReflectUtil.field( this, "resultInfo" ).get() ) );
    }
    else
    {
      Class<?> kindSelectorClass = ReflectUtil.type( "com.sun.tools.javac.code.Kinds$KindSelector" );
      Object VAL = ReflectUtil.field( kindSelectorClass, "VAL" ).getStatic();
      Object VALorVAR = ReflectUtil.field( kindSelectorClass, valVar ).getStatic();
      if( valVar.equals( "VAR" ) )
      {
        if( !(boolean)ReflectUtil.method( _pkind(), "contains", kindSelectorClass ).invoke( VAL ) )
        {
          owntype = types().capture( owntype );
        }
      }
      ReflectUtil.field( this, "result" ).set( ReflectUtil.method( this, "check", JCTree.class, Type.class, kindSelectorClass, ReflectUtil.type( Attr.class.getTypeName() + "$ResultInfo" ) )
        .invoke( tree, owntype, VALorVAR, ReflectUtil.field( this, "resultInfo" ).get() ) );
    }
  }

  default ArrayList<Node<JCExpression, Tag>> getBindingOperands( JCExpression tree, ArrayList<Node<JCExpression, Tag>> operands )
  {
    if( tree instanceof JCBinary && tree.getTag() == Tag.NO_TAG )
    {
      // Binding expr

      getBindingOperands( ((JCBinary)tree).lhs, operands );
      getBindingOperands( ((JCBinary)tree).rhs, operands );
    }
    else if( tree instanceof JCBinary )
    {
      JCBinary binExpr = (JCBinary)tree;

      Tag opcode = (Tag)ReflectUtil.field( tree, "opcode" ).get();

      getBindingOperands( binExpr.lhs, operands );
      int index = operands.size();
      getBindingOperands( binExpr.rhs, operands );

      Node<JCExpression, Tag> rhsNode = operands.get( index );
      rhsNode._operatorLeft = opcode;
    }
    else
    {
      ReflectUtil.LiveMethodRef checkNonVoid = ReflectUtil.method( chk(), "checkNonVoid", JCDiagnostic.DiagnosticPosition.class, Type.class );
      ReflectUtil.LiveMethodRef attribExpr = ReflectUtil.method( this, "attribExpr", JCTree.class, Env.class );
      checkNonVoid.invoke( tree.pos(), attribExpr.invoke( tree, getEnv() ) );

      operands.add( new JavacBinder.Node<>( tree ) );
    }
    return operands;
  }

  default JCTree.JCTypeCast makeCast( JCTree.JCExpression expression, Type type )
  {
    TreeMaker make = JavacPlugin.instance().getTreeMaker();

    JCTree.JCTypeCast castCall = make.TypeCast( type, expression );

    // Transform to ManTypeCast to distinguish from non-generated casts, to avoid warnings
    ManTypeCast manTypeCast = new ManTypeCast( castCall.clazz, castCall.expr );
    ((JCTree)manTypeCast).type = type;
    manTypeCast.pos = expression.pos;
    return manTypeCast;
  }

  static Symbol.MethodSymbol resolveUnaryMethod( Types types, Tag tag, Type expr )
  {
    if( expr instanceof Type.TypeVar )
    {
      expr = types.erasure( expr );
    }

    if( !(expr.tsym instanceof Symbol.ClassSymbol) )
    {
      return null;
    }

    String op;
    switch( tag )
    {
      case NEG:
        op = UNARY_MINUS;
        break;
      case POSTINC:
      case PREINC:
        op = INC;
        break;
      case POSTDEC:
      case PREDEC:
        op = DEC;
        break;
      case NOT:
        op = NOT;
        break;
      case COMPL:
        op = COMPL;
        break;
      default:
        return null;
    }
    return getMethodSymbol( types, expr, null, op, (Symbol.ClassSymbol)expr.tsym, 0 );
  }

  static Symbol.MethodSymbol resolveIndexGetMethod( Types types, Type indexedType, Type indexType )
  {
    if( indexedType instanceof Type.TypeVar )
    {
      indexedType = types.erasure( indexedType );
    }

    if( !(indexedType.tsym instanceof Symbol.ClassSymbol) )
    {
      return null;
    }

    if( !(indexType.tsym instanceof Symbol.ClassSymbol) )
    {
      return null;
    }

    return getMethodSymbol( types, indexedType, indexType, "get", (Symbol.ClassSymbol)indexedType.tsym, 1 );
  }

  static Symbol.MethodSymbol resolveIndexSetMethod( Types types, Type indexedType, Type indexType )
  {
    if( indexedType instanceof Type.TypeVar )
    {
      indexedType = types.erasure( indexedType );
    }

    if( !(indexedType.tsym instanceof Symbol.ClassSymbol) )
    {
      return null;
    }

    if( !(indexType.tsym instanceof Symbol.ClassSymbol) )
    {
      return null;
    }

    Symbol.MethodSymbol getMethod = getMethodSymbol( types, indexedType, indexType, "get", (Symbol.ClassSymbol)indexedType.tsym, 1 );
    if( getMethod != null )
    {
      Type elemType = getMethod.type.isErroneous()
        ? getMethod.type
        : types.memberType( indexedType, getMethod ).getReturnType();

      Symbol.MethodSymbol setMethod = getMethodSymbol( types, indexedType, indexType, "set", (Symbol.ClassSymbol)indexedType.tsym, 2 );
      if( setMethod != null )
      {
        Type param2 = types.memberType( indexedType, setMethod ).getParameterTypes().get( 1 );
        if( types.isAssignable( elemType, param2 ) || isAssignableWithGenerics( types, elemType, param2 ) )
        {
          return setMethod;
        }
      }
    }
    return null;
  }

  static Symbol.MethodSymbol resolveOperatorMethod( Types types, Tag tag, Type left, Type right )
  {
    String opName = BINARY_OP_TO_NAME.get( tag );
    if( opName == null )
    {
      if( isComparableOperator( tag ) )
      {
        opName = COMPARE_TO_USING;
      }
      else
      {
        return null;
      }
    }

    if( left instanceof Type.TypeVar )
    {
      left = types.erasure( left );
    }

    if( !(left.tsym instanceof Symbol.ClassSymbol) )
    {
      return null;
    }

    int paramCount = opName.equals( COMPARE_TO_USING ) ? 2 : 1;
    Symbol.MethodSymbol methodSymbol = getMethodSymbol( types, left, right, opName, (Symbol.ClassSymbol)left.tsym, paramCount );
    if( methodSymbol == null && paramCount == 2 && !left.isPrimitive() && isRelationalOperator( tag ) )
    {
      // Support > >= < <= on any Comparable implementor
      methodSymbol = getMethodSymbol( types, left, right, COMPARE_TO, (Symbol.ClassSymbol)left.tsym, 1 );

    }
    return methodSymbol;
  }

  static Symbol.MethodSymbol getMethodSymbol( Types types, Type left, Type right, String opName, Symbol.ClassSymbol sym, int paramCount )
  {
    Symbol.MethodSymbol methodSymbol = getMethodSymbol( types, left, right, opName, sym, paramCount,
      ( t1, t2 ) -> types.isSameType( t1, t2 ) );
    if( methodSymbol != null )
    {
      return methodSymbol;
    }
    return getMethodSymbol( types, left, right, opName, sym, paramCount,
      ( t1, t2 ) -> types.isAssignable( t1, t2 ) || isAssignableWithGenerics( types, t1, t2 ) );
  }

  static Symbol.MethodSymbol getEnclosingMethodSymbol( Types types, Type left, Type right, String opName, Symbol.ClassSymbol sym, int paramCount )
  {
    Symbol.MethodSymbol methodSymbol = getEnclosingMethodSymbol( types, left, right, opName, sym, paramCount,
      ( t1, t2 ) -> types.isSameType( t1, t2 ) );
    if( methodSymbol != null )
    {
      return methodSymbol;
    }
    return getEnclosingMethodSymbol( types, left, right, opName, sym, paramCount,
      ( t1, t2 ) -> types.isAssignable( t1, t2 ) || isAssignableWithGenerics( types, t1, t2 ) );
  }

  static boolean isAssignableWithGenerics( Types types, Type t1, Type t2 )
  {
    if( t2 instanceof Type.TypeVar )
    {
      Type parameterizedParamType = types.asSuper( t1, t2.getUpperBound().tsym );
      return parameterizedParamType != null;
    }
    return false;
  }

  static Symbol.MethodSymbol getMethodSymbol( Types types, Type left, Type right, String opName, Symbol.ClassSymbol sym,
                                              int paramCount, BiPredicate<Type, Type> matcher )
  {
    if( sym == null )
    {
      return null;
    }

    for( Symbol member: IDynamicJdk.instance().getMembers( sym, e -> e instanceof Symbol.MethodSymbol ) )
    {
      Symbol.MethodSymbol m = (Symbol.MethodSymbol)member;

      if( isSynthetic( m ) )
      {
        continue;
      }

      if( m.params().size() != paramCount )
      {
        continue;
      }

      if( opName.equals( m.getSimpleName().toString() ) )
      {
        if( paramCount == 0 )
        {
          return m;
        }

        Type parameterizedMethod = types.memberType( left, m );
        while( parameterizedMethod instanceof Type.ForAll )
        {
          parameterizedMethod = parameterizedMethod.asMethodType();
        }
        if( matcher.test( right, parameterizedMethod.getParameterTypes().get( 0 ) ) )
        {
//          if( !"void".equals( m.getReturnType().tsym.name.toString() ) )
//          {
            return m;
//          }
        }
      }
    }

    Type superclass = sym.getSuperclass();
    if( superclass != null )
    {
      Symbol.MethodSymbol m = getMethodSymbol( types, left, right, opName, (Symbol.ClassSymbol)superclass.tsym, paramCount, matcher );
      if( m != null )
      {
        return m;
      }
    }

    // Look for default interface methods impls
    for( Type iface: sym.getInterfaces() )
    {
      Symbol.MethodSymbol m = getMethodSymbol( types, left, right, opName, (Symbol.ClassSymbol)iface.tsym, paramCount, matcher );
      if( m != null )
      {
        return m;
      }
    }

    return null;
  }

  static Symbol.MethodSymbol getEnclosingMethodSymbol( Types types, Type left, Type right, String opName, Symbol.ClassSymbol sym,
                                              int paramCount, BiPredicate<Type, Type> matcher )
  {
    if( sym == null )
    {
      return null;
    }

    Symbol.MethodSymbol methodSymbol = getMethodSymbol( types, left, right, opName, sym, paramCount, matcher );
    if( methodSymbol != null )
    {
      return methodSymbol;
    }

    if( sym.isStatic() )
    {
      return null;
    }

    Symbol enclosingElement = sym.getEnclosingElement();
    if( !(enclosingElement instanceof Symbol.ClassSymbol) )
    {
      return null;
    }

    return getEnclosingMethodSymbol( types, left, right, opName, (Symbol.ClassSymbol)enclosingElement, paramCount, matcher );
  }

  static boolean checkConcatenation( JCTree.JCLiteral tree, CharSequence chars, HostKind hostKind, Log logger )
  {
    if( hostKind == DOUBLE_QUOTE_LITERAL )
    {
      char prev = 0;
      for( int i = 1; i < chars.length()-1; i++ )
      {
        char c = chars.charAt( i );
        if( c == '"' && prev != '\\' )
        {
          if( logger != null )
          {
              logger.error( tree.pos().getStartPosition(), "proc.messager",
              "Manifold fragments are not supported with string concatenation. " +
                "Either make this one string literal, or consider using a multiline comment to host it instead." );
          }
          return true;
        }
        prev = c;
      }
    }
    return false;
  }

  default void checkReference( JCTree.JCMemberReference tree )
  {
    boolean isAutoReturnType =
      tree.sym instanceof Symbol.MethodSymbol && isAutoType( ((Symbol.MethodSymbol)tree.sym).getReturnType() );
    if( isAutoReturnType )
    {
      // Method references not supported with tuple/anonymous return type
      getLogger().error( tree.pos, "proc.messager", IssueMsg.MSG_ANON_RETURN_METHOD_REF_NOT_SUPPORTED.get( tree.sym.flatName() ) );
    }
  }

  static boolean isSynthetic( Symbol.MethodSymbol m )
  {
    return (m.flags() & Flags.SYNTHETIC) != 0 ||
           (m.flags() & Flags.BRIDGE) != 0;
  }

  static boolean isComparableOperator( Tag tag )
  {
    return tag == Tag.EQ ||
           tag == Tag.NE ||
           tag == Tag.LT ||
           tag == Tag.LE ||
           tag == Tag.GT ||
           tag == Tag.GE;
  }

  static boolean isRelationalOperator( Tag tag )
  {
    return tag == Tag.LT ||
           tag == Tag.LE ||
           tag == Tag.GT ||
           tag == Tag.GE;
  }

  static boolean isCommutative( Tag tag )
  {
    return tag == Tag.PLUS ||
           tag == Tag.MUL ||
           tag == Tag.BITOR ||
           tag == Tag.BITXOR ||
           tag == Tag.BITAND ||
           tag == Tag.EQ ||
           tag == Tag.NE;
  }

  class MyDiagnosticHandler extends Log.DiagnosticHandler
  {
    MyDiagnosticHandler( Log log )
    {
      install( log );
    }

    @Override
    public void report( JCDiagnostic jcDiagnostic )
    {
      prev.report( jcDiagnostic );
    }
  }

  class MyRuntimeException extends RuntimeException
  {
    @Override
    public synchronized Throwable fillInStackTrace()
    {
      // avoid cost of exception
      return this;
    }
  }

  //
  // Tuple methods
  //

  default boolean handleTupleType( JCTree.JCMethodInvocation tree )
  {
    if( !(tree.meth instanceof JCTree.JCIdent) ||
      !((JCTree.JCIdent)tree.meth).name.toString().equals( "$manifold_tuple" ) )
    {
      handleTupleAsNamedArgs( tree );
      return false;
    }

    Env<AttrContext> localEnv = getEnv().dup( tree, ReflectUtil.method( getEnv().info, "dup" ).invoke() );
//    ListBuffer<Type> argtypesBuf = new ListBuffer<>();
    List<JCExpression> argsNoLabels = removeLabels( tree.args );
//    ReflectUtil.method( this, "attribArgs", int.class, List.class, Env.class, ListBuffer.class ).
//      invoke( VAL, argsNoLabels, localEnv, argtypesBuf );
    ReflectUtil.method( this, "attribExprs", List.class, Env.class, Type.class ).
      invoke( argsNoLabels, localEnv, Type.noType );

    Map<JCTree.JCExpression, String> namesByArg = new HashMap<>();
    Map<String, String> fieldMap = makeTupleFieldMap( tree.args, namesByArg );
    // sort alphabetically
    argsNoLabels = List.from( argsNoLabels.stream().sorted( Comparator.comparing( namesByArg::get ) ).collect( Collectors.toList() ) );
    String pkg = findPackageForTuple();
    String tupleTypeName = ITupleTypeProvider.INSTANCE.get().makeType( pkg, fieldMap );
    Tree parent = JavacPlugin.instance().getTypeProcessor().getParent( tree, getEnv().toplevel );
    Symbol.ClassSymbol tupleTypeSym = findTupleClassSymbol( tupleTypeName );
    if( tupleTypeSym == null )
    {
      //todo: compile error?
      return false;
    }

    JCTree.JCNewClass newTuple = makeNewTupleClass( tupleTypeSym.type, tree, argsNoLabels );
    if( parent instanceof JCTree.JCReturn )
    {
      ((JCTree.JCReturn)parent).expr = newTuple;
    }
    else if( parent instanceof JCTree.JCParens )
    {
      ((JCTree.JCParens)parent).expr = newTuple;
    }
    ReflectUtil.field( this, "result" ).set( tupleTypeSym.type );
    tree.type = tupleTypeSym.type;
    return true;
  }

  //
  // expecting a single tuple expression arg for a method/constructor call eg.
  //   doit((x:8, y:9, z:10))  or
  //   foo.doit((x:8, y:9, z:10))  or
  //   FooType.doit((x:8, y:9, z:10))  or
  //   new Foo((x:8, y:9, z:10))
  //   etc.
  //
  //  validates the tuple call and replaces it with arguments to generated method, which matches the corresponding
  //  params-method having extra boolean params indicating whether the arg is supplied or the default expr should be used.
  //
  default void handleTupleAsNamedArgs( JCExpression tree )
  {
    List<JCExpression> treeArgs;
    if( tree instanceof JCTree.JCMethodInvocation )
    {
      treeArgs = ((JCTree.JCMethodInvocation)tree).args;
    }
    else if( tree instanceof JCTree.JCNewClass )
    {
      treeArgs = ((JCTree.JCNewClass)tree).args;
    }
    else
    {
      throw new IllegalArgumentException( "Unexpected type for tree: " + tree.getClass().getTypeName() );
    }

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
    JCTree.JCMethodInvocation tupleExpr = (JCTree.JCMethodInvocation)singleArg;

    TreeMaker make = TreeMaker.instance( JavacPlugin.instance().getContext() );

    Type receiverType = findReceiverType( tree );
    if( receiverType.hasTag( ERROR ) )
    {
      return;
    }

    Map<String, JCExpression> namedArgs = new LinkedHashMap<>();
    ArrayList<JCExpression> unnamedArgs = new ArrayList<>();
    for( int i = 0, argsSize = tupleExpr.args.size(); i < argsSize; i++ )
    {
      JCTree.JCExpression arg = tupleExpr.args.get( i );

      boolean namedArg = false;
      if( arg instanceof JCTree.JCMethodInvocation &&
        ((JCTree.JCMethodInvocation)arg).meth instanceof JCTree.JCIdent )
      {
        JCTree.JCIdent ident = (JCTree.JCIdent)((JCTree.JCMethodInvocation)arg).meth;
        if( "$manifold_label".equals( ident.name.toString() ) )
        {
          JCTree.JCIdent labelArg = (JCTree.JCIdent)((JCTree.JCMethodInvocation)arg).args.get( 0 );
          String name = labelArg.name.toString();
          if( ++i < tupleExpr.args.size() )
          {
            arg = tupleExpr.args.get( i );
            namedArgs.put( name, arg );
            namedArg = true;
          }
          else
          {
            break;
          }
        }
      }

      if( !namedArg )
      {
        if( !namedArgs.isEmpty() )
        {
          getLogger().error( arg.pos, "proc.messager", IssueMsg.MSG_OPT_PARAMS_POSITIONAL_BEFORE_NAMED.get() );
          return;
        }
        unnamedArgs.add( arg );
      }
    }

    String methodName = findMethodName( tree );
    if( methodName == null )
    {
      return;
    }
    Iterable<Symbol.MethodSymbol> paramsMethods = getParamsMethods( receiverType, methodName );
    boolean errant;
    String missingRequiredParam = null;
    nextParamsClass:
    for( Iterator<Symbol.MethodSymbol> iterator = paramsMethods.iterator(); iterator.hasNext(); )
    {
      errant = false;

      Symbol.MethodSymbol paramsMethod = iterator.next();

      Map<String, JCExpression> namedArgsCopy = new LinkedHashMap<>( namedArgs );
      ArrayList<JCExpression> unnamedArgsCopy = new ArrayList<>( unnamedArgs );

      List<String> paramsInOrder = getParamNames( paramsMethod, true );
      if( paramsInOrder.containsAll( namedArgsCopy.keySet() ) )
      {
        ArrayList<JCExpression> args = new ArrayList<>();
        boolean optional = false;
        List<Symbol.VarSymbol> params = paramsMethod.params;
        List<String> paramNames = getParamNames( paramsMethod, false );
        int targetParamsOffset = 0;
        for( int i = 0; i < params.size(); i++ )
        {
          Symbol.VarSymbol param = params.get( i );

          // .class files don't preserve param names, using encoded param names in paramsClass name,
          // in the list optional params are tagged with an "opt$" prefix
          String paramName = paramNames.get( i - targetParamsOffset );

          if( paramName.startsWith( "opt$" ) )
          {
            if( !optional )
            {
              // skip the $isXxx param
              optional = true;
              targetParamsOffset++;
              continue;
            }
            else
            {
              paramName = paramName.substring( "opt$".length() );
            }
          }

          JCExpression expr = unnamedArgsCopy.isEmpty()
                              ? namedArgsCopy.remove( paramName )
                              : unnamedArgsCopy.remove( 0 );

          if( optional )
          {
            args.add( make.Literal( expr != null ) );
            args.add( expr == null ? makeEmptyValue( param, make ) : expr );
            optional = false;
          }
          else if( expr != null )
          {
            args.add( expr );
          }
          else
          {
            // missing required arg, try next paramsClass
            missingRequiredParam = paramName;
            continue nextParamsClass;
          }
        }

        missingRequiredParam = null;

        if( !unnamedArgsCopy.isEmpty() )
        {
          // add remaining to trigger compile error
          args.addAll( unnamedArgsCopy );
          errant = true;
        }
        else if( !namedArgsCopy.isEmpty() )
        {
          // add remaining to trigger compile error
          args.addAll( namedArgsCopy.values() );
          errant = true;
        }

        if( !errant || !iterator.hasNext() )
        {
          //todo: for the errant condition, instead of settling with the the last one, store these and choose the best one
          if( tree instanceof JCTree.JCMethodInvocation )
          {
            if( ((JCTree.JCMethodInvocation)tree).meth instanceof JCTree.JCIdent )
            {
              Name name = ((JCTree.JCIdent)((JCTree.JCMethodInvocation)tree).meth).name;
              if( name != names()._this && name != names()._super )
              {
                ((JCTree.JCIdent)((JCTree.JCMethodInvocation)tree).meth).name = paramsMethod.name;
              }
            }
            else if( ((JCTree.JCMethodInvocation)tree).meth instanceof JCTree.JCFieldAccess )
            {
              ((JCTree.JCFieldAccess)((JCTree.JCMethodInvocation)tree).meth).name = paramsMethod.name;
            }
            ((JCTree.JCMethodInvocation)tree).args = List.from( args );
          }
          else
          {
            ((JCTree.JCNewClass)tree).args = List.from( args );
          }
          break;
        }
      }
      else if( !iterator.hasNext() )
      {
        putErrorOnBestMatchingMethod( tree.pos, namedArgsCopy, paramsMethods );
      }
    }
    if( missingRequiredParam != null )
    {
      getLogger().error( tree.pos, "proc.messager", IssueMsg.MSG_OPT_PARAMS_MISSING_REQ_ARG.get( missingRequiredParam ) );
    }
  }

  default void putErrorOnBestMatchingMethod( int pos, Map<String, JCExpression> namedArgsCopy, Iterable<Symbol.MethodSymbol> paramsMethods )
  {
    java.util.List<String> badNamedArgs = Collections.emptyList();
    for( Symbol.MethodSymbol paramsMethod: paramsMethods )
    {
      java.util.List<String> paramsInOrder = getParamNames( paramsMethod, true );
      java.util.List<String> candidate = namedArgsCopy.keySet().stream()
        .filter( e -> !paramsInOrder.contains( e ) )
        .collect( Collectors.toList() );
      if( badNamedArgs.isEmpty() || candidate.size() < badNamedArgs.size() )
      {
        badNamedArgs = candidate;
      }
    }
    getLogger().error( pos, "proc.messager",
      IssueMsg.MSG_OPT_PARAMS_NO_MATCHING_PARAMS_FOR_NAMED_ARGS.get( String.join( "', '", badNamedArgs ) ) );
  }


  default List<String> getParamNames( Symbol.MethodSymbol paramsMethod, boolean removeOpt$ )
  {
    //noinspection unchecked
    Class<Annotation> manifold_paramsClass = (Class<Annotation>)ReflectUtil.type( "manifold.ext.params.rt.manifold_params" );
    if( manifold_paramsClass == null )
    {
      return List.nil();
    }

    Annotation anno = paramsMethod.getAnnotation( manifold_paramsClass );
    if( anno == null )
    {
      return List.nil();
    }

    String params = (String)ReflectUtil.method( anno, "value" ).invoke();
    List<String> result = List.nil();
    StringTokenizer tokenizer = new StringTokenizer( params, "," );
    while( tokenizer.hasMoreTokens() )
    {
      String paramName = tokenizer.nextToken();
      if( removeOpt$ )
      {
        if( paramName.startsWith( "opt$" ) )
        {
          paramName = paramName.substring( "opt$".length() );
        }
      }
      result = result.append( paramName );
    }
    return result;
  }

  default Type findReceiverType( JCExpression call )
  {
    Env<AttrContext> localEnv = getEnv().dup( call, ReflectUtil.method( getEnv().info, "dup" ).invoke() );

    if( call instanceof JCTree.JCNewClass )
    {
      JCTree.JCNewClass tree = (JCTree.JCNewClass)call;
      JCExpression clazz = tree.clazz; // Class name following `new`

      // Attribute clazz expression
      return TreeInfo.isEnumInit( getEnv().tree )
        ? (Type)ReflectUtil.method( this, "attribIdentAsEnumType", Env.class, JCTree.JCIdent.class ).invoke( localEnv, clazz )
        : (Type)ReflectUtil.method( this, "attribType", JCTree.class, Env.class ).invoke( clazz, localEnv );
    }

    JCTree.JCMethodInvocation tree = (JCTree.JCMethodInvocation)call;
    JCExpression meth = tree.meth;
    if( meth instanceof JCTree.JCFieldAccess )
    {
      Type site;
      if( JreUtil.isJava8() )
      {
        JCTree.JCFieldAccess treeMeth = (JCTree.JCFieldAccess)meth;
        int skind = 0;
        if( treeMeth.name == names()._this ||
            treeMeth.name == names()._super ||
            treeMeth.name == names()._class )
        {
          skind = (int)KindSelector_TYP;
        }
        else
        {
          if( ((int)_pkind() & (int)KindSelector_PCK) != 0 ) skind = skind | (int)KindSelector_PCK;
          if( ((int)_pkind() & (int)KindSelector_TYP) != 0 ) skind = skind | (int)KindSelector_TYP | (int)KindSelector_PCK;
          if( ((int)_pkind() & ((int)KindSelector_VAL | (int)KindSelector_MTH)) != 0) skind = skind | (int)KindSelector_VAL | (int)KindSelector_TYP;
        }
  
        // Attribute the qualifier expression, and determine its symbol (if any).
        // Reflection for:  Type site = attribTree( tree.selected, env, new Attr.ResultInfo( skind, Infer.anyPoly ) );
        site = (Type)ReflectUtil.method( this, "attribTree", JCTree.class, Env.class, ReflectUtil.type( Attr.class.getTypeName() + "$ResultInfo" ) )
          .invoke( treeMeth.selected, localEnv, ReflectUtil.constructor( Attr.class.getTypeName() + "$ResultInfo", Attr.class, int.class, Type.class )
            .newInstance( this, skind, Infer.anyPoly ) );
      }
      else
      {
        Class<?> KindSelector = ReflectUtil.type( "com.sun.tools.javac.code.Kinds$KindSelector" );
        JCTree.JCFieldAccess treeMeth = (JCTree.JCFieldAccess)meth;
        Object skind = ReflectUtil.field( KindSelector, "NIL" ).getStatic();
        if( treeMeth.name == names()._this ||
            treeMeth.name == names()._super ||
            treeMeth.name == names()._class )
        {
          skind = KindSelector_TYP;
        }
        else
        {
          ReflectUtil.MethodRef of = ReflectUtil.method( KindSelector, "of", Array.newInstance( KindSelector, 0 ).getClass() );
          if( (boolean)ReflectUtil.method( _pkind(), "contains", KindSelector ).invoke( KindSelector_PCK ) )
          {
            skind = of.invokeStatic( new Object[]{Arrays.asList( skind, KindSelector_PCK ).toArray( (Object[])Array.newInstance( KindSelector, 0 ) )} );
          }
          if( (boolean)ReflectUtil.method( _pkind(), "contains", KindSelector ).invoke( KindSelector_TYP ) )
          {
            skind = of.invokeStatic( new Object[]{Arrays.asList( skind, KindSelector_TYP, KindSelector_PCK ).toArray( (Object[])Array.newInstance( KindSelector, 0 ) )} );
          }
          Object KindSelector_VAL_MTH = ReflectUtil.field( KindSelector, "VAL_MTH" ).getStatic();
          if( (boolean)ReflectUtil.method( _pkind(), "contains", KindSelector ).invoke( KindSelector_VAL_MTH ) )
          {
            skind = of.invokeStatic( new Object[]{Arrays.asList( skind, KindSelector_VAL, KindSelector_TYP ).toArray( (Object[])Array.newInstance( KindSelector, 0 ) )} );
          }
        }

        // Attribute the qualifier expression, and determine its symbol (if any).
        // Reflection for:  Type site = attribTree( tree.selected, env, new Attr.ResultInfo( skind, Type.noType ) );
        site = (Type)ReflectUtil.method( this, "attribTree", JCTree.class, Env.class, ReflectUtil.type( Attr.class.getTypeName() + "$ResultInfo" ) )
          .invoke( treeMeth.selected, localEnv, ReflectUtil.constructor( Attr.class.getTypeName() + "$ResultInfo", Attr.class, KindSelector, Type.class )
            .newInstance( this, skind, Type.noType ) );
      }
      
      return site;
    }
    else if( meth instanceof JCTree.JCIdent )
    {
      //todo: this does not handle the case where the JCIdent is resolved from static import
      return getEnclosingClass( tree ).type;
    }
    return Type.ErrorType.noType;
  }

  default String findMethodName( JCExpression call )
  {
    if( call instanceof JCTree.JCNewClass ||
      call instanceof JCTree.JCMethodInvocation && ((JCTree.JCMethodInvocation)call).meth instanceof JCTree.JCIdent &&
      (((JCTree.JCIdent)((JCTree.JCMethodInvocation)call).meth).getName().equals( names()._this ) ||
        ((JCTree.JCIdent)((JCTree.JCMethodInvocation)call).meth).getName().equals( names()._super )) )
    {
      return "constructor";
    }

    JCTree.JCMethodInvocation tree = (JCTree.JCMethodInvocation)call;
    JCExpression meth = tree.meth;
    if( meth instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess receiverExpr = (JCTree.JCFieldAccess)meth;
      return receiverExpr.name.toString();
    }
    else if( meth instanceof JCTree.JCIdent )
    {
      JCTree.JCIdent receiverExpr = (JCTree.JCIdent)meth;
      return receiverExpr.name.toString();
    }
    return null;
  }

  default JCExpression makeEmptyValue( Symbol.VarSymbol param, TreeMaker make )
  {
    if( param.type.isPrimitive() )
    {
      return make.Literal( defaultPrimitiveValue( param.type ) );
    }
    return types().isSameType( param.type, types().erasure( param.type ) )
           ? make.TypeCast( param.type, make.Literal( TypeTag.BOT, null ) )
           : make.Literal( TypeTag.BOT, null );
  }

  default List<Symbol.MethodSymbol> getParamsMethods( Type receiverType, String methodName )
  {
    return getParamsMethods( receiverType, methodName, new HashSet<>() );
  }
  default List<Symbol.MethodSymbol> getParamsMethods( Type receiverType, String methodName, Set<Type> seen )
  {
    if( seen.contains( receiverType ) || receiverType == null || receiverType.tsym == null || receiverType.hasTag( ERROR ) )
    {
      return List.nil();
    }
    seen.add( receiverType );

    Iterable paramsMethods = (Iterable)IDynamicJdk.instance().getMembers( (Symbol.ClassSymbol)receiverType.tsym,
      m -> m instanceof Symbol.MethodSymbol &&
           (methodName.equals( "constructor" ) && m.name.toString().equals( "<init>" ) || m.name.toString().equals( "$" + methodName )) &&
           getParamNames( (Symbol.MethodSymbol)m, false ).stream().anyMatch( n -> n.contains( "opt$" ) ) );
    List<Symbol.MethodSymbol> result = List.from( paramsMethods );

    Type superclass = ((Symbol.ClassSymbol)receiverType.tsym).getSuperclass();
    if( superclass != null )
    {
      result = result.appendList( getParamsMethods( superclass, methodName, seen ) );
    }
    List<Type> interfaces = ((Symbol.ClassSymbol)receiverType.tsym).getInterfaces();
    for( Type iface: interfaces )
    {
      result = result.appendList( getParamsMethods( iface, methodName, seen ) );
    }
    return result;
  }

  default Object defaultPrimitiveValue( Type type )
  {
    if( type == syms().intType ||
      type == syms().shortType )
    {
      return 0;
    }
    if( type == syms().byteType )
    {
      return (byte)0;
    }
    if( type == syms().longType )
    {
      return 0L;
    }
    if( type == syms().floatType )
    {
      return 0f;
    }
    if( type == syms().doubleType )
    {
      return 0d;
    }
    if( type == syms().booleanType )
    {
      return false;
    }
    if( type == syms().charType )
    {
      return (char)0;
    }
    if( type == syms().botType )
    {
      return null;
    }
    throw new IllegalArgumentException( "Unsupported primitive type: " + type.tsym.getSimpleName() );
  }


  default void addEnclosingClassOnTupleType( String fqn )
  {
    Set<ITypeManifold> typeManifolds = JavacPlugin.instance().getHost().getSingleModule().findTypeManifoldsFor( fqn );
    ITypeManifold tm = typeManifolds.stream().findFirst().orElse( null );
    ReflectUtil.method( tm, "addEnclosingSourceFile", String.class, URI.class )
      .invoke( fqn, getEnv().enclClass.sym.sourcefile.toUri() );
  }

  // if method overrides another method, use package of overridden method for tuples defined in override method
  default String findPackageForTuple()
  {
    JCTree.JCMethodDecl enclMethod = getEnv().enclMethod;
    if( enclMethod == null )
    {
      return getEnv().toplevel.packge.fullname.toString();
    }
    Set<Symbol.MethodSymbol> overriddenMethods = JavacTypes.instance( JavacPlugin.instance().getContext() )
      .getOverriddenMethods( enclMethod.sym );
    if( overriddenMethods.isEmpty() )
    {
      return getEnv().toplevel.packge.fullname.toString();
    }
    Symbol.MethodSymbol overridden = overriddenMethods.iterator().next();
    return overridden.owner.packge().fullname.toString();
  }

  default Symbol.ClassSymbol findTupleClassSymbol( String tupleTypeName )
  {
    addEnclosingClassOnTupleType( tupleTypeName );

    // First, try to load the class the normal way via FileManager#list()

    Context ctx = JavacPlugin.instance().getContext();
    Symbol.ClassSymbol sym = IDynamicJdk.instance().getTypeElement( ctx, getEnv().toplevel, tupleTypeName );
    if( sym != null )
    {
      return sym;
    }

    // Next, since tuples are not files and are therefore not known in advance for #list() to work, we force the
    // compiler to load it via ClassReader/Finder#includeClassFile()

    IModule compilingModule = JavacPlugin.instance().getHost().getSingleModule();
    if( compilingModule == null )
    {
      return null;
    }
    String pkg = ManClassUtil.getPackage( tupleTypeName );
    Symbol.PackageSymbol pkgSym;
    if( JreUtil.isJava8() )
    {
      pkgSym = JavacElements.instance( ctx ).getPackageElement( pkg );
    }
    else
    {
      //reflection for:  pkgSym = JavacElements.instance( ctx ).getPackageElement( getEnv().toplevel.modle, pkg );
      Object moduleSym = ReflectUtil.field( getEnv().toplevel, "modle" ).get();
      Class<?> moduleElementClass = ReflectUtil.type( "javax.lang.model.element.ModuleElement" );
      pkgSym = (Symbol.PackageSymbol)ReflectUtil
        .method( JavacElements.instance( ctx ), "getPackageElement", moduleElementClass, CharSequence.class )
        .invoke( moduleSym, pkg );
    }
    IssueReporter<JavaFileObject> issueReporter = new IssueReporter<>( () -> ctx );
    String fqn = tupleTypeName.replace( '$', '.' );
    ManifoldJavaFileManager fm = JavacPlugin.instance().getManifoldFileManager();
    JavaFileObject file = fm.findGeneratedFile( fqn, StandardLocation.CLASS_PATH, compilingModule, issueReporter );
    Object classReader = JreUtil.isJava8()
      ? ClassReader.instance( ctx )
      : ReflectUtil.method( "com.sun.tools.javac.code.ClassFinder", "instance", Context.class ).invokeStatic( ctx );
    ReflectUtil.method( classReader, "includeClassFile", Symbol.PackageSymbol.class, JavaFileObject.class )
      .invoke( pkgSym, file );
    return IDynamicJdk.instance().getTypeElement( ctx, getEnv().toplevel, tupleTypeName );
  }

  default List<JCTree.JCExpression> removeLabels( List<JCTree.JCExpression> args )
  {
    List<JCTree.JCExpression> filtered = List.nil();
    for( JCTree.JCExpression arg : args )
    {
      if( arg instanceof JCTree.JCMethodInvocation &&
        ((JCTree.JCMethodInvocation)arg).meth instanceof JCTree.JCIdent )
      {
        JCTree.JCIdent ident = (JCTree.JCIdent)((JCTree.JCMethodInvocation)arg).meth;
        if( "$manifold_label".equals( ident.name.toString() ) )
        {
          continue;
        }
      }
      filtered = filtered.append( arg );
    }
    return filtered;
  }

  default JCTree.JCNewClass makeNewTupleClass( Type tupleType, JCTree.JCExpression treePos, List<JCTree.JCExpression> args )
  {
    TreeMaker make = TreeMaker.instance( JavacPlugin.instance().getContext() );
    JCTree.JCNewClass tree = make.NewClass( null,
      null, make.QualIdent( tupleType.tsym ), args, null );
    Resolve rs = (Resolve)ReflectUtil.field( this, "rs" ).get();
    tree.constructor = (Symbol)ReflectUtil.method( rs, "resolveConstructor",
      JCDiagnostic.DiagnosticPosition.class, Env.class, Type.class, List.class, List.class ).invoke(
      treePos.pos(), getEnv(), tupleType, TreeInfo.types( args ), List.<Type>nil() );
    tree.constructorType = tree.constructor.type;
    tree.type = tupleType;
    tree.pos = treePos.pos;
    return tree;
  }

  default Map<String, String> makeTupleFieldMap( List<JCTree.JCExpression> args, Map<JCTree.JCExpression, String> argsByName )
  {
    Map<String, String> map = new LinkedHashMap<>();
    int nullNameCount = 0;
    for( int j = 0, argsSize = args.size(); j < argsSize; j++ )
    {
      JCTree.JCExpression arg = args.get( j );

      String name = null;

      if( arg instanceof JCTree.JCMethodInvocation &&
        ((JCTree.JCMethodInvocation)arg).meth instanceof JCTree.JCIdent )
      {
        JCTree.JCIdent ident = (JCTree.JCIdent)((JCTree.JCMethodInvocation)arg).meth;
        if( "$manifold_label".equals( ident.name.toString() ) )
        {
          JCTree.JCIdent labelArg = (JCTree.JCIdent)((JCTree.JCMethodInvocation)arg).args.get( 0 );
          name = labelArg.name.toString();
          if( ++j < args.size() )
          {
            arg = args.get( j );
          }
          else
          {
            break;
          }
        }
      }

      if( name == null )
      {
        if( arg instanceof JCTree.JCIdent )
        {
          name = ((JCTree.JCIdent)arg).name.toString();
        }
        else if( arg instanceof JCTree.JCFieldAccess )
        {
          name = ((JCTree.JCFieldAccess)arg).name.toString();
        }
        else if( arg instanceof JCTree.JCMethodInvocation )
        {
          JCTree.JCExpression meth = ((JCTree.JCMethodInvocation)arg).meth;
          if( meth instanceof JCTree.JCIdent )
          {
            name = getFieldNameFromMethodName( ((JCTree.JCIdent)meth).name.toString() );
          }
          else if( meth instanceof JCTree.JCFieldAccess )
          {
            name = getFieldNameFromMethodName( ((JCTree.JCFieldAccess)meth).name.toString() );
          }
        }
      }
      boolean named = name != null;
      String item = named ? name : "item";
      if( !named )
      {
        item += ++nullNameCount;
      }
      name = item;
      for( int i = 2; map.containsKey( item ); i++ )
      {
        item = name + '_' + i;
      }
      Type type =
        arg.type == syms().botType
        ? inferNullType( named, name )
        : RecursiveTypeVarEraser.eraseTypeVars( types(), arg.type );
      String typeName = type.toString();
      map.put( item, typeName );
      argsByName.put( arg, item );
    }
    return map;
  }

  default Type inferNullType( boolean named, String itemName )
  {
    if( named )
    {
      Type pt = resultInfo() == null ? null : (Type)ReflectUtil.field( resultInfo(), "pt" ).get();
      boolean isStructural = pt instanceof Type.ClassType && pt.tsym.getAnnotation( (Class)ReflectUtil.type( "manifold.ext.rt.api.Structural" ) ) != null;
      if( isStructural )
      {
        for( Symbol m : IDynamicJdk.instance().getMembers( (Symbol.ClassSymbol)pt.tsym,
          m -> m.getKind() == ElementKind.METHOD
            && m.name.toString().equals( "get" + ManStringUtil.capitalize( itemName ) )
            && ((Symbol.MethodSymbol)m).params().isEmpty()
            && ((Symbol.MethodSymbol)m).getReturnType() != Symtab.instance( JavacPlugin.instance().getContext() ).voidType ) )
        {
          return ((Symbol.MethodSymbol)m).getReturnType();
        }
      }
    }
    return IDynamicJdk.instance().getTypeElement( JavacPlugin.instance().getContext(), getEnv().toplevel, Null.class.getTypeName() ).type;
  }

  /**
   * Changes method name to a field name like this:
   * getAddress -> address
   * callHome -> home
   * findJDKVersion -> jdkVersion
   * id -> id
   */
  default String getFieldNameFromMethodName( String methodName )
  {
    for( int i = 0; i < methodName.length(); i++ )
    {
      if( Character.isUpperCase( methodName.charAt( i ) ) )
      {
        StringBuilder name = new StringBuilder( methodName.substring( i ) );
        for( int j = 0; j < name.length(); j++ )
        {
          char c = name.charAt( j );
          if( Character.isUpperCase( c ) &&
            (j == 0 || j == name.length() - 1 || Character.isUpperCase( name.charAt( j+1 ) )) )
          {
            name.setCharAt( j, Character.toLowerCase( c ) );
          }
          else
          {
            break;
          }
        }
        return name.toString();
      }
    }
    return methodName;
  }
}
