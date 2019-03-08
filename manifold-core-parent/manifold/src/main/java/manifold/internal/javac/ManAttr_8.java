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
import com.sun.tools.javac.code.Symbol;
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
import manifold.util.ReflectUtil;
import manifold.util.Stack;

public class ManAttr_8 extends Attr implements ManAttr
{
  private final ManLog_8 _manLog;
  private Stack<JCTree.JCFieldAccess> _selects;
  private Stack<JCTree.JCAnnotatedType> _annotatedTypes;

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
}