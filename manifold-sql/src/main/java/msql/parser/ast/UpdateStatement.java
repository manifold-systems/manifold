package msql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 7/7/2015.
 */
public class UpdateStatement extends Statement{
  private String _table;
  private ArrayList<JavaVar> _vars;
  private String _alias;
  private UpdateType internalType;
  private Expression _where, _limit;

  public UpdateStatement(String name){
    _table = name;
    _alias = null;
    _where = null;
    _limit = null;
  }

  private abstract class UpdateType{
    protected abstract void addColumn(String colName);
    protected abstract void setStatement(SelectStatement statement);
    protected abstract void addExpression(Expression e);
    protected abstract String toString(String initial);
  }

  private class UpdateAsExpressions extends UpdateType{
    private ArrayList<String> _columns;
    private ArrayList<Expression> _expressions;

    private UpdateAsExpressions(String firstColumn, Expression e){
      _columns = new ArrayList<>();
      _expressions = new ArrayList<>();
      _columns.add(firstColumn);
      _expressions.add(e);
    }

    protected void addColumn(String colName){
      _columns.add(colName);
    }

    protected void setStatement(SelectStatement statement){}

    protected void addExpression(Expression e){
      _expressions.add(e);
    }

    protected String toString(String initial){
      StringBuilder sb = new StringBuilder(initial+"SET " + _columns.get(0) + "=\n");
      sb.append(_expressions.get(0).toString(initial+"\t"));
      for(int i = 1; i<_columns.size(); i++){
        sb.append(initial+"    " + _columns.get(i) + "=\n");
        sb.append(_expressions.get(0).toString(initial+"\t"));
      }
      return sb.toString();
    }
  }

  private class UpdateAsSelection extends UpdateType{
    private ArrayList<String> _columns;
    private SelectStatement _select;

    private UpdateAsSelection(String firstColumn){
      _columns = new ArrayList<>();
      _columns.add(firstColumn);
    }

    protected void addColumn(String colName){
      _columns.add(colName);
    }

    protected void setStatement(SelectStatement statement){
      _select = statement;
    }

    protected void addExpression(Expression e){}

    protected String toString(String initial){
      StringBuilder sb = new StringBuilder(initial+"Columns:\n");
      for(String col: _columns){
        sb.append(initial+col+"\n");
      }
      sb.append(initial+"=\n");
      sb.append(_select.toString(initial+"\t"));
      return sb.toString();
    }
  }

  public void set(String col, Expression e){
    internalType = new UpdateAsExpressions(col, e);
  }

  public void set(String col, SelectStatement s){
    internalType = new UpdateAsSelection(col);
    internalType.setStatement(s);
  }

  public void set(ArrayList<String> cols){
    internalType = new UpdateAsSelection(cols.get(0));
    cols.remove(0);
    addColumns(cols);
  }

  public void addColumns(ArrayList<String> cols){
    for(String col: cols){
      internalType.addColumn(col);
    }
  }

  public void addColumn(String s){
    internalType.addColumn(s);
  }

  public void addExpression(Expression e){
    internalType.addExpression(e);
  }

  public void setSelect(SelectStatement select){
    internalType.setStatement(select);
  }

  public void setAlias(String alias){
    _alias = alias;
  }

  public void setWhereExpression(Expression e){
    _where = e;
  }

  public void setLimitExpression(Expression e){
    _limit = e;
  }

  public void setVars(ArrayList<JavaVar> vars){
    _vars = vars;
  }

  public ArrayList<JavaVar> getVariables(){return _vars;}

  public ArrayList<ResultColumn> getResultColumns(){
    return null;
  }

  public ArrayList<String> getTables(){
    ArrayList<String> e = new ArrayList<>();
    e.add(_table);
    return e;
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder("<Update>\n"+_table);
    if(_alias != null){
      sb.append(" AS " + _alias);
    }
    sb.append("\n");
    sb.append(internalType.toString("\t"));
    if(_where != null){
      sb.append("WHERE\n");
      sb.append(_where.toString("\t"));
    }
    if(_limit != null){
      sb.append("LIMIT\n");
      sb.append(_limit.toString("\t"));
    }
    return sb.toString();
  }

  protected String toString(String initial){
    StringBuilder sb = new StringBuilder(initial+"<Update>\n"+initial+_table);
    if(_alias != null){
      sb.append(" AS " + _alias);
    }
    sb.append("\n");
    sb.append(internalType.toString(initial+"\t"));
    if(_where != null){
      sb.append(initial+"WHERE\n");
      sb.append(_where.toString(initial+"\t"));
    }
    if(_limit != null){
      sb.append(initial+"LIMIT\n");
      sb.append(_limit.toString(initial+"\t"));
    }
    return sb.toString();
  }

}
