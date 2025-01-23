package manifold.ext;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import manifold.internal.javac.TypeProcessor;

import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static com.sun.tools.javac.code.Flags.FINAL;

public class MethodReferenceToLambda
{
  private final TreeMaker make;
  private final Names names;
  private final Types types;

  public MethodReferenceToLambda( TypeProcessor _tp )
  {
    this.make = _tp.getTreeMaker();
    this.names = Names.instance( _tp.getContext() );
    this.types = _tp.getTypes();
  }

  /**
   * Converts a JCMemberReference (method reference) to a JCLambda (lambda expression).
   *
   * @param methodRef The JCMemberReference to convert.
   * @param structural true if this is a structural method call
   * @param methodTransformer the function to apply on the created method call
   *
   * @return A JCLambda representing the lambda equivalent of the method reference.
   */
  public JCTree.JCLambda convert( JCTree.JCMemberReference methodRef, boolean structural, UnaryOperator<JCTree.JCMethodInvocation> methodTransformer )
  {
    Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) methodRef.sym;
    ParamAndDecl paramAndDecl = createParameter( methodSymbol.owner.type, methodRef.pos, methodSymbol );
    java.util.List<ParamAndDecl> lambdaParams = methodSymbol.params.stream().map( varSymbol -> createParameter( varSymbol.type, varSymbol.pos, methodSymbol ) ).collect( Collectors.toList() );

    // Create the lambda's method invocation (e.g., x -> x.methodName())
    JCTree.JCMethodInvocation methodCall = make.Apply( List.nil(),  // No receiver (it will be the lambda parameter)
      make.Select( paramAndDecl.param, methodSymbol ),  // Instance method call on the parameter (x.methodName)
      List.from( lambdaParams.stream().map( pd -> pd.param ).collect( Collectors.toList() ) )  // Arguments for the method
    );
    methodCall.polyKind = JCTree.JCPolyExpression.PolyKind.STANDALONE;

    // method callback
    methodCall = methodTransformer.apply( methodCall );
    methodCall.type = methodSymbol.type.getReturnType();

    ListBuffer<JCTree.JCVariableDecl> lambdaArgs = new ListBuffer<>();
    if( !methodSymbol.getModifiers().contains( javax.lang.model.element.Modifier.STATIC ) )
    {
      lambdaArgs.add( paramAndDecl.paramDecl );
    }
    lambdaParams.forEach( pd -> lambdaArgs.add( pd.paramDecl ) );

    // Create the lambda declaration (parameter + body)
    JCTree.JCLambda lambda = make.Lambda( lambdaArgs.toList(), methodCall );
    lambda.setPos( methodRef.pos );
    lambda.type = methodRef.type;
    lambda.targets = List.of( structural ? types.erasure( methodRef.type ) : methodRef.type );
    return lambda;
  }

  private static class ParamAndDecl
  {
    public final JCTree.JCIdent param;
    public final JCTree.JCVariableDecl paramDecl;

    ParamAndDecl( JCTree.JCIdent param, JCTree.JCVariableDecl paramDecl )
    {
      this.param = param;
      this.paramDecl = paramDecl;
    }
  }

  private ParamAndDecl createParameter( Type type, int pos, Symbol symbol )
  {
    Name paramName = names.fromString( "x" + UUID.randomUUID() );
    JCTree.JCVariableDecl paramDecl = make.VarDef( make.Modifiers( FINAL | Flags.PARAMETER ), paramName, make.Type( type ), null );
    paramDecl.sym = new Symbol.VarSymbol( FINAL, paramDecl.name, type, symbol );
    paramDecl.type = paramDecl.sym.type;
    paramDecl.pos = pos;
    JCTree.JCIdent param = make.Ident( paramDecl.sym );
    param.type = type;
    param.pos = pos;
    return new ParamAndDecl( param, paramDecl );
  }
}