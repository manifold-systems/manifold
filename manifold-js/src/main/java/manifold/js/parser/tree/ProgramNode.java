package manifold.js.parser.tree;


import java.util.LinkedList;
import java.util.List;
import manifold.js.parser.Tokenizer;

public class ProgramNode extends Node
{
  private List<ParseError> _errorList;

  public void addError( String msg, Tokenizer.Token token ) {
    _errorList.add(new ParseError(msg, token));
  }

  public int errorCount() {
    return _errorList.size();
  }

  public List<ParseError> getErrorList() { return _errorList; }

  public ProgramNode()
  {
    super( null );
    _errorList = new LinkedList<>();
  }

  /*Returns the full package name of imported class (ex. java.util.ArrayList)
   *from class name (ex. ArrayList)*/
   public String getPackageFromClassName(String packageClass) {
    for (ImportNode node: getChildren(ImportNode.class)) {
      if (node.getPackageClass().equals(packageClass))
        return node.getName();
    }
     return null;
  }

  @Override
  public String genCode() {
    String code = super.genCode();
    return code;
  }

}
