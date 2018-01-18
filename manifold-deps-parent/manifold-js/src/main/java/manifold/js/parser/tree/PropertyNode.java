package manifold.js.parser.tree;

public class PropertyNode extends ClassFunctionNode
{
  private boolean _isSetter;

  public PropertyNode( String name )
  {
    super( name );
  }

  //Test Constructor
  public PropertyNode(String name, boolean isSetter) {
    super(name);
    _isSetter = isSetter;
  }

  public PropertyNode(String name, String className, boolean isStatic, boolean isSetter) {
    super(name, className);
    _isSetter = isSetter;
    setStatic(isStatic);
  }

  public boolean isSetter() {
    return _isSetter;
  }

  @Override
  public String genCode()
  {
    String parameterCode = (getFirstChild(ParameterNode.class) == null) ?
            "" : getFirstChild(ParameterNode.class).genCode();
    String functionBodyCode = (getFirstChild(FunctionBodyNode.class) == null) ?
            "{}" : getFirstChild(FunctionBodyNode.class).genCode();
    return  (_isSetter?"set":"get") +
            ": function " + (_isSetter?"set":"get") + "(" + parameterCode + ")" +
            functionBodyCode; //Should have one FunctionBodyNode child
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PropertyNode)) return false;
    PropertyNode node = (PropertyNode) obj;
    return getName().equals(node.getName()) && isStatic() == node.isStatic() && _isSetter == node.isSetter() ;
  }

}
