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
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Symbol.OperatorSymbol;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.jvm.ByteCodes;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Warner;
import manifold.api.type.ISelfCompiledFile;
import manifold.api.util.IssueMsg;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.Stack;
import manifold.util.ReflectUtil;

import java.util.HashSet;
import java.util.Set;

import static com.sun.tools.javac.code.Kinds.MTH;
import static com.sun.tools.javac.code.Kinds.VAL;
import static com.sun.tools.javac.code.TypeTag.DEFERRED;

public class ManAttr_8 extends Attr implements ManAttr
{
  private final ManLog_8 _manLog;
  private final Symtab _syms;
  private final Stack<JCTree.JCFieldAccess> _selects;
  private final Stack<JCTree.JCMethodInvocation> _applys;
  private final Stack<JCTree.JCAnnotatedType> _annotatedTypes;
  private final Stack<JCTree.JCMethodDecl> _methodDefs;
  private final Stack<JCTree.JCBinary> _binaryExprs;
  private final Set<JCTree.JCMethodInvocation> _visitedAutoMethodCalls = new HashSet<>();

  public static ManAttr_8 instance( Context ctx )
  {
    Attr attr = ctx.get( attrKey );
    if( !(attr instanceof ManAttr_8) )
    {
      ctx.put( attrKey, (Attr)null );
      attr = new ManAttr_8( ctx );
    }

    return (ManAttr_8)attr;
  }

  private ManAttr_8( Context ctx )
  {
    super( ctx );
    _selects = new Stack<>();
    _applys = new Stack<>();
    _annotatedTypes = new Stack<>();
    _methodDefs = new Stack<>();
    _binaryExprs = new Stack<>();
    _syms = Symtab.instance( ctx );

    // Override logger to handle final field assignment for @Jailbreak
    _manLog = (ManLog_8)ManLog_8.instance( ctx );
    ReflectUtil.field( this, "log" ).set( _manLog );
    ReflectUtil.field( this, "rs" ).set( ManResolve.instance( ctx ) );
    reassignAllEarlyHolders( ctx );
  }

  private void reassignAllEarlyHolders( Context ctx )
  {
    Object[] earlyAttrHolders = {
      Resolve.instance( ctx ),
      DeferredAttr.instance( ctx ),
      MemberEnter.instance( ctx ),
      Lower.instance( ctx ),
      TransTypes.instance( ctx ),
      Annotate.instance( ctx ),
      TypeAnnotations.instance( ctx ),
      JavacTrees.instance( ctx ),
      JavaCompiler.instance( ctx ),
      LambdaToMethod.instance( ctx ),
    };
    for( Object instance: earlyAttrHolders )
    {
      ReflectUtil.LiveFieldRef attr = ReflectUtil.WithNull.field( instance, "attr" );
      if( attr != null )
      {
        attr.set( this );
      }
    }
  }

  /**
   * Handle properties in interfaces, which are non-static unless explicitly static.
   * This is necessary so that a non-static property can reference type variables in its type:  @var T element;
   */
  @Override
  public Type attribType(JCTree tree, Env<AttrContext> env)
  {
    ManAttr.super.handleNonStaticInterfaceProperty( env );
    return super.attribType( tree, env );
  }

  /**
   * Facilitates @Jailbreak. ManResolve#isAccessible() needs to know the JCFieldAccess in context.
   */
  @Override
  public void visitSelect( JCTree.JCFieldAccess tree )
  {
    // record JCFieldAccess trees as they are visited so we can access them elsewhere while in context
    _selects.push( tree );
    try
    {
      JCTree.JCMethodInvocation parent = peekApply();
      DeferredAttrDiagHandler deferredAttrDiagHandler = null;
      if( parent == null || parent.meth != tree )
      {
        deferredAttrDiagHandler = suppressDiagnositics( tree );
      }
      try
      {
        super.visitSelect( tree );
      }
      finally
      {
        if( deferredAttrDiagHandler != null )
        {
          restoreDiagnostics( tree, deferredAttrDiagHandler );
        }
      }
      patchAutoFieldType( tree );
    }
    finally
    {
      _selects.pop();
    }
  }

