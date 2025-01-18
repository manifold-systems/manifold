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

package manifold.ext;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.AttrContextEnv;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.Resolve;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.*;
import manifold.ExtIssueMsg;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.gen.SrcParameter;
import manifold.api.host.IManifoldHost;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;
import manifold.ext.rt.ExtensionMethod;
import manifold.ext.rt.ReflectionRuntimeMethods;
import manifold.ext.rt.RuntimeMethods;
import manifold.ext.rt.api.*;
import manifold.internal.javac.*;
import manifold.rt.api.*;
import manifold.rt.api.util.Pair;
import manifold.rt.api.util.TypesUtil;
import manifold.util.JreUtil;
import manifold.util.ReflectUtil;
import manifold.util.concurrent.ConcurrentHashSet;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.TypeTag.*;
import static manifold.internal.javac.HostKind.DOUBLE_QUOTE_LITERAL;
import static manifold.internal.javac.HostKind.TEXT_BLOCK_LITERAL;

/**
 *
 */
public class ExtensionTransformer extends TreeTranslator
{
  private final ExtensionManifold _sp;
  private final TypeProcessor _tp;
  private boolean _bridgeMethod;
  private boolean _lambdaMethod;
  private JCTree.JCVariableDecl _parameter;

  public ExtensionTransformer( ExtensionManifold sp, TypeProcessor typeProcessor )
  {
    _sp = sp;
    _tp = typeProcessor;
  }

  @SuppressWarnings( "WeakerAccess" )
  public TypeProcessor getTypeProcessor()
  {
    return _tp;
  }


  @Override
  public void visitIndexed( JCTree.JCArrayAccess tree )
  {
    super.visitIndexed( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( _tp.getTypes().isArray( tree.indexed.type ) )
    {
      return;
    }

    // replace with indexedExpr.get(indexExpr)

    Symbol.MethodSymbol operatorMethod = ManAttr.resolveIndexGetMethod( _tp.getTypes(),
      tree.indexed.type, tree.index.type );

    if( operatorMethod != null )
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

      TreeMaker make = _tp.getTreeMaker();

      JCTree.JCMethodInvocation methodCall;
      JCExpression receiver = tree.indexed;
      JCExpression arg = tree.index;
      arg = boxUnboxIfNeeded( _tp.getTypes(), _tp.getTreeMaker(),
        Names.instance( _tp.getContext() ), arg, operatorMethod.params().get( 0 ).type );
      methodCall = make.Apply( List.nil(), IDynamicJdk.instance().Select( make, receiver, operatorMethod ), List.of( arg ) );
      methodCall = configMethod( tree, operatorMethod, methodCall );

      result = methodCall;
    }
  }

  private int tempVarIndex = 0;

  public void visitBinary( JCTree.JCBinary tree )
  {
    super.visitBinary( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    IDynamicJdk dynJdk = IDynamicJdk.instance();
    Symbol op = dynJdk.getOperator( tree );
    if( op instanceof OverloadOperatorSymbol )
    {
      TreeMaker make = _tp.getTreeMaker();
      Symtab symbols = _tp.getSymtab();

      // Handle operator overload expressions

      Symbol.MethodSymbol operatorMethod = (Symbol.MethodSymbol)op;
      boolean swap = false;
      while( operatorMethod instanceof OverloadOperatorSymbol )
      {
        swap = ((OverloadOperatorSymbol)operatorMethod).isSwapped();
        operatorMethod = ((OverloadOperatorSymbol)operatorMethod).getMethod();
      }

      if( operatorMethod != null )
      {
        operatorMethod = favorStringsWithNumberCoercion( tree, operatorMethod );
        JCExpression expr = null;

        JCTree.JCMethodInvocation methodCall;
        JCExpression receiver = swap ? tree.rhs : tree.lhs;
        JCExpression arg = swap ? tree.lhs : tree.rhs;
        arg = boxUnboxIfNeeded( _tp.getTypes(), _tp.getTreeMaker(),
          Names.instance( _tp.getContext() ), arg, operatorMethod.params().get( 0 ).type );
        if( ManAttr.isComparableOperator( tree.getTag() ) )
        {
          Context ctx = JavacPlugin.instance().getContext();
          if( tree.getTag() == JCTree.Tag.EQ || tree.getTag() == JCTree.Tag.NE )
          {
            // Equality requires null check before calling compareTo/Using():
            //
            // a == b
            // ? true/false
            // : a == null || b == null
            //   ? false/true
            //   : a.compareToUsing( b, op );

            tempVarIndex++;

            Symbol owner = getEnclosingSymbol( tree, ctx );

            List<JCTree.JCVariableDecl> tempVars = List.nil();
            JCTree[] receiverTemp = tempify( tree, make, receiver, ctx, owner, "$receiverExprTemp" + tempVarIndex );
            if( receiverTemp != null )
            {
              tempVars = tempVars.append( (JCTree.JCVariableDecl)receiverTemp[0] );
              receiver = (JCExpression)receiverTemp[1];
            }
            JCTree[] argTemp = tempify( tree, make, arg, ctx, owner, "$argExprTemp" + tempVarIndex );
            if( argTemp != null )
            {
              tempVars = tempVars.append( (JCTree.JCVariableDecl)argTemp[0] );
              arg = (JCExpression)argTemp[1];
            }

            JCTree.JCBinary cond = make.Binary( JCTree.Tag.EQ, receiver, arg );
            cond.type = symbols.booleanType;
            dynJdk.setOperatorSymbol( ctx, cond, JCTree.Tag.EQ, "==", symbols.objectType.tsym );

            JCTree.JCLiteral nullLiteral = make.Literal( BOT, null );
            nullLiteral.type = symbols.objectType;

            JCTree.JCBinary eqNull1 = make.Binary( JCTree.Tag.EQ, receiver, nullLiteral );
            eqNull1.type = symbols.booleanType;
            dynJdk.setOperatorSymbol( ctx, eqNull1, JCTree.Tag.EQ, "==", symbols.objectType.tsym );
            JCTree.JCBinary eqNull2 = make.Binary( JCTree.Tag.EQ, arg, nullLiteral );
            eqNull2.type = symbols.booleanType;
            dynJdk.setOperatorSymbol( ctx, eqNull2, JCTree.Tag.EQ, "==", symbols.objectType.tsym );
            JCTree.JCBinary elsePart = make.Binary( JCTree.Tag.OR, eqNull1, eqNull2 );
            elsePart.type = symbols.booleanType;
            dynJdk.setOperatorSymbol( ctx, elsePart, JCTree.Tag.OR, "||", symbols.booleanType.tsym );

            methodCall = make.Apply( List.nil(),
              IDynamicJdk.instance().Select( make, receiver, operatorMethod ),
              List.from( new JCExpression[]{arg, getRelationalOpEnumConst( make, tree )} ) );
            methodCall = configMethod( tree, operatorMethod, methodCall );

            JCTree.JCConditional elsePart2 = make.Conditional( elsePart, make.Literal( tree.getTag() == JCTree.Tag.NE ), methodCall );
            elsePart2.type = symbols.booleanType;

            expr = make.Conditional( cond, make.Literal( tree.getTag() == JCTree.Tag.EQ ), elsePart2 );
            expr.type = symbols.booleanType;
            expr.pos = tree.pos;

            if( !tempVars.isEmpty() )
            {
              expr = ILetExpr.makeLetExpr( make, tempVars, expr, expr.type, expr.pos );
            }
          }
          else if( operatorMethod.name.toString().equals( "compareTo" ) )
          {
            // (x > y) generated as: (x.compareTo(y) > 0)

            methodCall = make.Apply( List.nil(), IDynamicJdk.instance().Select( make, receiver, operatorMethod ), List.of( arg ) );
            methodCall.type = symbols.intType;
            methodCall.pos = tree.pos;

            JCTree.JCLiteral zeroLiteral = make.Literal( INT, 0 );
            zeroLiteral.type = symbols.intType;

            JCTree.JCBinary compareToCond = make.Binary( tree.getTag(), methodCall, zeroLiteral );
            compareToCond.type = symbols.booleanType;
            dynJdk.setOperatorSymbol( ctx, compareToCond, tree.getTag(), relOpString( tree.getTag() ), symbols.intType.tsym );
            compareToCond.pos = tree.pos;

            expr = compareToCond;
          }
          else
          {
            // (x > y) generated as: (x.compareToUsing(y, GT))

            methodCall = make.Apply( List.nil(),
              IDynamicJdk.instance().Select( make, receiver, operatorMethod ),
              List.from( new JCExpression[]{arg, getRelationalOpEnumConst( make, tree )} ) );

            methodCall = configMethod( tree, operatorMethod, methodCall );
          }
        }
        else
        {
          methodCall = make.Apply( List.nil(), IDynamicJdk.instance().Select( make, receiver, operatorMethod ), List.of( arg ) );

          methodCall = configMethod( tree, operatorMethod, methodCall );
        }

        result = expr == null ? methodCall : expr;
      }
    }
  }

  // Create a temporary variable and corresonding identifier to avoid cop
  private JCTree[] tempify( JCTree.JCExpression tree, TreeMaker make, JCExpression expr, Context ctx, Symbol owner, String varName )
  {
    return tempify( false, tree, make, expr, ctx, owner, varName, tempVarIndex );
  }
  private JCTree[] tempify( boolean force, JCTree.JCExpression tree, TreeMaker make, JCExpression expr, Context ctx, Symbol owner, String varName )
  {
    return tempify( force, tree, make, expr, ctx, owner, varName, tempVarIndex );
  }
  public static JCTree[] tempify( boolean force, JCTree.JCExpression tree, TreeMaker make, JCExpression expr, Context ctx, Symbol owner, String varName, int tempVarIndex )
  {
    switch( expr.getTag() )
    {
      case LITERAL:
      case IDENT:
        if( !force )
        {
          return null;
        }
        // fall through...

      default:
        JCTree.JCVariableDecl tempVar = make.VarDef( make.Modifiers( FINAL | SYNTHETIC ),
          Names.instance( ctx ).fromString( varName + tempVarIndex ), make.Type( expr.type ), expr );
        tempVar.sym = new Symbol.VarSymbol( FINAL | SYNTHETIC, tempVar.name, expr.type, owner );
        tempVar.type = tempVar.sym.type;
        tempVar.pos = tree.pos;
        JCExpression ident = make.Ident( tempVar );
        ident.type = expr.type;
        ident.pos = tree.pos;
        return new JCTree[]{tempVar, ident};
    }
  }

  private JCTree.JCMethodInvocation configMethod( JCTree.JCExpression tree, Symbol.MethodSymbol operatorMethod, JCTree.JCMethodInvocation methodCall )
  {
    methodCall.setPos( tree.pos );
    methodCall.type = operatorMethod.getReturnType();

    // If methodCall is an extension method, rewrite it
    methodCall = maybeReplaceWithExtensionMethod( methodCall );

    // If methodCall is a structural call, rewrite it e.g., from an arithmetic expression using an operator overload call
    methodCall = maybeReplaceWithStructuralCall( methodCall );

    // Concrete type set in attr
    methodCall.type = tree.type;
    return methodCall;
  }

  private String relOpString( JCTree.Tag tag )
  {
    switch( tag )
    {
      case LT:
        return "<";
      case LE:
        return "<=";
      case GT:
        return ">";
      case GE:
        return ">=";
    }
    throw new IllegalStateException( "Expecting only relational op, but found: " + tag );
  }

  /**
   * If the binding expression is of the form `A b` where `A` is a Float or Double *literal* and `b` defines a
   * `R postfixBind(String)` where `R` is the same return type as the original binding expression, then get the
   * token that was parsed for the Float or Double literal and use the String version of `postfixBind()`. This has
   * the effect of preserving the value of the token, where otherwise it can be lost due to IEEE floating point
   * encoding.
   */
  private Symbol.MethodSymbol favorStringsWithNumberCoercion( JCTree.JCBinary tree, Symbol.MethodSymbol operatorMethod )
  {
    String operatorMethodName = operatorMethod.name.toString();
    if( !operatorMethodName.equals( "postfixBind" ) )
    {
      return operatorMethod;
    }

    if( tree.lhs instanceof JCTree.JCLiteral &&
      (tree.lhs.getKind() == Tree.Kind.FLOAT_LITERAL || tree.lhs.getKind() == Tree.Kind.DOUBLE_LITERAL) )
    {
      Type rhsType = tree.rhs.type;
      Symbol.MethodSymbol postfixBinding_string = ManAttr.getMethodSymbol(
        _tp.getTypes(), rhsType, _tp.getSymtab().stringType, operatorMethodName, (Symbol.ClassSymbol)rhsType.tsym, 1 );

      if( postfixBinding_string != null &&
        postfixBinding_string.getParameters().get( 0 ).type.tsym == _tp.getSymtab().stringType.tsym &&
        postfixBinding_string.getReturnType().equals( operatorMethod.getReturnType() ) )
      {
        // since the source may be preprocessed we attempt to get it in its preprocessed form
        CharSequence source = ParserFactoryFiles.getSource( _tp.getCompilationUnit().getSourceFile() );
        int start = tree.lhs.pos;
        int end = tree.lhs.pos().getEndPosition( ((JCTree.JCCompilationUnit)_tp.getCompilationUnit()).endPositions );
        String token = source.subSequence( start, end ).toString();
        if( token.endsWith( "d" ) || token.endsWith( "f" ) )
        {
          token = token.substring( 0, token.length() - 1 );
        }
        JCTree.JCLiteral temp = (JCTree.JCLiteral)tree.lhs;
        tree.lhs = _tp.getTreeMaker().Literal( token );
        tree.lhs.type = _tp.getSymtab().stringType;
        tree.lhs.pos = temp.pos;

        return postfixBinding_string;
      }
    }
    return operatorMethod;
  }

  private JCExpression getRelationalOpEnumConst( TreeMaker make, JCTree tree )
  {
    Symbol.ClassSymbol opClassSym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(),
      ComparableUsing.Operator.class.getCanonicalName() );

    Names names = Names.instance( _tp.getContext() );
    Symbol.VarSymbol operatorSym = resolveField(
      tree.pos(), _tp.getContext(), names.fromString( tree.getTag().name() ), opClassSym.type );

    JCTree.JCFieldAccess opEnumConst = (JCTree.JCFieldAccess)memberAccess( make, _tp.getElementUtil(),
      ComparableUsing.Operator.class.getName() + "." + tree.getTag().name() );
    opEnumConst.type = operatorSym.type;
    opEnumConst.sym = operatorSym;
    opEnumConst.pos = tree.pos;
    assignTypes( opEnumConst.selected, opClassSym );
    opEnumConst.selected.pos = tree.pos;

    return opEnumConst;
  }

