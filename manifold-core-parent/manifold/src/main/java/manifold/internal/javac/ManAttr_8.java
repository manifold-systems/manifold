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

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.OperatorSymbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotations;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.DeferredAttr;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Lower;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.comp.TransTypes;
import com.sun.tools.javac.jvm.ByteCodes;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Warner;
import manifold.api.type.FragmentValue;
import manifold.api.type.ISelfCompiledFile;
import manifold.rt.api.util.Stack;
import manifold.util.ReflectUtil;


import static com.sun.tools.javac.code.Kinds.MTH;
import static com.sun.tools.javac.code.TypeTag.CLASS;
import static manifold.internal.javac.HostKind.DOUBLE_QUOTE_LITERAL;
import static manifold.internal.javac.HostKind.TEXT_BLOCK_LITERAL;

public class ManAttr_8 extends Attr implements ManAttr
{
  private final ManLog_8 _manLog;
  private final Symtab _syms;
  private Stack<JCTree.JCFieldAccess> _selects;
  private Stack<JCTree.JCAnnotatedType> _annotatedTypes;
  private Stack<JCTree.JCMethodDecl> _methodDefs;

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
    _annotatedTypes = new Stack<>();
    _methodDefs = new Stack<>();
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
   * Facilitates @Jailbreak. ManResolve#isAccessible() needs to know the JCFieldAccess in context.
   */
  @Override
  public void visitSelect( JCTree.JCFieldAccess tree )
  {
    // record JCFieldAccess trees as they are visited so we can access them elsewhere while in context
    _selects.push( tree );
    try
    {
      super.visitSelect( tree );
    }
    finally
    {
      _selects.pop();
    }
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

  public JCTree.JCAnnotatedType peekAnnotatedType()
  {
    return _annotatedTypes.isEmpty() ? null : _annotatedTypes.peek();
  }

  /**
   * Handles @Jailbreak
   */
  @Override
  public void visitApply( JCTree.JCMethodInvocation tree )
  {
    if( !(tree.meth instanceof JCTree.JCFieldAccess) )
    {
      super.visitApply( tree );
      patchMethodType( tree );
      return;
    }

    if( JAILBREAK_PRIVATE_FROM_SUPERS )
    {
      _manLog.pushSuspendIssues( tree ); // since method-calls can be nested, we need a tree of stacks TreeNode(JCTree.JCFieldAccess, Stack<JCDiagnostic>>)
    }

    JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess)tree.meth;
    try
    {
      super.visitApply( tree );
      patchMethodType( tree );

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
      if( JAILBREAK_PRIVATE_FROM_SUPERS )
      {
        _manLog.popSuspendIssues( tree );
      }
    }
  }

  @Override
  public void visitBinary( JCTree.JCBinary tree )
  {
    if( !JavacPlugin.instance().isExtensionsEnabled() )
    {
      super.visitBinary( tree );
      return;
    }

    if( tree.getTag() == JCTree.Tag.APPLY ) // binding expr
    {
      // Handle binding expressions

      visitBindingExpression( tree );
      ReflectUtil.field( tree, "opcode" ).set( JCTree.Tag.MUL ); // pose as a MUL expr to pass binary expr checks
      return;
    }

    ReflectUtil.LiveMethodRef checkNonVoid = ReflectUtil.method( chk(), "checkNonVoid", DiagnosticPosition.class, Type.class );
    ReflectUtil.LiveMethodRef attribExpr = ReflectUtil.method( this, "attribExpr", JCTree.class, Env.class );
    Type left = (Type)checkNonVoid.invoke( tree.lhs.pos(), attribExpr.invoke( tree.lhs, getEnv() ) );
    Type right = (Type)checkNonVoid.invoke( tree.lhs.pos(), attribExpr.invoke( tree.rhs, getEnv() ) );

    if( handleOperatorOverloading( tree, left, right ) )
    {
      // Handle operator overloading
      return;
    }

    // Everything after left/right operand attribution (see super.visitBinary())
    _visitBinary_Rest( tree, left, right );
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

    if( handleNegationOverloading( tree ) )
    {
      // Handle negation overloading
      return;
    }

    super.visitUnary( tree );
  }

  /**
   * Overrides to handle fragments in String literals
   */
  public void visitLiteral( JCTree.JCLiteral tree )
  {
    if( tree.typetag == CLASS && tree.value.toString().startsWith( "[>" ) )
    {
      Type type = getFragmentValueType( tree );
      tree.type = type;
      ReflectUtil.field( this, "result" ).set( type );
    }
    else
    {
      super.visitLiteral( tree );
    }
  }

  private Type getFragmentValueType( JCTree.JCLiteral tree )
  {
    try
    {
      CharSequence source = ManParserFactory.getSource( getEnv().toplevel.sourcefile );
      CharSequence chars = source.subSequence( tree.pos().getStartPosition(),
        tree.pos().getEndPosition( getEnv().toplevel.endPositions ) );
      FragmentProcessor.Fragment fragment = FragmentProcessor.instance().parseFragment(
        tree.pos().getStartPosition(), chars.toString(),
        chars.length() > 3 && chars.charAt( 1 ) == '"'
        ? TEXT_BLOCK_LITERAL
        : DOUBLE_QUOTE_LITERAL );
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
    }
    catch( Exception e )
    {
      getLogger().rawWarning( tree.pos().getStartPosition(),
        "Error parsing Manifold fragment.\n" +
        e.getClass().getSimpleName() + ": " + e.getMessage() + "\n" +
        (e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "") );
    }
    return _syms.stringType.constType( tree.value );
  }

  private Type getFragmentValueType( Attribute.Compound attribute )
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