  /**
   * Handle the LetExpr, which is normally used after the attribution phase. We use it during parse to transform
   * AssignOp to normal Assign so that other manifold features are easier to implement (operator overloading,
   * properties, etc.)
   */
  @Override
  public void visitLetExpr( JCTree.LetExpr tree )
  {
    Env env = getEnv();
    Env localEnv = env.dup( tree, ReflectUtil.method( env.info, "dup" ).invoke() );
    for( JCTree.JCVariableDecl def: tree.defs )
    {
      attribStat( def, localEnv );
      def.type = def.init.type;
      def.vartype.type = def.type;
      def.sym.type = def.type;
    }
    ReflectUtil.field( this, "result" ).set( attribExpr( tree.expr, localEnv ) );
    tree.type = tree.expr.type;
  }

  private boolean shouldCheckSuperType( Type type )
  {
    return _shouldCheckSuperType( type, true );
  }

  private boolean _shouldCheckSuperType( Type type, boolean checkSuper )
  {
    return
      type instanceof Type.ClassType &&
      type != Type.noType &&
      !(type instanceof Type.ErrorType) &&
      !type.toString().equals( Object.class.getTypeName() ) &&
      (!checkSuper || _shouldCheckSuperType( ((Symbol.ClassSymbol)type.tsym).getSuperclass(), false ));
  }

  public void visitMethodDef( JCTree.JCMethodDecl tree )
  {
    JCTree returnType = tree.getReturnType();
    if( isAutoTypeAssigned( returnType ) )
    {
      // already attributed method returning 'auto'
      return;
    }

    _methodDefs.push( tree );
    try
    {
      super.visitMethodDef( tree );
      handleIntersectionAutoReturnType( tree );
    }
    finally
    {
      _methodDefs.pop();
    }
  }

  /**
   * Since intersection types are not supported in bytecode (method signatures) we make an attempt at
   * selecting the most relevant type in the intersection as the method's return type.
   *
   * todo: consider an alternative that utilizes intersection types indirectly
   * For instance:
   * - add a runtime annotation to preserve the intersection return type in the method's bytecode
   * - in the ClassReader reassign the method's return type accordingly
   */
  private void handleIntersectionAutoReturnType( JCTree.JCMethodDecl tree )
  {
    if( tree.restype != null && tree.restype.type.isCompound() )
    {
      Type retType = tree.restype.type;
      retType = (Type)ReflectUtil.field( retType, "supertype_field" ).get();
      //noinspection unchecked
      List<Type> interfaces = (List<Type>)ReflectUtil.field( tree.restype.type, "interfaces_field" ).get();

      if( !interfaces.isEmpty() && types().isSameType( syms().objectType, retType ) )
      {
        // Since an interface implicitly has Object's members, it is more relevant than Object.
        // Choose the one with the most members as a simple way to find the "best" one.

        int maxMemberCount = -1;
        for( Type t : interfaces )
        {
          int[] memberCount = {0};
          IDynamicJdk.instance().getMembers( (Symbol.ClassSymbol)t.tsym ).forEach( m -> memberCount[0]++ );
          if( maxMemberCount < memberCount[0] )
          {
            maxMemberCount = memberCount[0];
            retType = t;
          }
        }
      }
      assignMethodReturnType( retType, tree );
    }
  }

  private boolean isAutoTypeAssigned( JCTree returnType )
  {
    return returnType != null &&
      (returnType.toString().equals( ManClassUtil.getShortClassName( AUTO_TYPE ) ) || returnType.toString().equals( AUTO_TYPE )) &&
      !AUTO_TYPE.equals( returnType.type.tsym.flatName().toString() );
  }

  @Override
  public void visitVarDef( JCTree.JCVariableDecl tree )
  {
    super.visitVarDef( tree );

    inferAutoLocalVar( tree );
  }

