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

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.Check;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.Tag;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import manifold.internal.javac.AbstractBinder.Node;
import manifold.util.ReflectUtil;


import static manifold.util.JreUtil.isJava8;

public interface ManAttr
{
  boolean JAILBREAK_PRIVATE_FROM_SUPERS = true;

  Map<Tag, String> BINARY_OP_TO_NAME = new HashMap<Tag, String>()
  {{
    put( Tag.PLUS, "add" );
    put( Tag.MINUS, "subtract" );
    put( Tag.MUL, "multiply" );
    put( Tag.DIV, "divide" );
    put( Tag.MOD, "remainder" );
    // note ==, !=, >, >=, <, <=  are covered via IComparableWith
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

  default boolean handleOperatorOverloading( JCTree.JCBinary tree )
  {
    // Attribute arguments
    ReflectUtil.LiveMethodRef checkNonVoid = ReflectUtil.method( chk(), "checkNonVoid", JCDiagnostic.DiagnosticPosition.class, Type.class );
    ReflectUtil.LiveMethodRef attribExpr = ReflectUtil.method( this, "attribExpr", JCTree.class, Env.class );
    Type left = (Type)checkNonVoid.invoke( tree.lhs.pos(), attribExpr.invoke( tree.lhs, getEnv() ) );
    Type right = (Type)checkNonVoid.invoke( tree.lhs.pos(), attribExpr.invoke( tree.rhs, getEnv() ) );

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
      overloadOperator = new OverloadOperatorSymbol( overloadOperator, swapped );
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

  default boolean handleNegationOverloading( JCTree.JCUnary tree )
  {
    if( tree.getTag() != Tag.NEG )
    {
      return false;
    }

    // Attribute arguments
    ReflectUtil.LiveMethodRef checkNonVoid = ReflectUtil.method( chk(), "checkNonVoid", JCDiagnostic.DiagnosticPosition.class, Type.class );
    ReflectUtil.LiveMethodRef attribExpr = ReflectUtil.method( this, "attribExpr", JCTree.class, Env.class );
    Type expr = (Type)checkNonVoid.invoke( tree.arg.pos(), attribExpr.invoke( tree.arg, getEnv() ) );

    // Handle operator overloading
    Symbol.MethodSymbol overloadOperator = ManAttr.resolveNegationMethod( types(), tree.getTag(), expr );
    if( overloadOperator != null )
    {
      overloadOperator = new OverloadOperatorSymbol( overloadOperator, false );
      IDynamicJdk.instance().setOperator( tree, (Symbol.OperatorSymbol)overloadOperator );
      Type owntype = overloadOperator.type.isErroneous()
                     ? overloadOperator.type
                     : types().memberType( expr, overloadOperator ).getReturnType();
      setResult( tree, owntype );
      return true;
    }
    return false;
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
    if( isJava8() )
    {
      Object VAL = ReflectUtil.field( "com.sun.tools.javac.code.Kinds", "VAL" ).getStatic();
      ReflectUtil.field( this, "result" ).set( ReflectUtil.method( this, "check", JCTree.class, Type.class, int.class, ReflectUtil.type( Attr.class.getTypeName() + "$ResultInfo" ) )
        .invoke( tree, owntype, VAL, ReflectUtil.field( this, "resultInfo" ).get() ) );
    }
    else
    {
      Class<?> kindSelectorClass = ReflectUtil.type( "com.sun.tools.javac.code.Kinds$KindSelector" );
      Object VAL = ReflectUtil.field( kindSelectorClass, "VAL" ).getStatic();
      ReflectUtil.field( this, "result" ).set( ReflectUtil.method( this, "check", JCTree.class, Type.class, kindSelectorClass, ReflectUtil.type( Attr.class.getTypeName() + "$ResultInfo" ) )
        .invoke( tree, owntype, VAL, ReflectUtil.field( this, "resultInfo" ).get() ) );
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

  static Symbol.MethodSymbol resolveNegationMethod( Types types, Tag tag, Type expr )
  {
    if( expr instanceof Type.TypeVar )
    {
      expr = types.erasure( expr );
    }

    if( !(expr.tsym instanceof Symbol.ClassSymbol) )
    {
      return null;
    }

    return getMethodSymbol( types, expr, null, "negate", (Symbol.ClassSymbol)expr.tsym, 0 );
  }

  static Symbol.MethodSymbol resolveOperatorMethod( Types types, Tag tag, Type left, Type right )
  {
    String opName = BINARY_OP_TO_NAME.get( tag );
    if( opName == null )
    {
      if( isComparableOperator( tag ) )
      {
        opName = "compareToWith";
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

    int paramCount = opName.equals( "compareToWith" ) ? 2 : 1;
    return getMethodSymbol( types, left, right, opName, (Symbol.ClassSymbol)left.tsym, paramCount );
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

  static Symbol.MethodSymbol getMethodSymbol( Types types, Type left, Type right, String opName, Symbol.ClassSymbol sym, int paramCount, BiPredicate<Type, Type> matcher )
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
          return m;
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
