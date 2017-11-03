package manifold.sql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 7/7/2015.
 */
public class DeleteStatement extends Statement{
  private String _tableName;
  private Expression _expr;
  private Term _term;
  private ArrayList<JavaVar> _vars;

  public DeleteStatement(String name){
    _tableName = name;
    _vars = new ArrayList<>();
    _expr = null;
    _term = null;
  }

  public String getTableName() {
    return _tableName;
  }

  public void setTableName(String _tableName) {
    this._tableName = _tableName;
  }

  public Expression getExpr() {
    return _expr;
  }

  public void setExpr(Expression _expr) {
    this._expr = _expr;
  }

  public Term getTerm() {
    return _term;
  }

  public void setTerm(Term _term) {
    this._term = _term;
  }

  public void addVar(JavaVar v){
    _vars.add(v);
  }

  public void setVars(ArrayList<JavaVar> v){
    _vars = v;
  }

  public ArrayList<ResultColumn> getResultColumns(){
    return null;
  }

  public ArrayList<String> getTables(){
    ArrayList<String> list = new ArrayList<>();
    list.add(_tableName);
    return list;
  }

  public ArrayList<JavaVar> getVariables(){
    return _vars;
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder("<Delete>\nFROM\n");
    sb.append(_tableName+"\n");
    if(_expr != null){
      sb.append("WHERE\n");
      sb.append(_expr.toString("\t"));
    }
    if(_term != null){
      sb.append("LIMIT\n");
      sb.append(_term.toString("\t"));
    }
    return sb.toString();
  }
}