  /**
   * Expand a boxing or unboxing conversion if needed.
   */
  <T extends JCTree> T boxUnboxIfNeeded( Types types, TreeMaker make, Names names, T tree, Type type )
  {
    boolean havePrimitive = tree.type.isPrimitive();
    if( havePrimitive == type.isPrimitive() )
    {
      return tree;
    }
    if( havePrimitive )
    {
      Type unboxedTarget = types.unboxedType( type );
      if( !unboxedTarget.hasTag( NONE ) )
      {
        if( !types.isSubtype( tree.type, unboxedTarget ) ) //e.g. Character c = 89;
        {
          tree.type = unboxedTarget.constType( tree.type.constValue() );
        }
        return (T)boxPrimitive( types, make, names, (JCExpression)tree, type );
      }
      else
      {
        tree = (T)boxPrimitive( types, make, names, (JCExpression)tree );
      }
    }
    else
    {
      tree = (T)unbox( types, make, names, _tp.getContext(), _tp.getCompilationUnit(), (JCExpression)tree, type );
    }
    return tree;
  }

  /**
   * Box up a single primitive expression.
   */
  private JCExpression boxPrimitive( Types types, TreeMaker make, Names names, JCExpression tree )
  {
    return boxPrimitive( types, make, names, tree, types.boxedClass( tree.type ).type );
  }

  /**
   * Box up a single primitive expression.
   */
  private JCExpression boxPrimitive( Types types, TreeMaker make, Names names, JCExpression tree, Type box )
  {
    make.at( tree.pos() );
    Symbol valueOfSym = resolveMethod( tree.pos(),
      names.valueOf,
      box,
      List.<Type>nil()
        .prepend( tree.type ) );
    return make.App( make.QualIdent( valueOfSym ), List.of( tree ) );
  }

  /**
   * Unbox an object to a primitive value.
   */
  public static JCExpression unbox( Types types, TreeMaker make, Names names, Context context, CompilationUnitTree compUnit,
                                    JCExpression tree, Type primitive )
  {
    Type unboxedType = types.unboxedType( tree.type );
    if( unboxedType.hasTag( NONE ) )
    {
      unboxedType = primitive;
      if( !unboxedType.isPrimitive() )
      {
        throw new AssertionError( unboxedType );
      }
      make.at( tree.pos() );
      tree = make.TypeCast( types.boxedClass( unboxedType ).type, tree );
    }
    else
    {
      // There must be a conversion from unboxedType to primitive.
      if( !types.isSubtype( unboxedType, primitive ) )
      {
        throw new AssertionError( tree );
      }
    }
    make.at( tree.pos() );
    Symbol valueSym = resolveMethod( tree.pos(), context, (JCTree.JCCompilationUnit)compUnit,
      unboxedType.tsym.name.append( names.Value ), // x.intValue()
      tree.type,
      List.<Type>nil() );
    return make.App( IDynamicJdk.instance().Select( make, tree, valueSym ) );
  }

//## todo: handle case where casting to a generic structural interface:  (StructuralInterface<Foo>)thing
//  @Override
//  public void visitTypeApply( JCTree.JCTypeApply tree )
//  {
//    if( _tp.isGenerate() && !shouldProcessForGeneration() )
//    {
//      super.visitTypeApply( tree );
//      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
//      return;
//    }
//
//    boolean isStructural = TypeUtil.isStructuralInterface( _tp, tree.clazz.type.tsym );
//
//    super.visitTypeApply( tree );
//
//    if( isStructural )
//    {
//      Symbol.ClassSymbol objectSym = getObjectClass();
//      JCTree.JCIdent objIdent = _tp.getTreeMaker().Ident( objectSym );
//      objIdent.type = objectSym.type;
//      objIdent.pos = tree.pos;
//
//      result = objIdent;
//    }
//  }

