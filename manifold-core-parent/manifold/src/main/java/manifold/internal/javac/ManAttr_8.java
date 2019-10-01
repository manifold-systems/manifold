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
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeAnnotations;
import com.sun.tools.javac.comp.Annotate;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.DeferredAttr;
import com.sun.tools.javac.comp.Lower;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.comp.TransTypes;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import java.io.IOException;
import manifold.api.type.FragmentValue;
import manifold.api.util.Stack;
import manifold.util.ReflectUtil;


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

    if( handleOperatorOverloading( tree ) )
    {
      // Handle operator overloading
      return;
    }

    super.visitBinary( tree );
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
      CharSequence source = getEnv().toplevel.sourcefile.getCharContent( true );
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
    catch( IOException e )
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
}