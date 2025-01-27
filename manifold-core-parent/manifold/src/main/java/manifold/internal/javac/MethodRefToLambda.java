package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import javax.tools.Diagnostic;

import static com.sun.tools.javac.code.Flags.FINAL;

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
      Symbol.MethodSymbol sym = (Symbol.MethodSymbol) methodRef.sym;
      TreeMaker make = tp.getTreeMaker();

      List<JCTree.JCVariableDecl> params = createParams( tp, methodRef );

      JCTree.JCExpression body = createBody( tp, methodRef, params );

      if( sym.params == null )
      {
        // set type to null to prevent mismatched types (e.g. Collection<String> instead of Set<String>)
        params.forEach( varDecl -> varDecl.vartype = null );
      }
      JCTree.JCLambda lambda = make.Lambda( params, body );
      lambda.setPos( methodRef.pos );
      lambda.type = methodRef.type;
      IDynamicJdk.instance().setTargets(lambda, List.of( methodRef.type ) );
      return lambda;
    }
    catch( Throwable e )
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
      params = params.append( createParam( make, sym, methodRef.sym.owner.type, idx++ ) );
    }
    for( Type paramType : sym.type.getParameterTypes() )
    {
      params = params.append( createParam( make, sym, paramType, idx++ ) );
    }
    return params;
  }

  private static JCTree.JCVariableDecl createParam( TreeMaker make, Symbol sym, Type paramType, int idx )
  {
    Name paramName = make.paramName( idx );
    JCTree.JCVariableDecl param = make.VarDef(
      make.Modifiers( 0 ),
      paramName,
      make.Type( paramType ),
      null
    );
    param.sym = new Symbol.VarSymbol( FINAL, param.name, paramType, sym );
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
        return setTypes( make.Apply(
          List.nil(),
          IDynamicJdk.instance().Select( make,  methodRef.expr, sym ),
          args
        ), methodRef );
      case UNBOUND:
        return setTypes( make.Apply(
          List.nil(),
          IDynamicJdk.instance().Select( make,  make.Ident( params.head ), sym ),
          args.tail
        ), methodRef );
      case SUPER:
        return setTypes( make.Apply(
          List.nil(),
          IDynamicJdk.instance().Select( make,  make.Super( sym.owner.type, sym.owner.type.tsym ), sym ),
          args
        ), methodRef );
      case STATIC:
        return setTypes( make.Apply(
          List.nil(),
          IDynamicJdk.instance().Select( make, make.Ident( sym.owner ), sym ),
          args
        ), methodRef );
      case ARRAY_CTOR:
        JCTree.JCNewArray newArray = make.NewArray(
          ( (JCTree.JCArrayTypeTree) methodRef.expr ).elemtype, // Element type (null for inferred type)
          args, // Dimensions (empty for array initializer)
          null // List of array elements
        );
        newArray.type = ( (Type.MethodType) methodRef.sym.type ).restype;
        return newArray;
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

  private static JCTree.JCMethodInvocation setTypes( JCTree.JCMethodInvocation method, JCTree.JCMemberReference methodRef )
  {
    method.type = ( (Type.MethodType) methodRef.sym.type ).restype;
    return method;
  }
}
