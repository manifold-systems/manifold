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
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.*;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import manifold.api.util.IssueMsg;
import manifold.internal.javac.AbstractBinder.Node;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;

import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.TypeTag.ERROR;
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

  boolean JAILBREAK_PRIVATE_FROM_SUPERS = true;

  String COMPARE_TO = "compareTo";
  String COMPARE_TO_USING = "compareToUsing";
  String UNARY_MINUS = "unaryMinus";
  String INC = "inc";
  String DEC = "dec";
  String NEGATE = "negate";
  Map<Tag, String> BINARY_OP_TO_NAME = new HashMap<Tag, String>()
  {{
    put( Tag.PLUS, "plus" );
    put( Tag.MINUS, "minus" );
    put( Tag.MUL, "times" );
    put( Tag.DIV, "div" );
    put( Tag.MOD, "rem" );
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
        tree.sym instanceof Symbol.VarSymbol )
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
        if( types().isAssignable( exprType, returnType ) )
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
    if( tree instanceof JCBinary && tree.getTag() == Tag.APPLY )
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
    manTypeCast.type = type;
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
        op = NEGATE;
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
}
