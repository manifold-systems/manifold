package manifold.internal.javac;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;

import javax.tools.Diagnostic;
import java.util.Iterator;

public class MethodRefToLambda
{

  private MethodRefToLambda()
  {
    // hide utility class constructor
  }

  /**
   * Converts a JCMemberReference (method reference) to a JCLambda (lambda expression).
   *
   * @param methodRef The JCMemberReference to convert.
   *
   * @return A JCLambda representing the lambda equivalent of the method reference
   */
  public static JCTree.JCLambda convert( TypeProcessor tp, JCTree.JCMemberReference methodRef )
  {
    try
    {
      TreeMaker make = tp.getTreeMaker();

      List<JCTree.JCVariableDecl> params = createParams( tp, methodRef );

      JCTree.JCExpression body = createBody( tp, methodRef, params );

      JCTree.JCLambda lambda = make.Lambda( params, body );
      lambda.setPos( methodRef.pos );

      lambda.type = createReturnType( methodRef, params );
      IDynamicJdk.instance().setTargets( lambda, List.of(  lambda.type ) );
      return lambda;
    } catch( Throwable e )
    {
      String message = String.format( "Error while converting method ref [%s] to lambda: %n%s ", methodRef, e );
      tp.report( methodRef, Diagnostic.Kind.ERROR, message );
      throw new IllegalStateException( message );
    }
  }

  private static List<JCTree.JCVariableDecl> createParams( TypeProcessor tp, JCTree.JCMemberReference methodRef )
  {
    TreeMaker make = tp.getTreeMaker();
    Symbol sym = methodRef.sym;
    boolean thisCall = "this".equals( methodRef.expr.toString() );
    List<JCTree.JCVariableDecl> params = List.nil();

    int idx = 0;
    if( methodRef.kind == JCTree.JCMemberReference.ReferenceKind.UNBOUND && !thisCall )
    {
      params = params.append( createParam( make, sym, methodRef.expr.type, idx++ ) );
    }
    for( Type paramType : sym.type.getParameterTypes() )
    {
      params = params.append( createParam( make, sym, paramType, idx++ ) );
    }
    return params;
  }

  private static JCTree.JCVariableDecl createParam( TreeMaker make, Symbol sym, Type paramType, int idx )
  {
    JCTree.JCVariableDecl param = make.VarDef(
      make.Modifiers( Flags.PARAMETER ),
      make.paramName( idx ),
      make.Type( paramType ),
      null
    );
    param.sym = new Symbol.VarSymbol( 0, param.name, paramType, sym );
    param.type = param.sym.type;
    return param;
  }

  private static JCTree.JCExpression createBody( TypeProcessor tp, JCTree.JCMemberReference methodRef,
    List<JCTree.JCVariableDecl> params )
  {
    TreeMaker make = tp.getTreeMaker();
    Symbol sym = methodRef.sym;
    List<JCTree.JCExpression> args = List.nil();
    for( JCTree.JCVariableDecl param : params )
    {
      args = args.append( make.Ident( param ) );
    }

    switch( methodRef.kind )
    {
      case IMPLICIT_INNER:
      case BOUND:
        return make.Apply(
          List.nil(),
          IDynamicJdk.instance().Select( make, methodRef.expr, sym ),
          args
        ).setType( methodRef.sym.type.getReturnType() );
      case UNBOUND:
        return make.Apply(
          List.nil(),
          IDynamicJdk.instance().Select( make, make.Ident( params.head ), sym ),
          args.tail
        ).setType( methodRef.sym.type.getReturnType() );
      case SUPER:
        return make.Apply(
          List.nil(),
          IDynamicJdk.instance().Select( make, make.Super( sym.owner.type, sym.owner.type.tsym ), sym ),
          args
        ).setType( methodRef.sym.type.getReturnType() );
      case STATIC:
        return make.Apply(
          List.nil(),
          IDynamicJdk.instance().Select( make, make.Ident( sym.owner ), sym ),
          args
        ).setType( methodRef.sym.type.getReturnType() );
      case ARRAY_CTOR:
        return make.NewArray(
          ( (JCTree.JCArrayTypeTree) methodRef.expr ).elemtype, // Element type (null for inferred type)
          args, // Dimensions (empty for array initializer)
          null // List of array elements
        ).setType( methodRef.sym.type.getReturnType() );
      case TOPLEVEL:
        if( sym.isConstructor() )
        {
          JCTree.JCNewClass newClass = make.NewClass(
            null,
            List.nil(),
            make.Ident( sym.owner ),
            args,
            null
          );
          newClass.constructor = sym;
          newClass.type = methodRef.expr.type;
          return newClass;
        }
        throw new IllegalStateException( "Not a constructor: " + sym );
      default:
        throw new IllegalArgumentException( "Unsupported member reference kind: " + methodRef.kind );
    }
  }

  private static Type createReturnType( JCTree.JCMemberReference methodRef, List<JCTree.JCVariableDecl> params )
  {
    boolean thisCall = "this".equals( methodRef.expr.toString() );
    if( methodRef.kind != JCTree.JCMemberReference.ReferenceKind.UNBOUND || thisCall )
    {
      return methodRef.type;
    }
    // Use parameter types for arguments, replacing some problematic types, such as IntersectionClassType
    List<Type> typeArgs = List.nil();
    Iterator<JCTree.JCVariableDecl> paramsIter = params.iterator();
    Iterator<Type> typeArgumentsIter = methodRef.type.getTypeArguments().iterator();
    while( paramsIter.hasNext() )
    {
      Type param = paramsIter.next().type;
      Type typeArgument = typeArgumentsIter.next();
      typeArgs = typeArgs.append( param.isPrimitive() && !typeArgument.isPrimitive() ? typeArgument : param );
    }
    if( typeArgumentsIter.hasNext() )
    {
      typeArgs = typeArgs.append( typeArgumentsIter.next() );
    }

    return new Type.ClassType(
      methodRef.type.getEnclosingType(),
      typeArgs,
      methodRef.type.tsym
    );
  }

}
