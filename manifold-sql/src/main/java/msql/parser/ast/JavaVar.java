package msql.parser.ast;

/**
 * Created by klu on 6/25/2015.
 */
public class JavaVar {
  private String _varName;
  private String _varType;
  private int line, col, skiplen;

  public JavaVar(){}

  public JavaVar(String s){_varName = s;}

  public JavaVar(String s, String t){
    _varName = s;
    _varType = t;
  }

  public String getVarName(){return _varName;}

  public String getVarType(){return _varType;}

  public void setVarName(String s){_varName = s;}

  public void setVarType(String s){_varType = s;}

  public void setLine(int l){line = l;}

  public void setCol(int c){col = c;}

  public int getLine(){return line;}

  public int getCol(){return col;}

  public void setSkiplen(int len){skiplen = len;}

  public int getSkiplen(){return skiplen;}

  @Override
  public boolean equals(Object o){
    JavaVar var = (JavaVar) o;
    return this._varName.equals(var._varName);
  }
}