  @Override
  public void visitForeachLoop( JCTree.JCEnhancedForLoop tree) {
    Env<AttrContext> env = getEnv();
    Env<AttrContext> loopEnv =
      env.dup(env.tree, (AttrContext)ReflectUtil.method( env.info, "dup", Scope.class ).invoke( ReflectUtil.method( ReflectUtil.field( env.info, "scope" ).get(), "dup" ).invoke() ) );
    try {
      //the Formal Parameter of a for-each loop is not in the scope when
      //attributing the for-each expression; we mimick this by attributing
      //the for-each expression first (against original scope).
      Type exprType = types().cvarUpperBound(attribExpr(tree.expr, loopEnv));
      attribStat(tree.var, loopEnv);
      ReflectUtil.method( chk(), "checkNonVoid", DiagnosticPosition.class, Type.class ).invoke(tree.pos(), exprType);
      Type elemtype = types().elemtype(exprType); // perhaps expr is an array?
      if (elemtype == null) {
        // or perhaps expr implements Iterable<T>?
        Type base = types().asSuper(exprType, syms().iterableType.tsym);
        if (base == null) {
          getLogger().error(tree.expr.pos(),
            "foreach.not.applicable.to.type",
            exprType,
            ReflectUtil.method( ReflectUtil.field( this, "diags" ).get(), "fragment", String.class ).invoke( "type.req.array.or.iterable" ) );
          elemtype = types().createErrorType(exprType);
        } else {
          List<Type> iterableParams = base.allparams();
          elemtype = iterableParams.isEmpty()
            ? syms().objectType
            : types().wildUpperBound(iterableParams.head);
        }
      }
      if( AUTO_TYPE.equals( tree.var.type.tsym.getQualifiedName().toString() ) )
      {
        tree.var.type = elemtype;
        tree.var.sym.type = elemtype;
      }
      ReflectUtil.method( chk(), "checkType", DiagnosticPosition.class, Type.class, Type.class )
        .invoke( tree.expr.pos(), elemtype, tree.var.sym.type );
      loopEnv.tree = tree; // before, we were not in loop!
      attribStat(tree.body, loopEnv);
      ReflectUtil.field( this, "result" ).set( null );
    }
    finally {
      ReflectUtil.method( ReflectUtil.field( loopEnv.info, "scope" ).get(), "leave" ).invoke();
    }
  }

  private void inferAutoLocalVar( JCTree.JCVariableDecl tree )
  {
    if( !isAutoType( tree.type ) )
    {
      // not 'auto' variable
      return;
    }

//    if( ((Scope)ReflectUtil.field( getEnv().info, "scope" ).get()).owner.kind != MTH )
//    {
//      // not a local var
//      return;
//    }

    JCTree.JCExpression initializer = tree.getInitializer();
    if( initializer == null )
    {
      // no initializer, no type inference

      Tree parent = JavacPlugin.instance().getTypeProcessor().getParent( tree, getEnv().toplevel );
      if( !(parent instanceof JCTree.JCEnhancedForLoop) )
      {
        IDynamicJdk.instance().logError( Log.instance( JavacPlugin.instance().getContext() ), tree.getType().pos(),
          "proc.messager", IssueMsg.MSG_AUTO_CANNOT_INFER_WO_INIT.get() );
      }
      return;
    }

    if( initializer.type == syms().botType )
    {
      IDynamicJdk.instance().logError( Log.instance( JavacPlugin.instance().getContext() ), tree.getType().pos(),
        "proc.messager", IssueMsg.MSG_AUTO_CANNOT_INFER_FROM_NULL.get() );
      return;
    }

    tree.type = initializer.type;
    tree.sym.type = initializer.type;
  }

  @Override
  public void visitReturn( JCTree.JCReturn tree )
  {
    boolean isAutoMethod = isAutoMethod();
    if( isAutoMethod )
    {
      Object resultInfo = ReflectUtil.field( getEnv().info, "returnResult" ).get();
      if( resultInfo != null )
      {
        ReflectUtil.field( resultInfo, "pt" ).set( Type.noType );
      }
    }

    super.visitReturn( tree );

    if( isAutoMethod )
    {
      reassignAutoMethodReturnTypeToInferredType( tree );
    }
  }

  private boolean isAutoMethod()
  {
    JCTree.JCMethodDecl enclMethod = getEnv().enclMethod;
    return enclMethod != null && enclMethod.getReturnType() != null &&
      "auto".equals( enclMethod.getReturnType().toString() );
  }

