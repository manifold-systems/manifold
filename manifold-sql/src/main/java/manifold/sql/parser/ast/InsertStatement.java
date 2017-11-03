package manifold.sql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 7/7/2015.
 */
public class InsertStatement extends Statement{
  private String _table;
  private ArrayList<String> _columns;
  private ArrayList<JavaVar> _variables;
  private ValuesClause _values;
  private SelectStatement _select;
  private ArrayList<Expression> _expressions;

  public InsertStatement(String tablename){
    _table = tablename;
    _columns = new ArrayList<>();
    _variables = new ArrayList<>();
    _values = null;
    _select = null;
    _expressions = new ArrayList<>();
  }

  public void addColumn(String columnName){
    _columns.add(columnName);
  }

  public void setColumns(ArrayList<String> columns){
    _columns = columns;
  }

  public void set(ValuesClause vals){
    _values = vals;
    _select = null;
    _expressions = new ArrayList<>();
  }

  public void set(SelectStatement select){
    _select = select;
    _values = null;
    _expressions = new ArrayList<>();
  }

  public void addExpression(Expression e){
    _expressions.add(e);
    _select = null;
    _values = null;
  }

  public void setVars(ArrayList<JavaVar> variables){
    _variables = variables;
  }

  public ArrayList<ResultColumn> getResultColumns(){return null;}

  public ArrayList<JavaVar> getVariables(){return _variables;}

  public ArrayList<String> getTables(){
    ArrayList<String> tables = new ArrayList<>();
    tables.add(_table);
    return tables;
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder("<Insert>\nINTO: "+_table+"\n");
    if(_expressions.size()>0){
      sb.append("SET " + _columns.get(0) + "=\n");
      sb.append(_expressions.get(0).toString("\t"));
      for(int i=1; i<_expressions.size(); i++){
        sb.append("    " + _columns.get(i) + "\n");
        sb.append(_expressions.get(i).toString("\t"));
      }
    } else {
      if(_columns.size()>0){
        for(String col: _columns){
          sb.append("\t"+col+"\n");
        }
      }
      if(_values != null){
        sb.append(_values.toString("\t"));
      } else {
        sb.append(_select.toString("\t"));
      }
    }
    return sb.toString();
  }

  protected String toString(String initial){
    StringBuilder sb = new StringBuilder(initial+"<Insert>\n"+initial+"INTO: "+_table+"\n");
    if(_expressions.size()>0){
      sb.append(initial+"SET " + _columns.get(0) + "=\n");
      sb.append(_expressions.get(0).toString(initial+"\t"));
      for(int i=1; i<_expressions.size(); i++){
        sb.append(initial+"    " + _columns.get(i) + "\n");
        sb.append(_expressions.get(i).toString(initial+"\t"));
      }
    } else {
      if(_columns.size()>0){
        for(String col: _columns){
          sb.append(initial+"\t"+col+"\n");
        }
      }
      if(_values != null){
        sb.append(_values.toString(initial+"\t"));
      } else {
        sb.append(_select.toString(initial+"\t"));
      }
    }
    return sb.toString();
  }

}