  /**
   * Erase all structural interface type literals to Object
   */
  @Override
  public void visitIdent( JCTree.JCIdent tree )
  {
    super.visitIdent( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() && !(shouldProcessForLambdaGeneration() && _parameter != null) )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( TypesUtil.isStructuralInterface( _tp.getTypes(), tree.sym ) && !isReceiver( tree ) )
    {
      Symbol.ClassSymbol objectSym = getObjectClass();
      Tree parent = _parameter != null ? _parameter : _tp.getParent( tree );
      JCTree.JCIdent objIdent = _tp.getTreeMaker().Ident( objectSym );
      if( parent instanceof JCTree.JCVariableDecl )
      {
        ((JCTree.JCVariableDecl)parent).type = objectSym.type;

        long parameterModifier = 8589934592L; // Flag.Flag.PARAMETER.value
        if( (((JCTree.JCVariableDecl)parent).mods.flags & parameterModifier) != 0 )
        {
          objIdent.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).sym.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).vartype = objIdent;
        }
      }
      else if( parent instanceof JCTree.JCWildcard )
      {
        JCTree.JCWildcard wildcard = (JCTree.JCWildcard)parent;
        wildcard.type = new Type.WildcardType( objectSym.type, wildcard.kind.kind, wildcard.type.tsym );
      }
      tree = objIdent;
      tree.type = objectSym.type;
    }
    result = tree;
  }

  @Override
  public void visitLambda( JCTree.JCLambda tree )
  {
    super.visitLambda( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    tree.type = eraseStructureType( tree.type );
    ArrayList<Type> types = new ArrayList<>();
    for( Type target : IDynamicJdk.instance().getTargets( tree ) )
    {
      types.add( eraseStructureType( target ) );
    }
    IDynamicJdk.instance().setTargets( tree, List.from( types ) );
  }

  /**
   * Erase all structural interface type literals to Object
   */
  @Override
  public void visitSelect( JCTree.JCFieldAccess tree )
  {
    super.visitSelect( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( TypesUtil.isStructuralInterface( _tp.getTypes(), tree.sym ) && !isReceiver( tree ) )
    {
      Symbol.ClassSymbol objectSym = getObjectClass();
      JCTree.JCIdent objIdent = _tp.getTreeMaker().Ident( objectSym );
      Tree parent = _tp.getParent( tree );
      if( parent instanceof JCTree.JCVariableDecl )
      {
        ((JCTree.JCVariableDecl)parent).type = objectSym.type;
        long parameterModifier = 8589934592L; // Flag.Flag.PARAMETER.value
        if( (((JCTree.JCVariableDecl)parent).mods.flags & parameterModifier) != 0 )
        {
          objIdent.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).sym.type = objectSym.type;
          ((JCTree.JCVariableDecl)parent).vartype = objIdent;
        }
      }
      else if( parent instanceof JCTree.JCWildcard )
      {
        JCTree.JCWildcard wildcard = (JCTree.JCWildcard)parent;
        wildcard.type = new Type.WildcardType( objectSym.type, wildcard.kind.kind, wildcard.type.tsym );
      }
      result = objIdent;
    }
    else if( isJailbreakReceiver( tree ) )
    {
      result = replaceWithReflection( tree );
    }
    else
    {
      result = tree;
    }
  }

  @Override
  public void visitAssign( JCTree.JCAssign tree )
  {
    super.visitAssign( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( handleIndexedAssignment( tree ) )
    {
      return;
    }

    if( isJailbreakReceiver( tree ) )
    {
      result = replaceWithReflection( tree );
    }
    else
    {
      result = tree;
    }
  }

  private boolean handleIndexedAssignment( JCTree.JCAssign tree )
  {
    if( !(tree.lhs instanceof JCTree.JCArrayAccess) )
    {
      return false;
    }

    JCTree.JCArrayAccess lhs = (JCTree.JCArrayAccess)tree.lhs;
    if( _tp.getTypes().isArray( lhs.indexed.type ) )
    {
      return false;
    }

    // replace  foo[bar] = baz  with  foo.set(bar, baz)

    Symbol.MethodSymbol operatorMethod = ManAttr.resolveIndexSetMethod( _tp.getTypes(),
      lhs.indexed.type, lhs.index.type );

    Context ctx = JavacPlugin.instance().getContext();
    if( operatorMethod != null )
    {
      TreeMaker make = _tp.getTreeMaker();

      tempVarIndex++;

      List<JCTree.JCVariableDecl> tempVars = List.nil();
      JCExpression rhs = tree.rhs;
      JCTree[] rhsTemp = tempify( tree, make, rhs, ctx, getEnclosingSymbol( tree, ctx ), "setRhsTempVar" + tempVarIndex );
      if( rhsTemp != null )
      {
        tempVars = tempVars.append( (JCTree.JCVariableDecl)rhsTemp[0] );
        rhs = (JCExpression)rhsTemp[1];
      }

      JCTree.JCMethodInvocation setCall;
      JCExpression receiver = lhs.indexed;
      JCExpression arg = lhs.index;
      Type parameterizedMethod = _tp.getTypes().memberType( lhs.indexed.type, operatorMethod );
      while( parameterizedMethod instanceof Type.ForAll )
      {
        parameterizedMethod = parameterizedMethod.asMethodType();
      }

      arg = boxUnboxIfNeeded( _tp.getTypes(), _tp.getTreeMaker(),
        Names.instance( _tp.getContext() ), arg, parameterizedMethod.getParameterTypes().get( 0 ) );
      setCall = make.Apply( List.nil(), IDynamicJdk.instance().Select( make, receiver, operatorMethod ), List.of( arg, rhs ) );
      setCall = configMethod( lhs, operatorMethod, setCall );

      JCTree[] setCallTemp = tempify( true, tree, make, setCall, ctx, getEnclosingSymbol( tree, ctx ), "$setCallTempVar" + tempVarIndex );
      tempVars = tempVars.append( (JCTree.JCVariableDecl)setCallTemp[0] );

      // Need let expr so that we can return the RHS value as required by java assignment op.
      // Note, the set() method can return whatever it wants, it is ignored here,
      // this allows us to support eg. List.set(int, T) where this method returns the previous value
      JCTree.LetExpr letExpr = ILetExpr.makeLetExpr( make, tempVars, rhs, lhs.type, tree.pos );

      result = letExpr;
      return true;
    }

    return false;
  }

  @Override
  public void visitAssignop( JCTree.JCAssignOp tree )
  {
    super.visitAssignop( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( isJailbreakReceiver( tree ) )
    {
      // +=, -=, etc. operators not supported with jailbreak, only direct assignment
      _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_COMPOUND_OP_NOT_ALLOWED_REFLECTION.get() );
      Types types = Types.instance( ((BasicJavacTask)_tp.getJavacTask()).getContext() );
      tree.type = types.createErrorType( tree.type );

      result = tree;
    }
    else
    {
      result = tree;
    }
  }

  @Override
  public void visitUnary( JCTree.JCUnary tree )
  {
    super.visitUnary( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    Symbol op = IDynamicJdk.instance().getOperator( tree );
    boolean isOverload = op instanceof OverloadOperatorSymbol;
    TreeMaker make = _tp.getTreeMaker();

    if( !(op instanceof Symbol.MethodSymbol) )
    {
      return;
    }

    // Handle operator overload expressions
    Symbol.MethodSymbol operatorMethod = (Symbol.MethodSymbol)op;
    while( operatorMethod instanceof OverloadOperatorSymbol )
    {
      operatorMethod = ((OverloadOperatorSymbol)operatorMethod).getMethod();
    }

    JCTree.Tag tag = tree.getTag();
    if( isOverload && operatorMethod != null &&
        (JCTree.Tag.NEG == tag ||
         JCTree.Tag.NOT == tag ||
         JCTree.Tag.COMPL == tag) )
    {
      genUnary( tree, make, operatorMethod );
      return;
    }

    if( (isOverload && operatorMethod != null) ||
        (tree.arg instanceof JCTree.JCArrayAccess &&
          !_tp.getTypes().isArray( ((JCTree.JCArrayAccess)tree.arg).indexed.type )) )
    {
      switch( tree.getTag() )
      {
        case PREINC:
        case PREDEC:
        case POSTINC:
        case POSTDEC:
          genUnaryIncDec( tree, make, isOverload ? operatorMethod : null );
          break;
        default:
          throw new IllegalStateException();
      }
      return;
    }

    if( isJailbreakReceiver( tree ) )
    {
      Tree.Kind kind = tree.getKind();
      if( kind == Tree.Kind.POSTFIX_INCREMENT || kind == Tree.Kind.POSTFIX_DECREMENT ||
        kind == Tree.Kind.PREFIX_INCREMENT || kind == Tree.Kind.PREFIX_DECREMENT )
      {
        // ++, -- operators not supported with jailbreak access to fields, only direct assignment
        _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_INCREMENT_OP_NOT_ALLOWED_REFLECTION.get() );
        Types types = Types.instance( ((BasicJavacTask)_tp.getJavacTask()).getContext() );
        tree.type = types.createErrorType( tree.type );
      }
    }

    result = tree;
  }

  // indexed[index]--
  //
  //var tempIndexed = indexed;
  //var tempIndex = index;
  //var tempReturn = tempIndexed[tempIndex]  or  tempIndexed.get(tempIndex)
  //methodCall = tempReturn.dec()
  //var tempAssign = tempIndexed[tempIndex] = methodCall  or  tempIndexed.set(tempIndex, methodCall)
  //
  //tempReturn
  private void genUnaryIncDec( JCTree.JCUnary tree, TreeMaker make, Symbol.MethodSymbol operatorMethod )
  {
    boolean isPost = tree.getTag() == JCTree.Tag.POSTDEC || tree.getTag() == JCTree.Tag.POSTINC;

    tempVarIndex++;

    List<JCTree.JCVariableDecl> tempVars = List.nil();
    JCExpression receiver = tree.arg;
    Context ctx = JavacPlugin.instance().getContext();
    JCExpression answer;

    if( receiver instanceof JCTree.JCArrayAccess )
    {
      // the LHS is indexed eg. indexed[index]--
      // note indexed[index] could be either an array or an expression using indexed operator overloading

      // store the index in a temp var
      JCExpression index = ((JCTree.JCArrayAccess)receiver).index;
      JCTree[] indexTemp = tempify( tree, make, index, ctx, getEnclosingSymbol( tree, ctx ), "$indexExprTemp" + tempVarIndex );
      if( indexTemp != null )
      {
        tempVars = tempVars.append( (JCTree.JCVariableDecl)indexTemp[0] );
        index = (JCExpression)indexTemp[1];
      }

      // store indexed in a temp var
      JCTree[] indexedTemp = tempify( true, tree, make, ((JCTree.JCArrayAccess)receiver).indexed, ctx, getEnclosingSymbol( tree, ctx ), "$indexedExprTemp" + tempVarIndex );
      tempVars = tempVars.append( (JCTree.JCVariableDecl)indexedTemp[0] );
      receiver = (JCExpression)indexedTemp[1];

      // store value of indexed[index] in a temp var
      Symbol.MethodSymbol indexedGetMethod = ManAttr.resolveIndexGetMethod( _tp.getTypes(), receiver.type, index.type );
      if( indexedGetMethod != null )
      {
        // indexed[index] uses indexed operator overloading (get/set by index)

        JCTree.JCMethodInvocation methodCall = make.Apply( List.nil(), IDynamicJdk.instance().Select( make, receiver, indexedGetMethod ), List.of( index ) );
        methodCall.setPos( tree.pos );
        methodCall.type = tree.arg.type;
        methodCall = maybeReplaceWithExtensionMethod( methodCall );

        JCTree[] oldValTemp = tempify( true, tree, make, methodCall, ctx, getEnclosingSymbol( tree, ctx ), "$oldValueTemp" + tempVarIndex );
        tempVars = tempVars.append( (JCTree.JCVariableDecl)oldValTemp[0] );
        answer = (JCExpression)oldValTemp[1];
      }
      else
      {
        // indexed[index] is a normal array access expression

        JCTree.JCArrayAccess arrayAccess = make.Indexed( receiver, index );
        arrayAccess.setPos( tree.pos );
        arrayAccess.type = tree.arg.type;

        JCTree[] oldValTemp = tempify( true, tree, make, arrayAccess, ctx, getEnclosingSymbol( tree, ctx ), "$oldValueTemp" + tempVarIndex );
        tempVars = tempVars.append( (JCTree.JCVariableDecl)oldValTemp[0] );
        answer = (JCExpression)oldValTemp[1];
      }

      // make the expression for the incremented/decremented value.
      // this is either the inc/dec call, which may be a provided inc/dec operator overload,
      //    or
      // if indexed[index] uses indexed operator overloading and evaluates to a primitive type,
      // this is a binary expression "indexed[index] + 1"
      JCExpression incDecValue = makeUnaryIncDecCall( tree, make, operatorMethod, answer );

      // assign the incremented/decremented value to the indexed[index]
      Symbol.MethodSymbol indexedSetMethod = ManAttr.resolveIndexSetMethod( _tp.getTypes(), receiver.type, index.type );
      if( indexedSetMethod != null )
      {
        // incDecValue = indexed.set(index, incDecValue)

        JCTree[] incDecTempValue = tempify( true, tree, make, incDecValue, ctx, getEnclosingSymbol( tree, ctx ), "$assignExprTemp2" + tempVarIndex );
        tempVars = tempVars.append( (JCTree.JCVariableDecl)incDecTempValue[0] );
        incDecValue = (JCExpression)incDecTempValue[1];

        JCTree.JCMethodInvocation setCall = make.Apply( List.nil(), IDynamicJdk.instance().Select( make, receiver, indexedSetMethod ), List.of( index, incDecValue ) );
        setCall.setPos( tree.pos );
        setCall.type = tree.arg.type;

        // If methodCall is an extension method, rewrite it accordingly
        setCall = maybeReplaceWithExtensionMethod( setCall );
        if( !isPost )
        {
          answer = incDecValue;
        }

        incDecValue = setCall;
      }
      else
      {
        // indexed[index] = incDecValue

        JCTree.JCArrayAccess arrayAccess = make.Indexed( receiver, index );
        arrayAccess.setPos( tree.pos );
        arrayAccess.type = tree.arg.type;

        JCTree.JCAssign assign = make.Assign( arrayAccess, incDecValue );
        assign.type = arrayAccess.type;
        assign.setPos( tree.pos );
        JCTree[] assignTempVar = tempify( true, tree, make, assign, ctx, getEnclosingSymbol( tree, ctx ), "$assignExprTemp2" + tempVarIndex );
        tempVars = tempVars.append( (JCTree.JCVariableDecl)assignTempVar[0] );
        if( !isPost )
        {
          answer = (JCExpression)assignTempVar[1];
        }

        incDecValue = assign;
      }

      // use a temp var to evaluate the assignment in a compound expression
      JCTree[] assignTempVar = tempify( true, tree, make, incDecValue, ctx, getEnclosingSymbol( tree, ctx ), "$assignExprTemp" + tempVarIndex );
      tempVars = tempVars.append( (JCTree.JCVariableDecl)assignTempVar[0] );
    }
    else
    {
      // not an array (not indexed)

      JCTree[] oldValTemp = tempify( true, tree, make, receiver, ctx, getEnclosingSymbol( tree, ctx ), "$oldValueTemp" + tempVarIndex );
      tempVars = tempVars.append( (JCTree.JCVariableDecl)oldValTemp[0] );
      answer = (JCExpression)oldValTemp[1];

      JCTree.JCExpression methodCall = makeUnaryIncDecCall( tree, make, operatorMethod, answer );

      JCTree.JCAssign assign = make.Assign( receiver, methodCall );
      assign.type = answer.type;
      assign.setPos( tree.pos );
      JCTree[] assignTempVar = tempify( true, tree, make, assign, ctx, getEnclosingSymbol( tree, ctx ), "$assignExprTemp" + tempVarIndex );
      tempVars = tempVars.append( (JCTree.JCVariableDecl)assignTempVar[0] );
      if( !isPost )
      {
        answer = (JCExpression)assignTempVar[1];
      }
    }

    // note, the "let" expression provides the following needed features:
    // - ensures we don't compile an expression multiple times, instead the expressions are initializers of the vars
    // - a way to have a compound expression, for instance "temp = a = a.ref" lets us perform the assignment as an expr
    // - handle postfix inc/dec value where the value of the postfix expr is the lhs beforehand
    result = ILetExpr.makeLetExpr( make, tempVars, answer, tree.arg.type, tree.pos );
  }

  public JCTree.JCMethodInvocation maybeReplaceWithExtensionMethod( JCTree.JCMethodInvocation methodCall )
  {
    // If methodCall is an extension method, rewrite it accordingly
    boolean[] isSmartStatic = {false};
    Symbol.MethodSymbol extMethod = findExtMethod( methodCall, isSmartStatic );
    if( extMethod != null )
    {
      // Replace with extension method call
      methodCall = replaceExtCall( methodCall, extMethod, isSmartStatic[0] );
    }
    return methodCall;
  }

  public JCTree.JCMethodInvocation maybeReplaceWithStructuralCall( JCTree.JCMethodInvocation methodCall )
  {
    if( isStructuralMethod( methodCall ) )
    {
      // The structural interface method is implemented directly in the type or supertype hierarchy,
      // replace with proxy call
      methodCall = replaceStructuralCall( methodCall );
    }
    return methodCall;
  }

  private JCTree.JCExpression makeUnaryIncDecCall( JCTree.JCUnary tree, TreeMaker make, Symbol.MethodSymbol operatorMethod, JCExpression operand )
  {
    if( operatorMethod == null )
    {
      Type unboxedType = _tp.getTypes().unboxedType( operand.type );
      if( unboxedType != null && !unboxedType.hasTag( NONE ) )
      {
        operand = unbox( _tp.getTypes(), _tp.getTreeMaker(), Names.instance( JavacPlugin.instance().getContext() ),
          _tp.getContext(), _tp.getCompilationUnit(), operand, unboxedType );
      }

      //
      // the only way operatorMethod can be null is when the inc/dec operand is indexed, but not an array and the indexed elems are primitive
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
      env.toplevel = (JCTree.JCCompilationUnit)_tp.getCompilationUnit();
      env.enclClass = getEnclosingClass( tree );
      if( JreUtil.isJava8() )
      {
        binary.operator = resolveMethod( tree.pos(), Names.instance( _tp.getContext() ).fromString( binary.getTag() == JCTree.Tag.PLUS ? "+" : "-" ), _tp.getSymtab().predefClass.type, List.of( binary.lhs.type, binary.rhs.type ) );
      }
      else
      {
        //reflective: binary.operator = Operators.instance( _tp.getContext ).resolveBinary( ... );
        Object operators = ReflectUtil.method( "com.sun.tools.javac.comp.Operators", "instance", Context.class ).invokeStatic( _tp.getContext() );
        ReflectUtil.field( binary, "operator" ).set( ReflectUtil.method( operators, "resolveBinary", JCDiagnostic.DiagnosticPosition.class, JCTree.Tag.class, Type.class, Type.class )
          .invoke( tree.pos(), binary.getTag(), binary.lhs.type, binary.rhs.type ) );
      }
      return binary;

      // maybe unbox here?
    }
    else
    {
      JCTree.JCMethodInvocation methodCall = make.Apply( List.nil(), IDynamicJdk.instance().Select( make, operand, operatorMethod ), List.nil() );
      methodCall.setPos( tree.pos );
      methodCall.type = operatorMethod.getReturnType();

      // If methodCall is an extension method, rewrite it accordingly
      methodCall = maybeReplaceWithExtensionMethod( methodCall );
      return methodCall;
    }
  }

  // --indexed[index]
  //
  //var tempIndexed = indexed;
  //var tempIndex = index;
  //var tempReturn = tempIndexed[tempIndex]  or  tempIndexed.get(tempIndex)
  //methodCall = tempReturn.dec()
  //var tempAssign = tempIndexed[tempIndex] = methodCall  or  tempIndexed.set(tempIndex, methodCall)
  //
  //tempReturn
  private void genUnaryPreIncDec( JCTree.JCUnary tree, TreeMaker make, Symbol.MethodSymbol operatorMethod )
  {
    JCTree.JCMethodInvocation methodCall;
    methodCall = make.Apply( List.nil(), IDynamicJdk.instance().Select( make, tree.arg, operatorMethod ), List.nil() );
    methodCall.setPos( tree.pos );
    methodCall.type = operatorMethod.getReturnType();

    // If methodCall is an extension method, rewrite it accordingly
    methodCall = maybeReplaceWithExtensionMethod( methodCall );

    JCTree.JCAssign assign = make.Assign( tree.arg, methodCall );
    assign.type = tree.arg.type;
    assign.setPos( tree.pos );

    result = assign;
  }

  public void genUnary( JCTree.JCUnary tree, TreeMaker make, Symbol.MethodSymbol operatorMethod )
  {
    JCExpression receiver = tree.getExpression();
    JCTree.JCMethodInvocation methodCall = make.Apply( List.nil(), IDynamicJdk.instance().Select( make, receiver, operatorMethod ), List.nil() );
    methodCall.setPos( tree.pos );
    methodCall.type = operatorMethod.getReturnType();

    // If methodCall is an extension method, rewrite it accordingly
    methodCall = maybeReplaceWithExtensionMethod( methodCall );

    result = methodCall;
  }

  @Override
  public void visitNewClass( JCTree.JCNewClass tree )
  {
    super.visitNewClass( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( isJailbreakReceiver( tree ) )
    {
      result = replaceWithReflection( tree );
    }
    else
    {
      result = tree;
    }
  }

  @Override
  public void visitConditional( JCTree.JCConditional tree )
  {
    super.visitConditional( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( TypesUtil.isStructuralInterface( _tp.getTypes(), tree.type.tsym ) )
    {
      // erase type if structural interface
      Symbol.ClassSymbol objectSym = getObjectClass();
      tree.type = objectSym.type;
    }
  }

  public void visitVarDef( JCTree.JCVariableDecl tree )
  {
    if( shouldProcessForLambdaGeneration() && (tree.getModifiers().flags & PARAMETER) != 0 )
    {
      _parameter = tree;
    }
    try
    {
      super.visitVarDef(tree);
    }
    finally
    {
      _parameter = null;
    }

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    JCExpression vartype = tree.vartype;
    if( vartype instanceof JCTree.JCAnnotatedType )
    {
      if( ((JCTree.JCAnnotatedType)tree.vartype).getAnnotations().stream()
        .anyMatch( anno -> Jailbreak.class.getTypeName().equals( anno.attribute.type.toString() ) ) )
      {
        // if the type itself is inaccessible and annotated with @Jailbreak, it will be erased to Object
        tree.type = ((JCTree.JCAnnotatedType)tree.vartype).underlyingType.type;
        tree.sym.type = _tp.getSymtab().objectType;
      }
//      else if( tree.vartype.type ==
//        IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), Null.class.getTypeName() ).type )
//      {
//        // if the type is manifold.rt.api.Null, replace with Object
//        Symbol.ClassSymbol objectSym = getObjectClass();
//        JCTree.JCIdent objIdent = _tp.getTreeMaker().Ident( objectSym );
//        tree.type = objectSym.type;
//        objIdent.type = objectSym.type;
//        tree.sym.type = objectSym.type;
//        tree.vartype = objIdent;
//        tree.sym.type = _tp.getSymtab().objectType;
//      }
    }
  }

  @Override
  public void visitTypeCast( JCTypeCast tree )
  {
    super.visitTypeCast( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      eraseCompilerGeneratedCast( tree );

      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( TypesUtil.isStructuralInterface( _tp.getTypes(), tree.type.tsym ) )
    {
      Type objectType = _tp.getSymtab().objectType;
      tree.expr = makeCast( tree.getExpression(), objectType );
      tree.type = objectType;
    }
    else
    {
      JCTree castType = tree.clazz;
      if( castType instanceof JCTree.JCAnnotatedType )
      {
        JCTree.JCAnnotatedType annoType = (JCTree.JCAnnotatedType)castType;
        if( annoType.getAnnotations().stream()
          .anyMatch( anno -> Jailbreak.class.getTypeName().equals( anno.attribute.type.toString() ) ) )
        {
          // Prevent the cast, the jailbreak type cannot be referenced in bytecode,
          // everything works due to erasure of jailbreak types and reflection
          tree.type = tree.expr.type;
          tree.clazz.type = tree.type;
          annoType.underlyingType.type = tree.type;
          if( annoType.underlyingType instanceof JCTree.JCFieldAccess )
          {
            ((JCTree.JCFieldAccess)annoType.underlyingType).sym.type = tree.type;
          }
        }
      }
    }
    result = tree;
  }

  @Override
  public void visitLiteral( JCTree.JCLiteral tree )
  {
    super.visitLiteral( tree );

    if( _tp.isGenerate() && !shouldProcessForGeneration() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( tree.typetag.getKindLiteral() == Tree.Kind.STRING_LITERAL )
    {
      result = replaceStringLiteral( tree );
    }
    else
    {
      result = tree;
    }
  }

  private JCTree replaceStringLiteral( JCTree.JCLiteral tree )
  {
    String literal = (String)tree.getValue();
    if( !literal.trim().startsWith( FragmentProcessor.FRAGMENT_START ) ||
      !literal.contains( FragmentProcessor.FRAGMENT_END ) )
    {
      return tree;
    }

    if( _tp.getParent( tree ) instanceof JCTree.JCBinary )
    {
      // fragments not supported with string '+' concatenation
      return tree;
    }

    JCTree.JCClassDecl enclosingClass = getEnclosingClass( tree );

    CharSequence source = ParserFactoryFiles.getSource( enclosingClass.sym.sourcefile );
    CharSequence chars = source.subSequence( tree.pos().getStartPosition(),
      tree.pos().getEndPosition( ((JCTree.JCCompilationUnit)_tp.getCompilationUnit()).endPositions ) );
    HostKind hostKind = chars.length() > 3 && chars.charAt( 1 ) == '"'
      ? TEXT_BLOCK_LITERAL
      : DOUBLE_QUOTE_LITERAL;
    if( ManAttr.checkConcatenation( tree, chars, hostKind, null ) )
    {
      // string concat not supported with fragments
      return tree;
    }

    FragmentProcessor.Fragment fragment = FragmentProcessor.instance().parseFragment(
      tree.pos().getStartPosition(), chars.toString(), hostKind );
    if( fragment != null && isHandledFileExtension( fragment.getExt() ) )
    {
      String fragClass = enclosingClass.sym.packge().toString() + '.' + fragment.getName();
      Symbol.ClassSymbol fragSym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), fragClass );
      for( Attribute.Compound annotation : fragSym.getAnnotationMirrors() )
      {
        if( annotation.type.toString().equals( FragmentValue.class.getName() ) )
        {
          return replaceStringLiteral( fragSym, tree, annotation );
        }
      }
    }

    return tree;
  }

  private boolean isHandledFileExtension( String fileExt )
  {
    return _tp.getHost().getSingleModule().getTypeManifolds().stream()
      .filter( tm -> tm.getContributorKind() != ContributorKind.Supplemental )
      .anyMatch( tm -> tm.handlesFileExtension( fileExt.toLowerCase() ) );
  }

  private JCTree replaceStringLiteral( Symbol.ClassSymbol fragSym, JCTree.JCLiteral tree, Attribute.Compound attribute )
  {
    if( attribute == null )
    {
      return tree;
    }

    String methodName = null;
    String type = null;
    for( com.sun.tools.javac.util.Pair<Symbol.MethodSymbol, Attribute> pair : attribute.values )
    {
      Name argName = pair.fst.getSimpleName();
      if( argName.toString().equals( "methodName" ) )
      {
        methodName = (String)pair.snd.getValue();
      }
      else if( argName.toString().equals( "type" ) )
      {
        type = (String)pair.snd.getValue();
      }
    }

    if( type != null )
    {
      return replaceStringLiteral( fragSym, tree, methodName, type );
    }

    return tree;
  }

  private JCExpression replaceStringLiteral( Symbol.ClassSymbol fragSym, JCTree tree, String methodName, String type )
  {
    TreeMaker make = _tp.getTreeMaker();
    Names names = Names.instance( _tp.getContext() );

    Symbol.MethodSymbol fragmentValueMethod = resolveMethod( tree.pos(), names.fromString( methodName ),
      fragSym.type, List.nil() );

    JCTree.JCMethodInvocation fragmentValueCall = make.Apply( List.nil(),
      memberAccess( make, _tp.getElementUtil(), fragSym.getQualifiedName() + "." + methodName ), List.nil() );
    fragmentValueCall.type = fragmentValueMethod.getReturnType(); // type
    fragmentValueCall.setPos( tree.pos );
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)fragmentValueCall.getMethodSelect();
    newMethodSelect.sym = fragmentValueMethod;
    newMethodSelect.type = fragmentValueMethod.type;
    assignTypes( newMethodSelect.selected, fragSym );

    return fragmentValueCall;
  }

  private void eraseCompilerGeneratedCast( JCTypeCast tree )
  {
    // the javac compiler generates casts e.g., for a generic call such as List#get()

    if( TypesUtil.isStructuralInterface( _tp.getTypes(), tree.type.tsym ) && !isConstructProxyCall( tree.getExpression() ) )
    {
      tree.type = getObjectClass().type;
      TreeMaker make = _tp.getTreeMaker();
      tree.clazz = make.Type( getObjectClass().type );
    }
  }

  private boolean isConstructProxyCall( JCExpression expression )
  {
    if( expression instanceof JCTree.JCMethodInvocation )
    {
      // don't erase cast if we generated it here e.g.., for structural call cast on constructProxy

      JCExpression meth = ((JCTree.JCMethodInvocation)expression).meth;
      return meth instanceof JCTree.JCFieldAccess && ((JCTree.JCFieldAccess)meth).getIdentifier().toString().equals( "constructProxy" );
    }
    return expression instanceof JCTypeCast && isConstructProxyCall( ((JCTypeCast)expression).getExpression() );
  }

  /**
   * Replace all extension method call-sites with static calls to extension methods
   */
  @Override
  public void visitApply( JCTree.JCMethodInvocation tree )
  {
    super.visitApply( tree );

    eraseGenericStructuralVarargs( tree );

    if( _tp.isGenerate() &&
      // handle compiler-generated call to iterator(), sometimes a structural interface is involved here (lists in JSON)
      !isStructuralIteratorCall( tree ) )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    boolean isSmartStatic[] = {false};
    Symbol.MethodSymbol method = findExtMethod( tree, isSmartStatic );
    if( method != null )
    {
      // Replace with extension method call
      result = replaceExtCall( tree, method, isSmartStatic[0] );
    }
    else if( isStructuralMethod( tree ) )
    {
      // The structural interface method is implemented directly in the type or supertype hierarchy,
      // replace with proxy call
      result = replaceStructuralCall( tree );
    }
    else if( isJailbreakReceiver( tree ) )
    {
      result = replaceWithReflection( tree );
    }
    else
    {
      result = tree;
    }
  }

  private boolean isStructuralIteratorCall( JCTree.JCMethodInvocation tree )
  {
    return tree.meth instanceof JCTree.JCFieldAccess &&
      ((JCTree.JCFieldAccess)tree.meth).sym.name.toString().equals( "iterator" ) &&
      isStructuralMethod( tree );
  }

  @Override
  public void visitReference( JCTree.JCMemberReference tree )
  {
    super.visitReference( tree );

    if( isExtensionMethod( tree.sym ) )
    {
      // Method references not supported on extension methods

      _tp.report( tree, Diagnostic.Kind.ERROR,
        ExtIssueMsg.MSG_EXTENSION_METHOD_REF_NOT_SUPPORTED.get( tree.sym.flatName() ) );
    }
    else if( isStructuralMethod( tree.sym ) )
    {
      // Method references not supported on structural interface methods

      _tp.report( tree, Diagnostic.Kind.ERROR,
        ExtIssueMsg.MSG_STRUCTURAL_METHOD_REF_NOT_SUPPORTED.get( tree.sym.flatName() ) );

      //todo, see LambdaToMethod for how javac translates a method ref to a lambda, setting ownerAccessible and then
      // utilizing LambdaToMethod directly should work:
      //      - tree.ownerAccessible = false;
      //      - call into LambdaToMethod to transform the method ref to a lambda expr
      //      - call configMethod() here in case the method is an extension method
    }
  }

  private boolean isStructuralMethod( Symbol sym )
  {
    if( sym != null && !sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC ) )
    {
      if( !isObjectMethod( sym ) )
      {
        return TypesUtil.isStructuralInterface( _tp.getTypes(), sym.owner );
      }
    }
    return false;
  }

  private boolean isExtensionMethod( Symbol sym )
  {
    if( sym instanceof Symbol.MethodSymbol )
    {
      for( Attribute.Compound annotation : sym.getAnnotationMirrors() )
      {
        if( annotation.type.toString().equals( ExtensionMethod.class.getName() ) )
        {
          return true;
        }
      }
    }
    return false;
  }

  @SuppressWarnings( "WeakerAccess" )
  public static boolean isJailbreakReceiver( JCTree tree )
  {
    if( tree instanceof JCTree.JCFieldAccess )
    {
      return isJailbreakReceiver( (JCTree.JCFieldAccess)tree );
    }
    else if( tree instanceof JCTree.JCMethodInvocation )
    {
      JCExpression methodSelect = ((JCTree.JCMethodInvocation)tree).getMethodSelect();
      return isJailbreakReceiver( methodSelect );
    }
    else if( tree instanceof JCTree.JCAssign )
    {
      JCExpression lhs = ((JCTree.JCAssign)tree).lhs;
      return isJailbreakReceiver( lhs );
    }
    else if( tree instanceof JCTree.JCAssignOp )
    {
      JCExpression lhs = ((JCTree.JCAssignOp)tree).lhs;
      return isJailbreakReceiver( lhs );
    }
    else if( tree instanceof JCTree.JCUnary )
    {
      JCExpression arg = ((JCTree.JCUnary)tree).arg;
      return isJailbreakReceiver( arg );
    }
    else if( tree instanceof JCTree.JCVariableDecl )
    {
      JCExpression initializer = ((JCTree.JCVariableDecl)tree).init;
      return isJailbreakReceiver( initializer );
    }
    else if( tree instanceof JCTree.JCTypeCast &&
      ((JCTypeCast)tree).clazz instanceof JCTree.JCAnnotatedType )
    {
      JCTree.JCAnnotatedType annotatedType = (JCTree.JCAnnotatedType)((JCTypeCast)tree).clazz;
      return annotatedType.getAnnotations().stream()
        .anyMatch( anno -> Jailbreak.class.getTypeName().equals( anno.attribute.type.toString() ) );
    }
    else if( tree instanceof JCTree.JCNewClass )
    {
      return isJailbreakReceiver( (JCTree.JCNewClass)tree );
    }
    return false;
  }

  // called reflectively from manifold core
  @SuppressWarnings( "WeakerAccess" )
  public static boolean isJailbreakReceiver( JCTree.JCFieldAccess fieldAccess )
  {
    Symbol sym = null;
    JCExpression selected = fieldAccess.selected;
    if( selected instanceof JCTree.JCIdent )
    {
      sym = ((JCTree.JCIdent)selected).sym;
    }
    else if( selected instanceof JCTree.JCParens )
    {
      return isJailbreakReceiver( ((JCTree.JCParens)selected).expr );
    }
    else if( selected instanceof JCTree.JCMethodInvocation )
    {
      if( ((JCTree.JCMethodInvocation)selected).meth instanceof JCTree.JCFieldAccess )
      {
        sym = ((JCTree.JCFieldAccess)((JCTree.JCMethodInvocation)selected).meth).sym;
      }
      else if( ((JCTree.JCMethodInvocation)selected).meth instanceof JCTree.JCIdent )
      {
        sym = ((JCTree.JCIdent)((JCTree.JCMethodInvocation)selected).meth).sym;
      }
    }

    return isJailbreakSymbol( sym );
  }

  // Called reflectively from manifold core
  @SuppressWarnings( "WeakerAccess" )
  public static boolean isJailbreakSymbol( Symbol sym )
  {
    if( sym == null )
    {
      return false;
    }

    SymbolMetadata metadata = sym.getMetadata();
    if( metadata == null || (metadata.isTypesEmpty() && metadata.isEmpty()) )
    {
      return false;
    }

    List<Attribute.TypeCompound> typeAttributes = metadata.getTypeAttributes();
    if( !typeAttributes.isEmpty() )
    {
      return typeAttributes.stream()
        .anyMatch( attr -> attr.type.toString().equals( Jailbreak.class.getTypeName() ) );
    }

    List<Attribute.Compound> attributes = metadata.getDeclarationAttributes();
    if( !attributes.isEmpty() )
    {
      return attributes.stream()
        .anyMatch( attr -> attr.type.toString().equals( Jailbreak.class.getTypeName() ) );
    }
    return false;
  }

  private static boolean isJailbreakReceiver( JCTree.JCNewClass newExpr )
  {
    JCExpression classExpr = newExpr.clazz;
    if( classExpr instanceof JCTree.JCAnnotatedType )
    {
      return ((JCTree.JCAnnotatedType)classExpr).annotations.stream()
        .anyMatch( e -> Jailbreak.class.getTypeName().equals( e.attribute.type.toString() ) );
    }
    return false;
  }

  @Override
  public void visitForeachLoop( JCTree.JCEnhancedForLoop tree )
  {
    super.visitForeachLoop( tree );

    // Support case where Iterable is made structural via extension.
    // In this case foreach should work with types that implement Iterable structurally.

    Type iterableType = _tp.getTypes().erasure( _tp.getSymtab().iterableType );

    if( TypesUtil.isStructuralInterface( _tp.getTypes(), iterableType.tsym ) &&
      _tp.getTypes().isAssignable( tree.expr.type.tsym.type, iterableType ) )
    {
      // replace with: makeIterable( expr.iterator() )

      Names names = Names.instance( _tp.getContext() );
      Symbol.ClassSymbol runtimeMethodsClassSym = getRtClassSym( RuntimeMethods.class );

      Symbol.MethodSymbol makeIterableMethod = resolveMethod( tree.expr.pos(), names.fromString( "makeIterable" ), runtimeMethodsClassSym.type,
        List.from( new Type[]{_tp.getSymtab().iteratorType} ) );

      TreeMaker make = _tp.getTreeMaker();
      JavacElements javacElems = _tp.getElementUtil();

      Symbol.MethodSymbol iteratorMethSym = resolveMethod( tree.expr.pos(), names.fromString( "iterator" ), tree.expr.type, List.nil() );
      JCTree.JCFieldAccess iterMethAccess = make.Select( tree.expr, names.fromString( "iterator" ) );
      iterMethAccess.type = getIteratorType( names, tree.expr.type );
      iterMethAccess.sym = iteratorMethSym;
      JCTree.JCMethodInvocation iteratorCall = make.Apply( List.nil(), iterMethAccess, List.nil() );
      iteratorCall.setPos( tree.expr.pos );
      iteratorCall.type = iterMethAccess.type;
      ((JCTree.JCFieldAccess)iteratorCall.meth).sym = iteratorMethSym;
      iteratorCall = maybeReplaceWithExtensionMethod( iteratorCall );
      iteratorCall = maybeReplaceWithStructuralCall( iteratorCall );

      JCTree.JCMethodInvocation makeIterableCall = make.Apply( List.nil(),
        memberAccess( make, javacElems, RuntimeMethods.class.getTypeName() + ".makeIterable" ), List.of( iteratorCall ) );
      makeIterableCall.setPos( tree.expr.pos );
      makeIterableCall.type = iterableType;
      JCTree.JCFieldAccess methodSelect = (JCTree.JCFieldAccess)makeIterableCall.getMethodSelect();
      methodSelect.sym = makeIterableMethod;
      methodSelect.type = makeIterableMethod.type;
      methodSelect.pos = tree.expr.pos;
      assignTypes( methodSelect.selected, runtimeMethodsClassSym );
      methodSelect.selected.pos = tree.expr.pos;
      tree.expr = makeIterableCall;
    }
  }

  private Type getIteratorType( Names names, Type type )
  {
    Iterable<Symbol> matches = IDynamicJdk.instance().getMembersByName( (Symbol.ClassSymbol)type.tsym, names.fromString( "iterator" ) );
    for( Symbol s: matches )
    {
      if( s instanceof Symbol.MethodSymbol && ((Symbol.MethodSymbol)s).params().isEmpty() )
      {
        return _tp.getTypes().memberType( type, s ).getReturnType();
      }
    }
    //## todo: should be a compile error?
    return _tp.getTypes().erasure( _tp.getSymtab().iteratorType );
  }

  @Override
  public void visitAnnotation( JCTree.JCAnnotation tree )
  {
    super.visitAnnotation( tree );
    if( tree.getAnnotationType().type == null || !Self.class.getTypeName().equals( tree.getAnnotationType().type.tsym.toString() ) )
    {
      return;
    }

    if( !isSelfInMethodDeclOrFieldDecl( tree, tree ) )
    {
      _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_SELF_NOT_ALLOWED_HERE.get() );
    }
    else
    {
      verifySelfOnThis( tree, tree );
    }
  }

  private void verifySelfOnThis( JCTree annotated, JCTree.JCAnnotation selfAnno )
  {
    String fqn;
    if( annotated instanceof JCTree.JCAnnotatedType )
    {
      fqn = ((JCTree.JCAnnotatedType)annotated).getUnderlyingType().type.tsym.getQualifiedName().toString();
    }
    else if( annotated instanceof JCTree.JCMethodDecl )
    {
      fqn = ((JCTree.JCMethodDecl)annotated).getReturnType().type.tsym.getQualifiedName().toString();
    }
    else
    {
      //## todo: shouldn't happen
      return;
    }

    try
    {
      JCTree.JCClassDecl enclosingClass = _tp.getClassDecl( annotated );
      if( !isDeclaringClassOrExtension( annotated, fqn, enclosingClass ) && !fqn.equals( "Array" ) )
      {
        _tp.report( selfAnno, Diagnostic.Kind.ERROR,
          ExtIssueMsg.MSG_SELF_NOT_ON_CORRECT_TYPE.get( fqn, enclosingClass.sym.getQualifiedName() ) );
      }
    }
    catch( Throwable ignore )
    {
    }
  }

  private boolean isDeclaringClassOrExtension( JCTree annotated, String fqn, JCTree.JCClassDecl enclosingClass )
  {
    if( enclosingClass.sym.getQualifiedName().toString().equals( fqn ) ||
      _tp.getTypes().isAssignable( IDynamicJdk.instance().getTypeElement(
        _tp.getContext(), _tp.getCompilationUnit(), fqn ).asType(), enclosingClass.sym.type ) )
    {
      return true;
    }

    return isOnExtensionMethod( annotated, fqn, enclosingClass );
  }

  private boolean isOnExtensionMethod( JCTree annotated, String fqn, JCTree.JCClassDecl enclosingClass )
  {
    if( isExtensionClass( enclosingClass ) )
    {
      String extendedClassName = getExtendedClassName();
      if( extendedClassName != null && extendedClassName.equals( fqn ) )
      {
        JCTree.JCMethodDecl declMethod = findDeclMethod( annotated );
        if( declMethod != null )
        {
          List<JCTree.JCVariableDecl> parameters = declMethod.getParameters();
          for( JCTree.JCVariableDecl param : parameters )
          {
            if( hasAnnotation( param.getModifiers().getAnnotations(), This.class ) )
            {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private JCTree.JCMethodDecl findDeclMethod( Tree annotated )
  {
    if( annotated == null )
    {
      return null;
    }

    if( annotated instanceof JCTree.JCMethodDecl )
    {
      return (JCTree.JCMethodDecl)annotated;
    }
    return findDeclMethod( _tp.getParent( annotated ) );
  }

  private boolean isSelfInMethodDeclOrFieldDecl( Tree tree, JCTree.JCAnnotation anno )
  {
    if( tree == null )
    {
      return false;
    }

    Tree parent = _tp.getParent( tree );

    if( parent instanceof JCTree.JCMethodDecl )
    {
      return true;
    }

    if( parent instanceof JCTree.JCVariableDecl )
    {
      Tree container = _tp.getParent( parent );
      if( container instanceof JCTree.JCClassDecl )
      {
        // @Self allowed only on class var, not local var
        return true;
      }
    }
    return isSelfInMethodDeclOrFieldDecl( parent, anno );
  }

  @Override
  public void visitClassDef( JCTree.JCClassDecl tree )
  {
    super.visitClassDef( tree );

    verifyExtensionInterfaces( tree );

    checkExtensionClassError( tree );

    precompileClasses( tree );

    compileGeneratedProxyFactoryClasses( tree );

    incrementalCompileClasses( tree );
  }

  private void precompileClasses( JCTree.JCClassDecl tree )
  {
    Map<String, Set<String>> typeNames = new HashMap<>();
    for( JCTree.JCAnnotation anno : tree.getModifiers().getAnnotations() )
    {
      if( anno.getAnnotationType().type.toString().equals( Precompile.class.getCanonicalName() ) )
      {
        getTypesToCompile( anno, typeNames );
      }
    }

    if( !typeNames.isEmpty() )
    {
      precompile( typeNames );
    }
  }

  private void getTypesToCompile( JCTree.JCAnnotation precompileAnno, Map<String, Set<String>> typeNames )
  {
    Attribute.Compound attribute = precompileAnno.attribute;
    if( attribute == null )
    {
      return;
    }

    String typeManifoldClassName = null;
    String regex = ".*";
    String ext = "*";
    for( com.sun.tools.javac.util.Pair<Symbol.MethodSymbol, Attribute> pair : attribute.values )
    {
      Name argName = pair.fst.getSimpleName();
      switch( argName.toString() )
      {
        case "typeManifold":
          typeManifoldClassName = pair.snd.getValue().toString();
          break;
        case "fileExtension":
          ext = pair.snd.getValue().toString();
          break;
        case "typeNames":
          regex = pair.snd.getValue().toString();
          break;
      }
    }

    addToPrecompile( typeNames, typeManifoldClassName, ext, regex );
  }

  private void addToPrecompile( Map<String, Set<String>> typeNames, String typeManifoldClassName, String ext, String regex )
  {
    if( typeManifoldClassName != null )
    {
      Set<String> regexes = typeNames.computeIfAbsent( typeManifoldClassName, tm -> new HashSet<>() );
      regexes.add( regex );
    }
    else
    {
      boolean all = "*".equals( ext );
      _tp.getHost().getSingleModule().getTypeManifolds().stream()
        .filter( tm -> tm.getContributorKind() != ContributorKind.Supplemental )
        .forEach( tm ->
        {
          if( all || tm.handlesFileExtension( ext ) )
          {
            String classname = tm.getClass().getTypeName();
            Set<String> regexes = typeNames.computeIfAbsent( classname, e -> new HashSet<>() );
            regexes.add( regex );
          }
        } );
    }
  }

  private void precompile( Map<String, Set<String>> typeNames )
  {
    for( ITypeManifold tm : _tp.getHost().getSingleModule().getTypeManifolds() )
    {
      for( Map.Entry<String, Set<String>> entry : typeNames.entrySet() )
      {
        String typeManifoldClassName = entry.getKey();
        if( tm.getClass().getName().equals( typeManifoldClassName ) )
        {
          Collection<String> namesToPrecompile = computeNamesToPrecompile( tm.getAllTypeNames(), entry.getValue() );
          for( String fqn : namesToPrecompile )
          {
            // This call surfaces the type in the compiler.  If compiling in "static" mode, this means
            // the type will be compiled to disk.
            IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), fqn );
          }
          break;
        }
      }
    }
  }

  private Collection<String> computeNamesToPrecompile( Collection<String> allTypeNames, Set<String> regexes )
  {
    Set<String> matchingTypes = new HashSet<>();
    for( String fqn : allTypeNames )
    {
      if( regexes.stream().anyMatch( fqn::matches ) )
      {
        matchingTypes.add( fqn );
      }
    }
    return matchingTypes;
  }

  private void compileGeneratedProxyFactoryClasses( JCTree.JCClassDecl tree )
  {
    if( isExtensionClass( tree ) )
    {
      StaticCompiler.instance().surfaceGeneratedProxyFactoryClasses( _tp.getContext(), _tp.getCompilationUnit() );
    }
  }

  private void incrementalCompileClasses( JCTree.JCClassDecl tree )
  {
    if( _tp.isGenerate() )
    {
      return;
    }

    // Keep track of Manifold types compiled, for hotswap compile drivers, and more generally for JPS build management
    // e.g., to keep track of resource file to .class files so subsequent incremental build can delete .class files and
    // recompile changed resources.
    mapResourceFileToTargetClassFiles( tree );

    // Ensure modified resource files are compiled, stemming from incremental compilation only
    incrementalCompile( tree );
  }

  private Set<Object> findDrivers( JCTree.JCClassDecl tree )
  {
    if( isIncremental() )
    {
      // an incremental build has a driver per module per test/production/etc. target, where each driver maintains
      // changed resource files for that target only. This helps with forcing the types corresponding with the resources
      // to compile, but only ones that pertain to the target (where the generated _Manifold_Temp_Main_.java is). The
      // compilation is done in incrementalCompile().

      Set<Object> drivers = new HashSet<>();
      for( JCTree.JCAnnotation anno : tree.getModifiers().getAnnotations() )
      {
        if( anno.getAnnotationType().type.toString().equals( IncrementalCompile.class.getCanonicalName() ) )
        {
          getIncrementalCompileDrivers( anno, drivers );
        }
      }
      _tp.addDrivers( drivers );
    }

    return _tp.getDrivers();
  }

  private boolean isIncremental()
  {
    JavacPlugin javacPlugin = JavacPlugin.instance();
    return javacPlugin != null && javacPlugin.isIncremental();
  }

  private void getIncrementalCompileDrivers( JCTree.JCAnnotation anno, Set<Object> drivers )
  {
    Attribute.Compound attribute = anno.attribute;
    if( attribute == null )
    {
      return;
    }

    String fqnDriver = null;
    Integer driverId = null;
    for( com.sun.tools.javac.util.Pair<Symbol.MethodSymbol, Attribute> pair : attribute.values )
    {
      Name argName = pair.fst.getSimpleName();
      if( argName.toString().equals( "driverInstance" ) )
      {
        driverId = (int)pair.snd.getValue();
      }
      else if( argName.toString().equals( "driverClass" ) )
      {
        fqnDriver = (String)pair.snd.getValue();
      }
    }

    if( driverId != null )
    {
      Object driver = ReflectUtil.method( fqnDriver, "getInstance", int.class ).invokeStatic( driverId );
      drivers.add( driver );
    }
  }

  private void incrementalCompile( JCTree.JCClassDecl tree )
  {
    Set<Object> drivers = findDrivers( tree );
    for( Object driver : drivers )
    {
      JavacPlugin.instance().setIncremental();

      //noinspection unchecked
      Collection<File> changedFiles = (Collection<File>)ReflectUtil.method( driver, "getChangedFiles" ).invoke();
      if( changedFiles == null || changedFiles.isEmpty() )
      {
        // nothing to compile
        continue;
      }

      IManifoldHost host = _tp.getHost();
      Set<IFile> changes = changedFiles.stream().map( ( File f ) -> host.getFileSystem().getIFile( f ) )
        .collect( Collectors.toSet() );
      for( ITypeManifold tm : host.getSingleModule().getTypeManifolds() )
      {
        for( IFile file : changes )
        {
          Set<String> types = Arrays.stream( tm.getTypesForFile( file ) ).collect( Collectors.toSet() );
          if( types.size() > 0 )
          {
            Map<File, Set<String>> typesToFile = getTypesToFile();
            if( typesToFile != null )
            {
              typesToFile.put( file.toJavaFile(), types );
            }

            for( String fqn : types )
            {
              // This call surfaces the type in the compiler.  If compiling in "static" mode, this means
              // the type will be compiled to disk.
              Symbol.ClassSymbol classSym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), fqn );
              assert classSym != null;
              changedFiles.remove( file.toJavaFile() );
            }
          }
        }
      }
    }
  }

  private void mapResourceFileToTargetClassFiles( JCTree.JCClassDecl tree )
  {
    if( tree.sym == null )
    {
      return;
    }

    Name qualifiedName = tree.sym.getQualifiedName();
    if( qualifiedName == null )
    {
      return;
    }

    JavaFileObject sourcefile = tree.sym.sourcefile;
    if( !(sourcefile instanceof GeneratedJavaStubFileObject) )
    {
      // not a Manifold generated type
      return;
    }

    Map<File, Set<String>> typesCompiledByFile = new HashMap<>();
    Set<IFile> resourceFiles = ((GeneratedJavaStubFileObject)sourcefile).getResourceFiles();
    for( IFile ifile : resourceFiles )
    {
      File file;
      try
      {
        while( ifile instanceof IFileFragment )
        {
          ifile = ((IFileFragment)ifile).getEnclosingFile();
        }
        file = ifile.toJavaFile();
      }
      catch( Exception e )
      {
        continue;
      }

      Set<String> types = typesCompiledByFile.get( file );
      if( types == null )
      {
        typesCompiledByFile.put( file, types = new ConcurrentHashSet<>() );
      }
      StringBuilder sb = new StringBuilder();
      make$name( tree.sym, sb );
      types.add( sb.toString() );
    }

    mapResourceFileToTargetClassFiles( typesCompiledByFile );
  }

  private void make$name( Symbol.ClassSymbol sym, StringBuilder sb )
  {
    Symbol enclosingElement = sym.getEnclosingElement();
    if( enclosingElement instanceof Symbol.ClassSymbol )
    {
      make$name( (Symbol.ClassSymbol)enclosingElement, sb );
      sb.append( '$' ).append( sym.getSimpleName() );
    }
    else
    {
      sb.append( sym.getQualifiedName().toString() );
    }
  }

  private void mapResourceFileToTargetClassFiles( Map<File, Set<String>> typesCompiledByFile )
  {
    if( typesCompiledByFile.isEmpty() )
    {
      // nothing to add
      return;
    }

    // Keep track of compiled Manifold types during a Rebuild.
    //
    // Generally, since Manifold types are magically added to the build as
    // they are referenced, they need to be mapped in the JPS compilation
    // process to support hotswap debugging, etc.

    Map<File, Set<String>> typesToFile = getTypesToFile();
    if( typesToFile != null )
    {
      typesCompiledByFile.forEach( ( file, types ) -> {
        Set<String> existingTypes = typesToFile.get( file );
        if( existingTypes != null )
        {
          existingTypes.addAll( types );
        }
        else
        {
          typesToFile.put( file, types );
        }
      } );
    }
  }

  private Map<File, Set<String>> getTypesToFile()
  {
    //## todo: maybe make this a service so it is not specific to IntelliJ
    Class<?> type = ReflectUtil.type( "manifold.ij.jps.IjChangedResourceFiles" );
    if( type == null )
    {
      // not compiling with IJ
      return null;
    }
    return (Map<File, Set<String>>)ReflectUtil.method( type, "getTypesToFile" ).invokeStatic();
  }

  private void verifyExtensionInterfaces( JCTree.JCClassDecl tree )
  {
    if( !hasAnnotation( tree.getModifiers().getAnnotations(), Extension.class ) )
    {
      return;
    }

    outer:
    for( JCExpression iface : tree.getImplementsClause() )
    {
      final Symbol.TypeSymbol ifaceSym = iface.type.tsym;
      if( ifaceSym == _tp.getSymtab().objectType.tsym )
      {
        continue;
      }

      for( Attribute.Compound anno : ifaceSym.getAnnotationMirrors() )
      {
        if( anno.type.toString().equals( Structural.class.getName() ) )
        {
          continue outer;
        }
      }
      // extension interfaces must be structural
      _tp.report( iface, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_ONLY_STRUCTURAL_INTERFACE_ALLOWED_HERE.get( iface.toString() ) );
    }
  }

  private boolean shouldProcessForGeneration()
  {
    return _bridgeMethod;
  }

  private boolean shouldProcessForLambdaGeneration()
  {
    return _lambdaMethod;
  }

  private boolean isBridgeMethod( JCTree.JCMethodDecl tree )
  {
    long modifiers = tree.getModifiers().flags;
    return (Flags.BRIDGE & modifiers) != 0;
  }
  private boolean isLambdaMethod( JCTree.JCMethodDecl tree )
  {
    long modifiers = tree.getModifiers().flags;
    return (SYNTHETIC & modifiers) != 0;
  }

  private Type eraseStructureType( Type type )
  {
    return new StructuralTypeEraser( this ).visit( type );
  }

  private boolean isReceiver( JCTree tree )
  {
    Tree parent = _tp.getParent( tree );
    if( parent instanceof JCTree.JCFieldAccess )
    {
      return ((JCTree.JCFieldAccess)parent).getExpression() == tree;
    }
    return false;
  }

  Symbol.ClassSymbol getObjectClass()
  {
    Symtab symbols = Symtab.instance( _tp.getContext() );
    return (Symbol.ClassSymbol)symbols.objectType.tsym;
  }

  private void eraseGenericStructuralVarargs( JCTree.JCMethodInvocation tree )
  {
    if( tree.varargsElement instanceof Type.ClassType && TypesUtil.isStructuralInterface( _tp.getTypes(), tree.varargsElement.tsym ) )
    {
      tree.varargsElement = _tp.getSymtab().objectType;
    }
  }

  /**
   * Issue errors/warnings if an extension method violates extension method grammar or conflicts with an existing method
   */
  @Override
  public void visitMethodDef( JCTree.JCMethodDecl tree )
  {
    if( isBridgeMethod( tree ) )
    {
      // we process bridge methods during Generation, since they don't exist prior to Generation
      _bridgeMethod = true;
    }
    if( isLambdaMethod( tree ) )
    {
      _lambdaMethod = true;
    }

    try
    {
      if( !_tp.isGenerate() && !_bridgeMethod )
      {
        unproxyStructuralArgsIfDefaultMethod( tree );
      }

      super.visitMethodDef( tree );

      if( _lambdaMethod )
      {
        updateMethodSymbolParamTypes( tree );
      }
    }
    finally
    {
      _bridgeMethod = false;
      _lambdaMethod = false;
    }

    if( _tp.isGenerate() )
    {
      // Don't process tree during GENERATE, unless the tree was generated e.g., a bridge method
      return;
    }

    if( tree.sym.owner.isAnonymous() )
    {
      // Keep track of anonymous classes so we can process any bridge methods added to them
      JCTree.JCClassDecl anonymousClassDef = (JCTree.JCClassDecl)_tp.getTreeUtil().getTree( tree.sym.owner );
      _tp.preserveInnerClassForGenerationPhase( anonymousClassDef );
    }

    verifyExtensionMethod( tree );
    result = tree;
  }

  private void updateMethodSymbolParamTypes( JCTree.JCMethodDecl tree )
  {
    // !! todo: improve "lambda" check
    if( !tree.getName().toString().contains( "lambda" ) )
    {
      return;
    }

    List<JCTree.JCVariableDecl> params = tree.params;
    List<Symbol.VarSymbol> newSyms = List.nil();
    List<Type> newTypes = List.nil();
    for( int i = 0; i < params.size(); i++ )
    {
      JCTree.JCVariableDecl param = params.get(i);
      newSyms = newSyms.append( param.sym );
      newTypes = newTypes.append( param.type );
    }
    tree.sym.params = newSyms;
    ((Type.MethodType)tree.sym.type).argtypes = newTypes;
  }

  /**
   This is to handle the case where a structural interface has a default method and is called reflectively from a proxy
   and the method has at least one parameter that is also a structural interface. The problem with this case is that
   ReflectUtil.invokeDefault() calls deep inside methodHandleLand where the param is cast to the structural interface
   before calling the method, the cast of course fails because we *only* proxy the *receiver* at the call-site to make
   the structural call; to maintain object identity we never keep proxies alive beyond that, thus we don't pass them to
   methods. Note, the JVM does *not* type check arguments passed to methods.

   As a remedy we now "fake" proxy the arg to get past the cast; we generate code in the receiving method to unproxy
   the fake proxied argument before it used; again this all just to get around methodhandle calling code that casts the
   arg to the structural iface type before invoking it. A fake proxy is a normal Java proxy that is never called, it
   exists solely to make it past the cast.
  */
  private void unproxyStructuralArgsIfDefaultMethod( JCTree.JCMethodDecl tree )
  {
    if( tree.sym == null || !tree.sym.isDefault() )
    {
      return;
    }

    for( JCTree.JCVariableDecl param: tree.getParameters() )
    {
      if( TypesUtil.isStructuralInterface( _tp.getTypes(), param.type.tsym ) )
      {
        // default void myDefaultMethod( MyStructuralIface obj ) {
        //   generated --> obj = unFakeProxy( obj );
        //   ...
        // }

        TreeMaker make = TreeMaker.instance( _tp.getContext() );
        JavacElements javacElems = _tp.getElementUtil();
        Symtab symbols = _tp.getSymtab();

        int bodyPos = tree.getBody().pos;

        if( !param.type.isPrimitive() )
        {
          JCTree.JCIdent proxiedParam = make.Ident( param.sym );
          proxiedParam.pos = bodyPos;

          Names names = Names.instance( _tp.getContext() );
          Symbol.ClassSymbol runtimeMethodsClassSym = getRtClassSym( RuntimeMethods.class );

          Symbol.MethodSymbol unproxyMethod = resolveMethod( tree.pos(), names.fromString( "unFakeProxy" ), runtimeMethodsClassSym.type,
            List.from( new Type[]{symbols.objectType} ) );

          JCTree.JCMethodInvocation unproxyCall = make.Apply( List.nil(), 
            memberAccess( make, javacElems, RuntimeMethods.class.getTypeName() + ".unFakeProxy" ), List.of( proxiedParam ) );
          unproxyCall.setPos( bodyPos );
          unproxyCall.type = symbols.objectType;
          JCTree.JCFieldAccess methodSelect = (JCTree.JCFieldAccess)unproxyCall.getMethodSelect();
          methodSelect.sym = unproxyMethod;
          methodSelect.type = unproxyMethod.type;
          methodSelect.pos = bodyPos;
          assignTypes( methodSelect.selected, runtimeMethodsClassSym );
          methodSelect.selected.pos = bodyPos;

          JCTypeCast castedUnproxyCall = make.TypeCast( param.type, unproxyCall );
          castedUnproxyCall.pos = bodyPos;

          JCTree.JCIdent lhs = make.Ident( param.sym );
          lhs.pos = bodyPos;

          JCTree.JCAssign unproxy = make.Assign( lhs, castedUnproxyCall );
          unproxy.pos = bodyPos;
          unproxy.type = symbols.objectType;
          JCTree.JCExpressionStatement unproxyStmt = make.Exec( unproxy );
          unproxyStmt.pos = bodyPos;
          unproxyStmt.type = unproxy.type;

          JCTree.JCBlock body = tree.getBody();
          tree.body = make.Block( 0, List.of( unproxyStmt, body ) );
          tree.body.pos = bodyPos;
        }
      }
    }
  }

  private Symbol.ClassSymbol getRtClassSym( Class cls )
  {
    Symbol.ClassSymbol sym = IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), cls.getTypeName() );
    if( sym == null )
    {
      sym = JavacElements.instance( _tp.getContext() ).getTypeElement( cls.getTypeName() );
    }
    return sym;
  }

  private void checkExtensionClassError( JCTree.JCClassDecl typeDecl )
  {
    JavacPlugin javacPlugin = JavacPlugin.instance();
    if( javacPlugin == null )
    {
      return;
    }

    if( !isExtensionClass( typeDecl ) )
    {
      return;
    }

    String extendedFqn = getExtendedClassName();
    if( javacPlugin.getJavaInputFiles().stream().anyMatch(
      pair -> !(pair.getSecond() instanceof GeneratedJavaStubFileObject) && pair.getFirst().equals( extendedFqn ) ) )
    {
      _tp.report( typeDecl, Diagnostic.Kind.WARNING, ExtIssueMsg.MSG_CANNOT_EXTEND_SOURCE_FILE.get( extendedFqn ) );
    }
  }

  private void verifyExtensionMethod( JCTree.JCMethodDecl tree )
  {
//    if( JavacPlugin.instance() == null )
//    {
//      // don't perform verification at runtime, slow
//      return;
//    }

    if( !isExtensionClass( _tp.getParent( tree ) ) )
    {
      return;
    }

    String extendedClassName = getExtendedClassName();
    if( extendedClassName == null )
    {
      return;
    }

    boolean thisAnnoFound = false;
    List<JCTree.JCVariableDecl> parameters = tree.getParameters();
    for( int i = 0; i < parameters.size(); i++ )
    {
      JCTree.JCVariableDecl param = parameters.get( i );
      long methodModifiers = tree.getModifiers().flags;
      boolean This;
      if( (This = hasAnnotation( param.getModifiers().getAnnotations(), This.class )) ||
        hasAnnotation( param.getModifiers().getAnnotations(), ThisClass.class ) )
      {
        thisAnnoFound = true;

        if( i != 0 )
        {
          _tp.report( param, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_THIS_FIRST.get() );
        }

        if( This && extendedClassName.equals( Array.class.getTypeName() ) )
        {
          if( !param.type.tsym.getQualifiedName().toString().equals( Object.class.getName() ) )
          {
            // Array extensions must use `Object` as @This param to handle both primitive and reference arrays
            _tp.report( param, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_EXPECTING_OBJECT_FOR_THIS.get( Object.class.getSimpleName() ) );
          }
        }
        else if( This &&
          (!(param.type.tsym instanceof Symbol.ClassSymbol) ||
            !((Symbol.ClassSymbol)param.type.tsym).className().equals( extendedClassName )) )
        {
          Symbol.ClassSymbol extendClassSym = IDynamicJdk.instance().getTypeElement(
            _tp.getContext(), _tp.getCompilationUnit(), extendedClassName );
          if( extendClassSym != null &&
            !TypesUtil.isStructuralInterface( _tp.getTypes(), extendClassSym ) && // an extended class could be made a structural interface which results in Object as @This param, ignore this
            !TypeUtil.isAssignableFromErased( _tp.getContext(), extendClassSym, param.type.tsym ) &&
            (tree.sym.enclClass() == null || !param.type.tsym.isEnclosedBy( extendClassSym )) // handle inner class extension method
          )
          {
            _tp.report( param, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_EXPECTING_TYPE_FOR_THIS.get( extendedClassName ) );
          }
        }
        else if( !This &&
          (!(param.type.tsym instanceof Symbol.ClassSymbol) ||
            !((Symbol.ClassSymbol)param.type.tsym).className().equals( Class.class.getTypeName() )) )
        {
          // @ThisClass must have Class type
          _tp.report( param, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_EXPECTING_CLASS_TYPE_FOR_THISCLASS.get( extendedClassName ) );
        }
      }
      else if( i == 0 &&
        Modifier.isStatic( (int)methodModifiers ) &&
        Modifier.isPublic( (int)methodModifiers ) &&
        param.type.toString().equals( extendedClassName ) )
      {
        _tp.report( param, Diagnostic.Kind.WARNING, ExtIssueMsg.MSG_MAYBE_MISSING_THIS.get() );
      }
    }

    if( thisAnnoFound || hasAnnotation( tree.getModifiers().getAnnotations(), Extension.class ) )
    {
      long methodModifiers = tree.getModifiers().flags;
      if( !Modifier.isStatic( (int)methodModifiers ) )
      {
        _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_MUST_BE_STATIC.get( tree.getName() ) );
      }

      if( Modifier.isPrivate( (int)methodModifiers ) )
      {
        _tp.report( tree, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_MUST_NOT_BE_PRIVATE.get( tree.getName() ) );
      }
    }
  }

  private String getExtendedClassName()
  {
    String extendedClassName = _tp.getCompilationUnit().getPackageName().toString();
    int iExt = extendedClassName.indexOf( ExtensionManifold.EXTENSIONS_PACKAGE + '.' );
    if( iExt < 0 )
    {
      return null;
    }

    extendedClassName = extendedClassName.substring( iExt + ExtensionManifold.EXTENSIONS_PACKAGE.length() + 1 );
    return extendedClassName;
  }

  private boolean isExtensionClass( Tree parent )
  {
    if( parent instanceof JCTree.JCClassDecl )
    {
      return hasAnnotation( ((JCTree.JCClassDecl)parent).getModifiers().getAnnotations(), Extension.class );
    }
    return false;
  }

  public JCTree.JCClassDecl getEnclosingClass( Tree tree )
  {
    return getEnclosingClass( tree, child -> _tp.getParent( child ) );
  }
  public static JCTree.JCClassDecl getEnclosingClass( Tree tree, Function<Tree, Tree> parentOf )
  {
    if( tree == null )
    {
      return null;
    }
    if( tree instanceof JCTree.JCClassDecl )
    {
      return (JCTree.JCClassDecl)tree;
    }
    return getEnclosingClass( parentOf.apply( tree ), parentOf );
  }

  private Symbol getEnclosingSymbol( Tree tree, Context ctx )
  {
    return getEnclosingSymbol( tree, ctx, child -> _tp.getParent( child ) );
  }
  public static Symbol getEnclosingSymbol( Tree tree, Context ctx, Function<Tree,Tree> parentOf )
  {
    if( tree == null )
    {
      return null;
    }
    if( tree instanceof JCTree.JCClassDecl )
    {
      // should not really get here, but should be static block scope if possible
      return new Symbol.MethodSymbol( STATIC | BLOCK,
        Names.instance( ctx ).empty, null, ((JCTree.JCClassDecl)tree).sym );
    }
    if( tree instanceof JCTree.JCMethodDecl )
    {
      return ((JCTree.JCMethodDecl)tree).sym;
    }
    if( tree instanceof JCTree.JCVariableDecl )
    {
      Tree parent = parentOf.apply( tree );
      if( parent instanceof JCTree.JCClassDecl )
      {
        // field initializers have a block scope
        return new Symbol.MethodSymbol( (((JCTree.JCVariableDecl)tree).mods.flags & STATIC) | BLOCK,
          Names.instance( ctx ).empty, null, ((JCTree.JCClassDecl)parent).sym );
      }
    }
    return getEnclosingSymbol( parentOf.apply( tree ), ctx, parentOf );
  }

  private boolean hasAnnotation( List<JCTree.JCAnnotation> annotations, Class<? extends Annotation> annoClass )
  {
    for( JCTree.JCAnnotation anno : annotations )
    {
      if( anno.getAnnotationType().type.toString().equals( annoClass.getCanonicalName() ) )
      {
        return true;
      }
    }
    return false;
  }

  private JCTree.JCMethodInvocation replaceStructuralCall( JCTree.JCMethodInvocation theCall )
  {
    JCExpression methodSelect = theCall.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      int pos = theCall.pos;

      Symtab symbols = _tp.getSymtab();
      Names names = Names.instance( _tp.getContext() );
      Symbol.ClassSymbol reflectMethodClassSym = getRtClassSym( RuntimeMethods.class );
      Symbol.MethodSymbol makeInterfaceProxyMethod = resolveMethod( theCall.pos(), names.fromString( "constructProxy" ), reflectMethodClassSym.type,
        List.from( new Type[]{symbols.objectType, symbols.classType} ) );

      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      TreeMaker make = _tp.getTreeMaker();
      JavacElements javacElems = _tp.getElementUtil();
      JCExpression thisArg = m.selected;

      ArrayList<JCExpression> newArgs = new ArrayList<>();
      newArgs.add( thisArg );
      JCTree.JCFieldAccess ifaceClassExpr = (JCTree.JCFieldAccess)memberAccess( make, javacElems, thisArg.type.tsym.getQualifiedName().toString() + ".class" );
      ifaceClassExpr.type = symbols.classType;
      ifaceClassExpr.sym = symbols.classType.tsym;
      ifaceClassExpr.pos = pos;
      assignTypes( ifaceClassExpr.selected, thisArg.type.tsym );
      ifaceClassExpr.selected.pos = pos;
      newArgs.add( ifaceClassExpr );

      JCTree.JCMethodInvocation makeProxyCall = make.Apply( List.nil(), memberAccess( make, javacElems, RuntimeMethods.class.getName() + ".constructProxy" ), List.from( newArgs ) );
      makeProxyCall.setPos( pos );
      makeProxyCall.type = thisArg.type;
      JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)makeProxyCall.getMethodSelect();
      newMethodSelect.sym = makeInterfaceProxyMethod;
      newMethodSelect.type = makeInterfaceProxyMethod.type;
      newMethodSelect.pos = pos;
      assignTypes( newMethodSelect.selected, reflectMethodClassSym );
      newMethodSelect.selected.pos = pos;

      JCTypeCast cast = make.TypeCast( thisArg.type, makeProxyCall );
      cast.type = thisArg.type;
      cast.pos = pos;

      ((JCTree.JCFieldAccess)theCall.meth).selected = cast;

      theCall.pos = pos;

      return theCall;
    }
    return null;
  }

  private JCTypeCast makeCast( JCExpression expression, Type type )
  {
    TreeMaker make = _tp.getTreeMaker();

    JCTypeCast castCall = make.TypeCast( type, expression );
    castCall.type = type;
    castCall.pos = expression.pos;

    return castCall;
  }

  private JCTree.JCMethodInvocation replaceExtCall( JCTree.JCMethodInvocation tree, Symbol.MethodSymbol method, boolean isSmartStatic )
  {
    Symtab symTab = _tp.getSymtab();
    JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      boolean isStatic = m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
      TreeMaker make = _tp.getTreeMaker();
      JavacElements javacElems = _tp.getElementUtil();
      JCExpression thisArg = m.selected;
      String extensionFqn = method.getEnclosingElement().asType().tsym.toString();
      m.selected = memberAccess( make, javacElems, extensionFqn );
      BasicJavacTask javacTask = (BasicJavacTask)_tp.getJavacTask();
      Symbol.ClassSymbol extensionClassSym = ClassSymbols.instance( _sp.getModule() ).getClassSymbol( javacTask, _tp, extensionFqn ).getFirst();
      assignTypes( m.selected, extensionClassSym );
      m.selected.pos = tree.pos;
      m.sym = method;
      m.type = method.type;

      if( !isStatic )
      {
        ArrayList<JCExpression> newArgs = new ArrayList<>( tree.args );
        newArgs.add( 0, thisArg );
        tree.args = List.from( newArgs );
      }
      else if( isSmartStatic )
      {
        ArrayList<JCExpression> newArgs = new ArrayList<>( tree.args );
        JCTree.JCFieldAccess classExpr = (JCTree.JCFieldAccess)memberAccess( make, javacElems, thisArg.type.tsym.getQualifiedName().toString() + ".class" );
        classExpr.type = symTab.classType;
        classExpr.sym = symTab.classType.tsym;
        classExpr.pos = tree.pos;
        assignTypes( classExpr.selected, thisArg.type.tsym );
        classExpr.selected.pos = tree.pos;
        newArgs.add( 0, classExpr );
        tree.args = List.from( newArgs );
      }

      boolean isPublic = (method.flags_field & Flags.AccessFlags) == PUBLIC;
      if( !isPublic )
      {
        tree = replaceWithReflection( tree );
      }

      return tree;
    }
    else if( methodSelect instanceof JCTree.JCIdent )
    {
      JCTree.JCIdent m = (JCTree.JCIdent)methodSelect;
      boolean isStatic = m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
      TreeMaker make = _tp.getTreeMaker();
      JavacElements javacElems = _tp.getElementUtil();
      String extensionFqn = method.getEnclosingElement().asType().tsym.toString();

      ArrayList<JCExpression> newArgs = new ArrayList<>( tree.args );
      if( !isStatic )
      {
        JCExpression thisArg = make.This( _tp.getClassDecl( tree ).type );
        newArgs.add( 0, thisArg );
      }
      else if( isSmartStatic )
      {
        JCTree.JCClassDecl classDecl = _tp.getClassDecl( tree );
        JCTree.JCFieldAccess classExpr = (JCTree.JCFieldAccess)memberAccess( make, javacElems, classDecl.getSimpleName().toString() + ".class" );
        classExpr.type = symTab.classType;
        classExpr.sym = symTab.classType.tsym;
        classExpr.pos = tree.pos;
        assignTypes( classExpr.selected, classDecl.type.tsym );
        classExpr.selected.pos = tree.pos;
        newArgs.add( 0, classExpr );
      }
      JCTree.JCMethodInvocation extCall =
        make.Apply( List.nil(),
          memberAccess( make, javacElems, extensionFqn ),
          List.from( newArgs ) );
      extCall.setPos( tree.pos );
      extCall.type = tree.type;
      extCall.varargsElement = tree.varargsElement;
      JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)extCall.getMethodSelect();
      newMethodSelect.sym = method;
      newMethodSelect.type = method.type;
      newMethodSelect.pos = tree.pos;
      assignTypes( newMethodSelect.selected, method.owner );

      boolean isPublic = (method.flags_field & Flags.AccessFlags) == PUBLIC;
      if( !isPublic )
      {
        extCall = replaceWithReflection( extCall );
      }

      return extCall;
    }
    return tree;
  }

  private JCTree.JCMethodInvocation replaceWithReflection( JCTree.JCMethodInvocation tree )
  {
    //## todo: maybe try to avoid reflection if the method is accessible -- at least check if the method and its enclosing nest of classes are all public

    Type type = tree.getMethodSelect().type;
    if( type instanceof Type.ErrorType )
    {
      // No such field/method or wrong params
      return tree;
    }

    TreeMaker make = _tp.getTreeMaker();
    JavacElements javacElems = _tp.getElementUtil();

    JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      boolean isStatic = m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
      if( !(m.sym instanceof Symbol.MethodSymbol) )
      {
        return tree;
      }
      Type returnType = ((Symbol.MethodSymbol)m.sym).getReturnType();
      Symbol.MethodSymbol reflectMethodSym = findReflectUtilMethod( tree, returnType, isStatic );

      List<Symbol.VarSymbol> parameters = ((Symbol.MethodSymbol)m.sym).getParameters();
      ArrayList<JCExpression> paramTypes = new ArrayList<>();
      for( Symbol.VarSymbol param : parameters )
      {
        JCExpression classExpr = makeClassExpr( tree, _tp.getTypes().erasure( param.type ) );
        paramTypes.add( classExpr );
      }
      Symtab symTab = _tp.getSymtab();
      JCTree.JCNewArray paramTypesArray = make.NewArray(
        make.Type( symTab.classType ), List.nil(), List.from( paramTypes ) );
      paramTypesArray.type = new Type.ArrayType( symTab.classType, symTab.arrayClass );

      JCTree.JCNewArray argsArray = make.NewArray(
        make.Type( symTab.objectType ), List.nil(), tree.getArguments() );
      argsArray.type = new Type.ArrayType( symTab.objectType, symTab.arrayClass );

      ArrayList<JCExpression> newArgs = new ArrayList<>();
      newArgs.add( isStatic ? makeClassExpr( tree, m.selected.type ) : m.selected ); // receiver or class
      newArgs.add( make.Literal( m.sym.flatName().toString() ) ); // method name
      newArgs.add( paramTypesArray ); // param types
      newArgs.add( argsArray ); // args

      Symbol.ClassSymbol reflectMethodClassSym =
        IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

      JCTree.JCMethodInvocation reflectCall =
        make.Apply( List.nil(),
          memberAccess( make, javacElems, ReflectionRuntimeMethods.class.getName() + "." + reflectMethodSym.flatName().toString() ),
          List.from( newArgs ) );
      reflectCall.setPos( tree.pos );
      reflectCall.type = returnType;
      reflectCall.pos = tree.pos;
      JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)reflectCall.getMethodSelect();
      newMethodSelect.sym = reflectMethodSym;
      newMethodSelect.type = reflectMethodSym.type;
      assignTypes( newMethodSelect.selected, reflectMethodClassSym );
      newMethodSelect.pos = tree.pos;
      return reflectCall;
    }
    return tree;
  }

  private JCTree replaceWithReflection( JCTree.JCFieldAccess tree )
  {
    TreeMaker make = _tp.getTreeMaker();
    JavacElements javacElems = _tp.getElementUtil();

    boolean isStatic = tree.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
    if( tree.sym instanceof Symbol.MethodSymbol )
    {
      return tree;
    }

    Tree parent = _tp.getParent( tree );
    if( parent instanceof JCTree.JCAssign && ((JCTree.JCAssign)parent).lhs == tree ||
      parent instanceof JCTree.JCAssignOp && ((JCTree.JCAssignOp)parent).lhs == tree )
    {
      // handled in visitAssign() or visitAssignOp()
      return tree;
    }

    if( parent instanceof JCTree.JCUnary && ((JCTree.JCUnary)parent).arg == tree )
    {
      Tree.Kind kind = parent.getKind();

      if( kind != Tree.Kind.UNARY_MINUS && kind != Tree.Kind.UNARY_PLUS &&
        kind != Tree.Kind.LOGICAL_COMPLEMENT && kind != Tree.Kind.BITWISE_COMPLEMENT )
      {
        // supporting -, +, !, ~  not supporting --, ++
        _tp.report( (JCTree)parent, Diagnostic.Kind.ERROR, ExtIssueMsg.MSG_INCREMENT_OP_NOT_ALLOWED_REFLECTION.get() );
        return tree;
      }
    }

    Type type = tree.sym.type;
    if( type instanceof Type.ErrorType )
    {
      // No such field/method
      return tree;
    }

    Symbol.MethodSymbol reflectMethodSym = findFieldAccessReflectUtilMethod( tree, type, isStatic, false );

    ArrayList<JCExpression> newArgs = new ArrayList<>();
    newArgs.add( isStatic ? makeClassExpr( tree, tree.selected.type ) : tree.selected ); // receiver or class
    newArgs.add( make.Literal( tree.sym.flatName().toString() ) ); // field name

    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    JCTree.JCMethodInvocation reflectCall =
      make.Apply( List.nil(),
        memberAccess( make, javacElems, ReflectionRuntimeMethods.class.getName() + "." + reflectMethodSym.flatName().toString() ),
        List.from( newArgs ) );
    reflectCall.setPos( tree.pos );
    reflectCall.type = type;
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)reflectCall.getMethodSelect();
    newMethodSelect.sym = reflectMethodSym;
    newMethodSelect.type = reflectMethodSym.type;
    assignTypes( newMethodSelect.selected, reflectMethodClassSym );

    return reflectCall;
  }

  private JCTree replaceWithReflection( JCTree.JCAssign assignTree )
  {
    JCTree.JCFieldAccess tree = (JCTree.JCFieldAccess)assignTree.lhs;

    TreeMaker make = _tp.getTreeMaker();
    JavacElements javacElems = _tp.getElementUtil();

    boolean isStatic = tree.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC );
    if( tree.sym instanceof Symbol.MethodSymbol )
    {
      return assignTree;
    }

    Type type = tree.sym.type;
    Symbol.MethodSymbol reflectMethodSym = findFieldAccessReflectUtilMethod( tree, type, isStatic, true );

    ArrayList<JCExpression> newArgs = new ArrayList<>();
    newArgs.add( isStatic ? makeClassExpr( tree, tree.selected.type ) : tree.selected ); // receiver or class
    newArgs.add( make.Literal( tree.sym.flatName().toString() ) ); // field name
    newArgs.add( assignTree.rhs ); // field value

    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    JCTree.JCMethodInvocation reflectCall =
      make.Apply( List.nil(),
        memberAccess( make, javacElems, ReflectionRuntimeMethods.class.getName() + "." + reflectMethodSym.flatName().toString() ),
        List.from( newArgs ) );
    reflectCall.setPos( tree.pos );
    reflectCall.type = type;
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)reflectCall.getMethodSelect();
    newMethodSelect.sym = reflectMethodSym;
    newMethodSelect.type = reflectMethodSym.type;
    assignTypes( newMethodSelect.selected, reflectMethodClassSym );

    return reflectCall;
  }

  private JCTree replaceWithReflection( JCTree.JCNewClass tree )
  {
    if( tree.constructor == null )
    {
      return tree;
    }

    TreeMaker make = _tp.getTreeMaker();
    JavacElements javacElems = _tp.getElementUtil();

    Type type = ((JCTree.JCAnnotatedType)tree.clazz).underlyingType.type;

    if( tree.constructor instanceof Symbol.ClassSymbol )
    {
      //assert tree.constructor.kind == com.sun.tools.javac.code.Kinds.ERR;
      return tree;
    }

    List<Symbol.VarSymbol> parameters = ((Symbol.MethodSymbol)tree.constructor).getParameters();
    ArrayList<JCExpression> paramTypes = new ArrayList<>();
    for( Symbol.VarSymbol param : parameters )
    {
      paramTypes.add( makeClassExpr( tree, _tp.getTypes().erasure( param.type ) ) );
    }
    Symtab symTab = _tp.getSymtab();
    JCTree.JCNewArray paramTypesArray = make.NewArray(
      make.Type( symTab.classType ), List.nil(), List.from( paramTypes ) );
    paramTypesArray.type = new Type.ArrayType( symTab.classType, symTab.arrayClass );

    JCTree.JCNewArray argsArray = make.NewArray(
      make.Type( symTab.objectType ), List.nil(), tree.getArguments() );
    argsArray.type = new Type.ArrayType( symTab.objectType, symTab.arrayClass );

    ArrayList<JCExpression> newArgs = new ArrayList<>();
    newArgs.add( makeClassExpr( tree, type ) ); // the class
    newArgs.add( paramTypesArray ); // param types
    newArgs.add( argsArray ); // args


    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    Symbol.MethodSymbol reflectMethodSym = findReflectUtilConstructor( tree );
    JCTree.JCMethodInvocation reflectCall =
      make.Apply( List.nil(),
        memberAccess( make, javacElems, ReflectionRuntimeMethods.class.getName() + "." + reflectMethodSym.flatName().toString() ),
        List.from( newArgs ) );
    reflectCall.setPos( tree.pos );
    reflectCall.type = type;
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)reflectCall.getMethodSelect();
    newMethodSelect.sym = reflectMethodSym;
    newMethodSelect.type = reflectMethodSym.type;
    assignTypes( newMethodSelect.selected, reflectMethodClassSym );

    return reflectCall;
  }

  private JCExpression makeClassExpr( JCTree tree, Type type )
  {
    BasicJavacTask javacTask = (BasicJavacTask)_tp.getJavacTask();
    Types types = Types.instance( javacTask.getContext() );
    type = types.erasure( type );

    JCExpression classExpr;
    if( isPrimitiveOrPrimitiveArray( type ) ||
      (JreUtil.isJava8() && type.tsym.getModifiers().contains( javax.lang.model.element.Modifier.PUBLIC )) )
    {
      // class is publicly accessible, assume we can use class literal
      classExpr = _tp.getTreeMaker().ClassLiteral( type );
      classExpr.pos = tree.pos;
    }
    else
    {
      // generate `ReflectUtil.type( typeName )`
      classExpr = classForNameCall( type, tree );
    }
    return classExpr;
  }

  private boolean isPrimitiveOrPrimitiveArray( Type type )
  {
    while( type instanceof Type.ArrayType )
    {
      type = ((Type.ArrayType)type).getComponentType();
    }
    return type.isPrimitive();
  }

  private JCExpression classForNameCall( Type type, JCTree tree )
  {
    TreeMaker make = _tp.getTreeMaker();
    JavacElements javacElems = _tp.getElementUtil();

    JCTree.JCMethodInvocation typeCall = make.Apply( List.nil(),
      memberAccess( make, javacElems, ReflectUtil.class.getName() + ".type" ),
      List.of( make.Literal( makeLiteralName( type ) ) ) );
    typeCall.setPos( Position.NOPOS );
    typeCall.type = _tp.getSymtab().classType;
    JCTree.JCFieldAccess newMethodSelect = (JCTree.JCFieldAccess)typeCall.getMethodSelect();

    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectUtil.class.getName() );
    Symbol.MethodSymbol typeMethodSymbol = resolveMethod( tree.pos(),
      Names.instance( _tp.getContext() ).fromString( "type" ),
      reflectMethodClassSym.type, List.of( _tp.getSymtab().stringType ) );
    newMethodSelect.sym = typeMethodSymbol;
    newMethodSelect.type = typeMethodSymbol.type;
    newMethodSelect.pos = tree.pos;
    assignTypes( newMethodSelect.selected, reflectMethodClassSym );

    return typeCall;
  }

  private String makeLiteralName( Type type )
  {
    StringBuilder sb = new StringBuilder();
    for( ; type instanceof Type.ArrayType; type = ((Type.ArrayType)type).getComponentType() )
    {
      sb.append( "[]" );
    }
    return type.tsym.flatName() + sb.toString();
  }

  private void assignTypes( JCExpression m, Symbol symbol )
  {
    if( m instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess)m;
      fieldAccess.sym = symbol;
      fieldAccess.type = symbol.type;
      assignTypes( fieldAccess.selected, symbol.owner );
    }
    else if( m instanceof JCTree.JCIdent )
    {
      JCTree.JCIdent ident = (JCTree.JCIdent)m;
      ident.sym = symbol;
      ident.type = symbol.type;
    }
  }

  private Symbol.MethodSymbol findExtMethod( JCTree.JCMethodInvocation tree, boolean[] isSmartStatic )
  {
    Symbol sym = null;
    if( tree.meth instanceof JCTree.JCFieldAccess )
    {
      sym = ((JCTree.JCFieldAccess)tree.meth).sym;
    }
    else if( tree.meth instanceof JCTree.JCIdent )
    {
      sym = ((JCTree.JCIdent)tree.meth).sym;
    }

    if( sym == null )
    {
      return null;
    }

    if( !sym.hasAnnotations() )
    {
      sym = sym.baseSymbol();
      if( sym == null || !sym.hasAnnotations() )
      {
        return null;
      }
    }

    for( Attribute.Compound annotation : sym.getAnnotationMirrors() )
    {
      if( annotation.type.toString().equals( ExtensionMethod.class.getName() ) )
      {
        String extensionClass = (String)annotation.values.get( 0 ).snd.getValue();
        boolean isStatic = (boolean)annotation.values.get( 1 ).snd.getValue();
        isSmartStatic[0] = (boolean)annotation.values.get( 2 ).snd.getValue();
        boolean isIntercept = (boolean)annotation.values.get( 3 ).snd.getValue();
        boolean isExtensionSource = (boolean) annotation.values.get( 4 ).snd.getValue();

        if( isIntercept && !shouldInterceptMethod( tree, sym, isExtensionSource ) )
        {
          // Do not replace an intercepted method call in the method declaration of the intercepted method
          return null;
        }
        BasicJavacTask javacTask = (BasicJavacTask)_tp.getJavacTask(); //JavacHook.instance() != null ? (JavacTaskImpl)JavacHook.instance().getJavacTask_PlainFileMgr() : ClassSymbols.instance( _sp.getModule() ).getJavacTask_PlainFileMgr();
        Pair<Symbol.ClassSymbol, JCTree.JCCompilationUnit> classSymbol = ClassSymbols.instance( _sp.getModule() ).getClassSymbol( javacTask, _tp, extensionClass );
        if( classSymbol == null )
        {
          // In module mode if a package in another module is not exported, classes in the package
          // will not be accessible to other modules, hence the null classSymbol
          continue;
        }

        Symbol.ClassSymbol extClassSym = classSymbol.getFirst();
        if( extClassSym == null )
        {
          // This can happen during bootstrapping with Dark Java classes from Manifold itself
          // So we short-circuit that here (ManClassFinder_9 or any other darkj class used during bootstrapping doesn't really need to use extensions)
          return null;
        }
        Types types = Types.instance( javacTask.getContext() );
        outer:
        for( Symbol elem : IDynamicJdk.instance().getMembers( extClassSym ) )
        {
          if( elem instanceof Symbol.MethodSymbol && elem.flatName().toString().equals( sym.name.toString() ) )
          {
            Symbol.MethodSymbol extMethodSym = (Symbol.MethodSymbol)elem;
            List<Symbol.VarSymbol> extParams = extMethodSym.getParameters();
            List<Symbol.VarSymbol> calledParams = ((Symbol.MethodSymbol)sym).getParameters();
            int thisOffset = isStatic && !isSmartStatic[0] && !isExtensionSource ? 0 : 1;
            if( extParams.size() - thisOffset != calledParams.size() )
            {
              continue;
            }
            for( int i = thisOffset; i < extParams.size(); i++ )
            {
              Symbol.VarSymbol extParam = extParams.get( i );
              Symbol.VarSymbol calledParam = calledParams.get( i - thisOffset );
              if( !types.isSameType( types.erasure( extParam.type ), types.erasure( calledParam.type ) ) )
              {
                continue outer;
              }
            }
            return extMethodSym;
          }
        }
      }
    }
    return null;
  }

  /**
   * Determines whether a method invocation should be intercepted. A method should not be intercepted if
   * it is invoked from within the body of the method being intercepted (i.e., the invocation is part of
   * the method’s own execution).
   *
   * @param tree The method invocation represented as a {@link JCTree.JCMethodInvocation} tree.
   * @param sym The symbol representing the method being called. This is the method to check for interception.
   * @param isExtensionSource A flag indicating whether the source of the method is an `ExtensionSource`.
   *                           If true, special handling is applied to the first parameter (which isn't annotated).
   *
   * @return true if the method should be intercepted; false otherwise.
   */
  private boolean shouldInterceptMethod( JCTree.JCMethodInvocation tree, Symbol sym, boolean isExtensionSource )
  {
    Tree parent = _tp.getParent( tree );
    // Traverse up the tree to find the method declaration
    while( !( parent instanceof JCTree.JCMethodDecl ) )
    {
      parent = _tp.getParent( parent );
    }
    JCTree.JCMethodDecl methodDecl = (JCTree.JCMethodDecl) parent;
    // If the method name doesn't match, it means we're not calling the same method, so it can be intercepted
    if( !methodDecl.name.toString().equals( sym.name.toString() ) )
    {
      return true;
    }
    // Determine how many parameters to subtract for the comparison
    int paramsToSubtract = 0;
    if( !methodDecl.params.isEmpty() )
    {
      if( isExtensionSource )
      {
        // Source of the method is an ExtensionSource, first parameter isn't annotated, but should be ignored
        paramsToSubtract = 1;
      }
      else
      {
        List<JCAnnotation> annotations = methodDecl.getParameters().get( 0 ).getModifiers().getAnnotations();
        if( !annotations.isEmpty() )
        {
          String annotationFqn = annotations.get( 0 ).getAnnotationType().type.toString();
          if( This.class.getTypeName().equals( annotationFqn ) || ThisClass.class.getTypeName().equals( annotationFqn ) )
          {
            // If the method's first parameter is annotated with @This or @ThisClass, it should be ignored.
            paramsToSubtract = 1;
          }
        }
      }
    }

    if( paramsToSubtract != 0 )
    {
      String objectName = methodDecl.params.get( 0 ).name.toString();
      if( !tree.toString().split( "\\.", 2 )[0].equals( objectName ) )
      {
        // The method does not call its parent. The current object name differs from the parameters name.
        return true;
      }
    }
    // Ensure that the number of arguments matches the number of parameters minus any ignored ones
    if( methodDecl.params.length() - paramsToSubtract != tree.args.length() )
    {
      // If the argument count doesn't match the parameter count, it can be intercepted
      return true;
    }
    // Compare the argument types to the method parameter types (ignoring the first parameter if needed)
    for( int i = 1; i < methodDecl.params.length(); i++ )
    {
      if( !methodDecl.params.get( i ).type.tsym.equals( tree.args.get( i - paramsToSubtract ).type.tsym ) )
      {
        // If the argument type doesn't match the parameter type, it can be intercepted
        return true;
      }
    }
    // If all checks pass, the method is calling itself and should not be intercepted
    return false;
  }

  //## todo: cache these
  private Symbol.MethodSymbol findReflectUtilMethod( JCTree tree, Type returnType, boolean isStatic )
  {
    String name = "invoke" + (isStatic ? "Static" : "") + '_' + typeForReflect( returnType );

    Symtab symtab = _tp.getSymtab();
    Type.ArrayType classArrayType = new Type.ArrayType( symtab.classType, symtab.arrayClass );
    Type.ArrayType objectArrayType = new Type.ArrayType( symtab.objectType, symtab.arrayClass );
    List<Type> paramTypes;
    if( isStatic )
    {
      paramTypes = List.of( symtab.classType, symtab.stringType, classArrayType, objectArrayType );
    }
    else
    {
      paramTypes = List.of( symtab.objectType, symtab.stringType, classArrayType, objectArrayType );
    }

    Names names = Names.instance( _tp.getContext() );
    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    return resolveMethod( tree.pos(), names.fromString( name ), reflectMethodClassSym.type, paramTypes );
  }

  //## todo: cache these
  private Symbol.MethodSymbol findFieldAccessReflectUtilMethod( JCTree tree, Type type, boolean isStatic, boolean setter )
  {
    String name = (setter ? "set" : "get") + "Field" + (isStatic ? "Static" : "") + '_' + typeForReflect( type );

    Symtab symtab = _tp.getSymtab();
    List<Type> paramTypes;
    if( setter )
    {
      paramTypes = List.of( isStatic ? symtab.classType : symtab.objectType, symtab.stringType, type );
    }
    else
    {
      paramTypes = List.of( isStatic ? symtab.classType : symtab.objectType, symtab.stringType );
    }

    Names names = Names.instance( _tp.getContext() );
    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    return resolveMethod( tree.pos(), names.fromString( name ), reflectMethodClassSym.type, paramTypes );
  }

  //## todo: cache this
  private Symbol.MethodSymbol findReflectUtilConstructor( JCTree.JCNewClass tree )
  {
    Symtab symtab = _tp.getSymtab();
    Type.ArrayType classArrayType = new Type.ArrayType( symtab.classType, symtab.arrayClass );
    Type.ArrayType objectArrayType = new Type.ArrayType( symtab.objectType, symtab.arrayClass );
    List<Type> paramTypes = List.of( symtab.classType, classArrayType, objectArrayType );

    Symbol.ClassSymbol reflectMethodClassSym =
      IDynamicJdk.instance().getTypeElement( _tp.getContext(), _tp.getCompilationUnit(), ReflectionRuntimeMethods.class.getName() );

    Names names = Names.instance( _tp.getContext() );
    return resolveMethod( tree.pos(), names.fromString( "construct" ), reflectMethodClassSym.type, paramTypes );
  }

  private String typeForReflect( Type returnType )
  {
    if( returnType.isPrimitive() )
    {
      return returnType.tsym.getSimpleName().toString();
    }
    return "Object";
  }

  private boolean isStructuralMethod( JCTree.JCMethodInvocation tree )
  {
    JCExpression methodSelect = tree.getMethodSelect();
    if( methodSelect instanceof JCTree.JCFieldAccess )
    {
      JCTree.JCFieldAccess m = (JCTree.JCFieldAccess)methodSelect;
      if( m.sym != null && !m.sym.getModifiers().contains( javax.lang.model.element.Modifier.STATIC ) )
      {
        if( !isObjectMethod( m.sym ) )
        {
          JCExpression thisArg = m.selected;
          return TypesUtil.isStructuralInterface( _tp.getTypes(), thisArg.type.tsym );
        }
      }
    }
    return false;
  }

  private boolean isObjectMethod( Symbol sym )
  {
    return sym.owner != null && sym.owner.type == _tp.getSymtab().objectType;
  }

  public static JCExpression memberAccess( TreeMaker make, JavacElements javacElems, String path )
  {
    return memberAccess( make, javacElems, path.split( "\\." ) );
  }

  public static JCExpression memberAccess( TreeMaker make, JavacElements node, String... components )
  {
    JCExpression expr = make.Ident( node.getName( components[0] ) );
    for( int i = 1; i < components.length; i++ )
    {
      expr = make.Select( expr, node.getName( components[i] ) );
    }
    return expr;
  }

  public Symbol.MethodSymbol resolveMethod( JCDiagnostic.DiagnosticPosition pos, Name name, Type qual, List<Type> args )
  {
    return resolveMethod( _tp.getContext(), _tp.getCompilationUnit(), pos, name, qual, args );
  }
  public static Symbol.MethodSymbol resolveMethod( Context context, CompilationUnitTree compUnit, JCDiagnostic.DiagnosticPosition pos, Name name, Type qual, List<Type> args )
  {
    return resolveMethod( pos, context, (JCTree.JCCompilationUnit)compUnit, name, qual, args );
  }

  private static Symbol.MethodSymbol resolveMethod( JCDiagnostic.DiagnosticPosition pos, Context ctx, JCTree.JCCompilationUnit compUnit, Name name, Type qual, List<Type> args )
  {
    Resolve rs = Resolve.instance( ctx );
    AttrContext attrContext = new AttrContext();
    Env<AttrContext> env = new AttrContextEnv( pos.getTree(), attrContext );
    env.toplevel = compUnit;
    return rs.resolveInternalMethod( pos, env, qual, name, args, null );
  }

  private Symbol.VarSymbol resolveField( JCDiagnostic.DiagnosticPosition pos, Context ctx, Name name, Type qual )
  {
    Resolve rs = Resolve.instance( ctx );
    AttrContext attrContext = new AttrContext();
    Env<AttrContext> env = new AttrContextEnv( pos.getTree(), attrContext );
    env.toplevel = (JCTree.JCCompilationUnit)_tp.getCompilationUnit();
    return rs.resolveInternalField( pos, env, qual, name );
  }
}