  private void reassignAutoMethodReturnTypeToInferredType( JCTree.JCReturn tree )
  {
    if( tree.expr == null )
    {
      return;
    }

    if( _methodDefs.isEmpty() )
    {
      return;
    }

    Type returnExprType = tree.expr.type;
    if( returnExprType.isErroneous() )
    {
      return;
    }

    if( !isAutoType( returnExprType ) )
    {
      JCTree.JCMethodDecl meth = _methodDefs.peek();

      // remove the constant type e.g., if derived from a constant return value such as "Foo"
      returnExprType = returnExprType.baseType();

      if( returnExprType.hasTag( DEFERRED ) )
      {
        // a DEFERRED type can't be compared using lub()
        return;
      }

      // compute LUB of the previous method return type assignment and this return expr type
      returnExprType = isAutoType( meth.restype.type )
        ? returnExprType
        : lub( tree, meth.restype.type, returnExprType ).baseType();

      // now assign the computed type to the method's return type
      assignMethodReturnType( returnExprType, meth );
    }
    else
    {
      JCTree.JCExpression returnExpr = tree.expr;
      while( returnExpr instanceof JCTree.JCParens )
      {
        returnExpr = ((JCTree.JCParens)returnExpr).expr;
      }
      if( !(returnExpr instanceof JCTree.JCMethodInvocation) )
      {
        IDynamicJdk.instance().logError( Log.instance( JavacPlugin.instance().getContext() ), tree.expr.pos(),
          "proc.messager", IssueMsg.MSG_AUTO_RETURN_MORE_SPECIFIC_TYPE.get() );
      }
    }
  }

  private void assignMethodReturnType( Type returnExprType, JCTree.JCMethodDecl meth )
  {
    ((Type.MethodType)meth.sym.type).restype = returnExprType;
    if( meth.restype instanceof JCTree.JCIdent )
    {
      ((JCTree.JCIdent)meth.restype).sym = returnExprType.tsym;
    }
    else if( meth.restype instanceof JCTree.JCFieldAccess )
    {
      ((JCTree.JCFieldAccess)meth.restype).sym = returnExprType.tsym;
    }
    meth.restype.type = returnExprType;

    // cause the msym's erasure field to reset with the new return type
    meth.sym.erasure_field = null;
    types().memberType( getEnv().enclClass.sym.type, meth.sym );
  }

  private Type lub( JCTree.JCReturn tree, Type type, Type returnExprType )
  {
    return (Type)ReflectUtil.method( this, "condType", DiagnosticPosition.class, Type.class, Type.class )
      .invoke( tree, type, returnExprType );
  }

  public JCTree.JCMethodDecl peekMethodDef()
  {
    return _methodDefs.isEmpty() ? null : _methodDefs.peek();
  }

  /**
   * Facilitates @Jailbreak. ManResolve#isAccessible() needs to know the JCAnnotatedType in context.
   */
  @Override
  public void visitAnnotatedType( JCTree.JCAnnotatedType tree )
  {
    _annotatedTypes.push( tree );
    try
    {
      super.visitAnnotatedType( tree );
    }
    finally
    {
      _annotatedTypes.pop();
    }
  }

  public JCTree.JCFieldAccess peekSelect()
  {
    return _selects.isEmpty() ? null : _selects.peek();
  }

  public JCTree.JCMethodInvocation peekApply()
  {
    return _applys.isEmpty() ? null : _applys.peek();
  }

  public JCTree.JCAnnotatedType peekAnnotatedType()
  {
    return _annotatedTypes.isEmpty() ? null : _annotatedTypes.peek();
  }

  @Override
  public void visitReference( JCTree.JCMemberReference tree )
  {
    super.visitReference( tree );
  }
  
  @Override
  public void visitIdent( JCTree.JCIdent tree )
  {
    super.visitIdent( tree );
    patchAutoFieldType( tree );
  }

  @Override
  public void visitNewClass( JCTree.JCNewClass tree )
  {
    handleTupleAsNamedArgs( tree );
    super.visitNewClass( tree );
  }

