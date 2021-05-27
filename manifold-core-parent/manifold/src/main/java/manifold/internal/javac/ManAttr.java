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

import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.*;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import manifold.internal.javac.AbstractBinder.Node;
import manifold.util.ReflectUtil;

import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.TypeTag.ERROR;
import static manifold.util.JreUtil.isJava8;

public interface ManAttr
{
  boolean JAILBREAK_PRIVATE_FROM_SUPERS = true;

  String COMPARE_TO = "compareTo";
  String COMPARE_TO_USING = "compareToUsing";
  String UNARY_MINUS = "unaryMinus";
  String INC = "inc";
  String DEC = "dec";
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

  default Check chk()
  {
    return (Check)ReflectUtil.field( this, "chk" ).get();
  }

  default Resolve rs()
  {
    return (Resolve)ReflectUtil.field( this, "rs" ).get();
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

  default void patchMethodType( JCTree.JCMethodInvocation tree )
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
      IDynamicJdk.instance().setOperator( tree, (Symbol.OperatorSymbol)overloadOperator );
      Type owntype = overloadOperator.type.isErroneous()
                     ? overloadOperator.type
                     : types().memberType( exprType, overloadOperator ).getReturnType();
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
}
