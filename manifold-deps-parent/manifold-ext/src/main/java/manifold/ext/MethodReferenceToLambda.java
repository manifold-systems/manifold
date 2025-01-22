package manifold.ext;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.*;

import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.sun.tools.javac.code.Flags.FINAL;

public class MethodReferenceToLambda
{

  private final TreeMaker make;
  private final Names names;

  public MethodReferenceToLambda( Context context, TreeMaker make )
  {
    this.make = make;
    this.names = Names.instance( context );
  }

  /**
   * Converts a JCMemberReference (method reference) to a JCLambda (lambda expression).
   *
   * @param methodRef The JCMemberReference to convert.
   *
   * @return A JCLambda representing the lambda equivalent of the method reference.
   */
  public JCTree.JCLambda convert( JCTree.JCMemberReference methodRef )
  {
    return convert( methodRef, UnaryOperator.identity() );
  }

  /**
   * Converts a JCMemberReference (method reference) to a JCLambda (lambda expression).
   *
   * @param methodRef The JCMemberReference to convert.
   * @param methodTransformer the function to apply on the created method call
   *
   * @return A JCLambda representing the lambda equivalent of the method reference.
   */
  public JCTree.JCLambda convert( JCTree.JCMemberReference methodRef, UnaryOperator<JCTree.JCMethodInvocation> methodTransformer )
  {
    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) methodRef.sym;  // The method being referenced
    return convert( methodRef, methodSymbol, methodTransformer );
  }

  /**
   * Converts a JCMemberReference (method reference) to a JCLambda (lambda expression).
   *
   * @param methodRef The JCMemberReference to convert.
   * @param methodTransformer the function to apply on the created method call
   *
   * @return A JCLambda representing the lambda equivalent of the method reference.
   */
  private JCTree.JCLambda convert( JCTree.JCMemberReference methodRef,
    Symbol.MethodSymbol methodSymbol, UnaryOperator<JCTree.JCMethodInvocation> methodTransformer )
  {
    ParamAndDecl paramAndDecl = createParameter( methodRef.expr.type, methodRef.pos, methodSymbol );
    java.util.List<ParamAndDecl> lambdaParams = methodSymbol.params.stream().map( varSymbol -> createParameter( varSymbol.type, varSymbol.pos, methodSymbol) ).collect( Collectors.toList() );

    // Create the lambda's method invocation (e.g., x -> x.methodName())
    JCTree.JCMethodInvocation methodCall = make.Apply(
      List.nil(),  // No receiver (it will be the lambda parameter)
      make.Select( paramAndDecl.param, methodSymbol ),  // Instance method call on the parameter (x.methodName)
      List.from( lambdaParams.stream().map( pd -> pd.param ).collect( Collectors.toList()) )  // Arguments for the method
    );

    ListBuffer<JCTree.JCVariableDecl> lambdaArgs = new ListBuffer<>();
    // If it's not a static method reference (e.g., ClassName::staticMethod)
    if( !methodSymbol.getModifiers().contains( javax.lang.model.element.Modifier.STATIC ) )
    {
      lambdaArgs.add( paramAndDecl.paramDecl );
    }
    lambdaParams.forEach( pd -> lambdaArgs.add( pd.paramDecl ) );

    // method callback
    methodCall = methodTransformer.apply( methodCall );
    methodCall.type = methodSymbol.type.getReturnType();
    // Create the lambda declaration (parameter + body)
    JCTree.JCLambda lambda = make.Lambda( lambdaArgs.toList() , methodCall );
    lambda.setPos( methodRef.pos );
    lambda.type = methodRef.type;
    lambda.targets  = List.of( methodRef.type );
    return lambda;
  }

  private class ParamAndDecl
  {
    public final JCTree.JCIdent param;
    public final JCTree.JCVariableDecl paramDecl;

    ParamAndDecl( JCTree.JCIdent param, JCTree.JCVariableDecl paramDecl )
    {
      this.param = param;
      this.paramDecl =paramDecl;
    }
  }

  private ParamAndDecl createParameter( Type type, int pos , Symbol symbol){
    Name paramName = names.fromString("x" +  UUID.randomUUID() );
    JCTree.JCVariableDecl paramDecl = make.VarDef( make.Modifiers( FINAL | Flags.PARAMETER ), paramName, make.Type( type ), null );
    paramDecl.sym = new Symbol.VarSymbol( FINAL , paramDecl.name, type, symbol );
    paramDecl.type = paramDecl.sym.type;
    paramDecl.pos = pos;
    JCTree.JCIdent param = make.Ident( paramDecl.sym );
    param.type = type;
    param.pos = pos;
    return new ParamAndDecl( param, paramDecl );
  }
}