package manifold.js.parser.tree;


public class FunctionNode extends Node
{

  private String _returnType = "java.lang.Object";

  public FunctionNode( String name )
  {
    super( name );
  }



  public void setReturnType(String returnType){  _returnType = returnType; }

  public String getReturnType() {return _returnType;}


  @Override
  public String genCode()
  {
    String parameterCode = (getFirstChild(ParameterNode.class) == null) ?
            "" : getFirstChild(ParameterNode.class).genCode();
    String functionBodyCode = (getFirstChild(FunctionBodyNode.class) == null) ?
            "{}" : getFirstChild(FunctionBodyNode.class).genCode();
      return "function " + getName() + "(" + parameterCode + ")" + functionBodyCode;
  }


  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof FunctionNode)) return false;
    FunctionNode node = (FunctionNode) obj;
    return getName().equals(node.getName());
  }
}