  /**
   * Handles @Jailbreak, unit expressions, 'auto'
   */
  @Override
  public void visitApply( JCTree.JCMethodInvocation tree )
  {
    if( !(tree.meth instanceof JCTree.JCFieldAccess) )
    {
      if( !handleTupleType( tree ) )
      {
        super.visitApply( tree );
        patchMethodType( tree, _visitedAutoMethodCalls );
      }
      return;
    }
    else
    {
      handleTupleAsNamedArgs( tree );
    }

    if( JAILBREAK_PRIVATE_FROM_SUPERS )
    {
      _manLog.pushSuspendIssues( tree ); // since method-calls can be nested, we need a tree of stacks TreeNode(JCTree.JCFieldAccess, Stack<JCDiagnostic>>)
    }

    _applys.push( tree );
    JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess)tree.meth;
    try
    {
      super.visitApply( tree );
      patchMethodType( tree, _visitedAutoMethodCalls );

      if( JAILBREAK_PRIVATE_FROM_SUPERS )
      {
        if( fieldAccess.type instanceof Type.ErrorType )
        {
          if( shouldCheckSuperType( fieldAccess.selected.type ) && _manLog.isJailbreakSelect( fieldAccess ) )
          {
            // set qualifier type to supertype to handle private methods
            Type.ClassType oldType = (Type.ClassType)fieldAccess.selected.type;
            fieldAccess.selected.type = ((Symbol.ClassSymbol)oldType.tsym).getSuperclass();
            ((JCTree.JCIdent)fieldAccess.selected).sym.type = fieldAccess.selected.type;
            fieldAccess.type = null;
            fieldAccess.sym = null;
            tree.type = null;

            // retry with supertype
            visitApply( tree );

            // restore original type
            fieldAccess.selected.type = oldType;
            ((JCTree.JCIdent)fieldAccess.selected).sym.type = fieldAccess.selected.type;
          }
        }
        else
        {
          // apply any issues logged for the found method (only the top of the suspend stack)
          _manLog.recordRecentSuspendedIssuesAndRemoveOthers( tree );
        }
      }
    }
    finally
    {
      _applys.pop();
      if( JAILBREAK_PRIVATE_FROM_SUPERS )
      {
        _manLog.popSuspendIssues( tree );
      }
    }
  }

  public Type attribExpr( JCTree tree, Env<AttrContext> env, Type pt )
  {
    if( isAutoType( pt ) )
    {
      // don't let 'auto' type influence the expression's type
      pt = Type.noType;
    }
    return super.attribExpr( tree, env, pt );
  }

  @Override
  public void visitIndexed( JCTree.JCArrayAccess tree )
  {
    if( !JavacPlugin.instance().isExtensionsEnabled() )
    {
      super.visitIndexed( tree );
      return;
    }

    ManAttr.super.handleIndexedOverloading( tree );
  }

  @Override
  public void visitAssign( JCTree.JCAssign tree )
  {
    Class<?> ResultInfo_Class = ReflectUtil.type( Attr.class.getTypeName() + "$ResultInfo" );
    Type owntype = (Type)ReflectUtil.method( this, "attribTree", JCTree.class, Env.class, ResultInfo_Class )
      .invoke( tree.lhs, getEnv().dup( tree ), ReflectUtil.field( this, "varInfo" ).get() );

    if( tree.lhs.type != null && tree.lhs.type.isPrimitive() )
    {
      // always cast rhs for the case where the original statement was a compound assign involving a primitive type
      // (manifold transforms a += b to a = a + b, so that we can simply use plus() to handle both addition and compound
      // assign addition, however:
      //   short a = 0;
      //   a += (byte)b;
      // blows up if we don't cast the rhs of the resulting
      // transformation:  a += (byte)b;  parse==>  a = a + (byte)b;  attr==>  a = (short) (a + (byte)b);
      tree.rhs = makeCast( tree.rhs, tree.lhs.type );
    }

    Type capturedType = types().capture( owntype );
    attribExpr( tree.rhs, getEnv(), owntype );
    setResult( tree, capturedType );
    ReflectUtil.field( this, "result" ).set( ReflectUtil.method( this, "check", JCTree.class, Type.class, int.class, ResultInfo_Class )
      .invoke( tree, capturedType, VAL, ReflectUtil.field( this, "resultInfo" ).get() ) );

    ensureIndexedAssignmentIsWritable( tree.lhs );
  }

  @Override
  public void visitAssignop( JCTree.JCAssignOp tree )
  {
    super.visitAssignop( tree );

    ensureIndexedAssignmentIsWritable( tree.lhs );
  }

  @Override
  public void visitBinary( JCTree.JCBinary tree )
  {
    if( !JavacPlugin.instance().isExtensionsEnabled() )
    {
      super.visitBinary( tree );
      return;
    }

    if( tree.getTag() == JCTree.Tag.NO_TAG ) // binding expr
    {
      // Handle binding expressions

      visitBindingExpression( tree );
      ReflectUtil.field( tree, "opcode" ).set( JCTree.Tag.MUL ); // pose as a MUL expr to pass binary expr checks
      return;
    }

    Type left;
    Type right;
    pushBinary( tree );
    try
    {
      ReflectUtil.LiveMethodRef checkNonVoid = ReflectUtil.method( chk(), "checkNonVoid", DiagnosticPosition.class, Type.class );
      ReflectUtil.LiveMethodRef attribExpr = ReflectUtil.method( this, "attribExpr", JCTree.class, Env.class );
      left = (Type)checkNonVoid.invoke( tree.lhs.pos(), attribExpr.invoke( tree.lhs, getEnv() ) );
      right = (Type)checkNonVoid.invoke( tree.rhs.pos(), attribExpr.invoke( tree.rhs, getEnv() ) );
    }
    finally
    {
      popBinary( tree );
    }

    if( handleOperatorOverloading( tree, left, right ) )
    {
      // Handle operator overloading
      return;
    }

    // Everything after left/right operand attribution (see super.visitBinary())
    _visitBinary_Rest( tree, left, right );
  }

  private void pushBinary( JCTree.JCBinary tree )
  {
    _binaryExprs.push( tree );
  }
  private JCTree.JCBinary popBinary( JCTree.JCBinary tree )
  {
    JCTree.JCBinary expr = _binaryExprs.pop();
    if( expr != tree )
    {
      throw new IllegalStateException();
    }
    return expr;
  }

  private void _visitBinary_Rest( JCTree.JCBinary tree, Type left, Type right )
  {
    // Find operator.
    Symbol operator = tree.operator =
      (Symbol)ReflectUtil.method( rs(), "resolveBinaryOperator",
        DiagnosticPosition.class, JCTree.Tag.class, Env.class, Type.class, Type.class )
        .invoke( tree.pos(), tree.getTag(), getEnv(), left, right );

    Type owntype = types().createErrorType( tree.type );
    if( operator.kind == MTH &&
        !left.isErroneous() &&
        !right.isErroneous() )
    {
      owntype = operator.type.getReturnType();
      // This will figure out when unboxing can happen and
      // choose the right comparison operator.
      int opc = (int)ReflectUtil.method( chk(), "checkOperator",
        DiagnosticPosition.class, OperatorSymbol.class, JCTree.Tag.class, Type.class, Type.class )
        .invoke( tree.lhs.pos(), operator, tree.getTag(), left, right );

      // If both arguments are constants, fold them.
      if( left.constValue() != null && right.constValue() != null )
      {
        Type ctype = (Type)ReflectUtil.method( cfolder(), "fold2", int.class, Type.class, Type.class ).invoke( opc, left, right );
        if( ctype != null )
        {
          owntype = (Type)ReflectUtil.method( cfolder(), "coerce", Type.class, Type.class ).invoke( ctype, owntype );
        }
      }

      // Check that argument types of a reference ==, != are
      // castable to each other, (JLS 15.21).  Note: unboxing
      // comparisons will not have an acmp* opc at this point.
      if( (opc == ByteCodes.if_acmpeq || opc == ByteCodes.if_acmpne) )
      {
        if( !types().isEqualityComparable( left, right,
          new Warner( tree.pos() ) ) )
        {
          getLogger().error( tree.pos(), "incomparable.types", left, right );
        }
      }

      ReflectUtil.method( chk(), "checkDivZero", DiagnosticPosition.class, Symbol.class, Type.class )
        .invoke( tree.rhs.pos(), operator, right );
    }
    setResult( tree, owntype );
  }

  @Override
  public void visitUnary( JCTree.JCUnary tree )
  {
    if( !JavacPlugin.instance().isExtensionsEnabled() )
    {
      super.visitUnary( tree );
      return;
    }

    if( handleUnaryOverloading( tree ) )
    {
      return;
    }

    super.visitUnary( tree );
  }

  /**
   * Overrides to handle fragments in String literals
   */
  public void visitLiteral( JCTree.JCLiteral tree )
  {
    if( !handleFragmentStringLiteral( tree ) )
    {
      super.visitLiteral(tree);
    }
  }

  @Override
  public void attribClass( DiagnosticPosition pos, Symbol.ClassSymbol c )
  {
    if( c.sourcefile instanceof ISelfCompiledFile )
    {
      ISelfCompiledFile sourcefile = (ISelfCompiledFile)c.sourcefile;
      String fqn = c.getQualifiedName().toString();
      if( sourcefile.isSelfCompile( fqn ) )
      {
        // signal the self-compiled class to fully parse and report errors
        // (note its source in javac is just a stub)
        sourcefile.parse( fqn );
      }
    }

    super.attribClass( pos, c );
  }
}