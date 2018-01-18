package manifold.js.parser.tree;


import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import manifold.js.parser.Token;

public class ProgramNode extends Node
{
  private List<ParseError> _errorList;
  private URL _url;

  public ProgramNode( URL url )
  {
    super( null );
    _url = url;
    _errorList = new LinkedList<>();
  }

  public void addError( String msg, Token token ) {
    _errorList.add(new ParseError(msg, token));
  }

  @Override
  public ProgramNode getProgramNode()
  {
    return this;
  }

  public URL getUrl()
  {
    return _url;
  }

  public int errorCount() {
    return _errorList.size();
  }

  public List<ParseError> getErrorList() { return _errorList; }

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
