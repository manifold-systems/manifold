package manifold.sql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 8/5/2015.
 */
public class SimpleSelect {
  private Term _limitingterm;
  private ArrayList<ResultColumn> _resultcolumns;
  private String _alias;
  private ArrayList<TableOrSubquery> _tables;
  private Expression _where, _having;
  private ArrayList<Expression> _groupby;

  public SimpleSelect(ResultColumn _resultcolumn){
    _limitingterm = null;
    _resultcolumns = new ArrayList<>();
    _resultcolumns.add(_resultcolumn);
    _alias = null;
    _tables = new ArrayList<>();
    _where = null;
    _having = null;
    _groupby = new ArrayList<>();
  }

  public SimpleSelect(ResultColumn _resultcolumn, Term _top){
    _limitingterm = _top;
    _resultcolumns = new ArrayList<>();
    _resultcolumns.add(_resultcolumn);
    _alias = null;
    _tables = new ArrayList<>();
    _where = null;
    _having = null;
    _groupby = new ArrayList<>();
  }

  public void addResultColumn(ResultColumn _resultcolumn){
    _resultcolumns.add(_resultcolumn);
  }

  protected ArrayList<ResultColumn> getResultColumns(){
    return _resultcolumns;
  }

  public void setAlias(String alias){
    _alias = alias;
  }

  public void addTableOrSubquery(TableOrSubquery table){
    _tables.add(table);
  }

  protected ArrayList<TableOrSubquery> getTablesAndSubqueries(){
    return _tables;
  }

  public void setWhereExpression(Expression e){
    _where = e;
  }

  public void setHavingExpression(Expression e){
    _having = e;
  }

  public void addGroupByExpression(Expression e){
    _groupby.add(e);
  }

  public void addOnExprToTable(Expression e){
    _tables.get(0).setJoinExpression(e);
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder("<Select>\n");
    if(_limitingterm != null){
      sb.append("\tTOP\n");
      sb.append(_limitingterm.toString("\t"));
    }
    for(ResultColumn _result: _resultcolumns){
      sb.append(_result.toString("\t"));
    }
    if(_alias != null){
      sb.append("\tAS " + _alias + " \n");
    }
    sb.append("\tFROM\n");
    for(TableOrSubquery _table: _tables){
      sb.append(_table.toString("\t"));
    }
    if(_where != null){
      sb.append("\tWHERE\n");
      sb.append(_where.toString("\t"));
    }
    for(Expression _group: _groupby){
      sb.append("\tGROUP BY\n");
      sb.append(_group.toString("\t"));
    }
    if(_having != null){
      sb.append("\tHAVING\n");
      sb.append(_having.toString("\t"));
    }
    return sb.toString();
  }

  protected String toString(String initial){
    StringBuilder sb = new StringBuilder(initial+"<Select>\n");
    if(_limitingterm != null){
      sb.append(initial+"\tTOP\n");
      sb.append(_limitingterm.toString(initial+"\t"));
    }
    for(ResultColumn _result: _resultcolumns){
      sb.append(_result.toString(initial+"\t"));
    }
    if(_alias != null){
      sb.append(initial+"\tAS " + _alias + " \n");
    }
    sb.append(initial+"\tFROM\n");
    for(TableOrSubquery _table: _tables){
      sb.append(_table.toString(initial+"\t"));
    }
    if(_where != null){
      sb.append(initial+"\tWHERE\n");
      sb.append(_where.toString(initial+"\t"));
    }
    for(Expression _group: _groupby){
      sb.append(initial+"\tGROUP BY\n");
      sb.append(_group.toString(initial+"\t"));
    }
    if(_having != null){
      sb.append(initial+"\tHAVING\n");
      sb.append(_having.toString(initial+"\t"));
    }
    return sb.toString();
  }

}
