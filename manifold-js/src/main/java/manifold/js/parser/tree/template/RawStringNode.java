package manifold.js.parser.tree.template;


import manifold.js.parser.tree.Node;

public class RawStringNode extends Node
{

  String _rawString;
  public RawStringNode(String rawString)
  {
    super( null );
    _rawString = rawString;
  }

  @Override
  public String genCode()
  {
    return _rawString;
  }
}
