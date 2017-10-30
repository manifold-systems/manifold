package manifold.js.parser.tree;


public class ImportNode extends Node
{
  public ImportNode(String packageName )
  {
    super( packageName );
    int lastDotIndex = packageName.lastIndexOf('.') + 1;
    if (lastDotIndex < 0) lastDotIndex = 0;
    _packageClass = packageName.substring(lastDotIndex);
  }


  private String _packageClass;

  @Override
  public String genCode()
  {
    return "var " + _packageClass + " = Java.type(\'" + getName() + "\');";
  }

  public String getPackageClass() {
    return _packageClass;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ImportNode)) return false;
    ImportNode node = (ImportNode) obj;
    return getName() != node.getName();
  }
}
