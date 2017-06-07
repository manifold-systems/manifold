package manifoldjs.parser.tree;


import java.util.LinkedList;
import java.util.List;

public class ProgramNode extends Node
{
  private List<Error> _errorList;

  public void addError(Error error) {
    _errorList.add(error);
  }

  public int errorCount() {
    return _errorList.size();
  }

  public List<Error> getErrorList() { return _errorList; }

  public void printErrors() {
    for (Error error : _errorList) {
      System.out.println(error.toString());
    }
  }

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
