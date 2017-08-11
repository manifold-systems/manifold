package msql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 6/22/2015.
 */
public class SelectStatement extends Statement {
  private SimpleSelect _primary;
  private ArrayList<JavaVar> _variables;
  private ArrayList<SimpleSelect> _chained;
  private ArrayList<SimpleSelect> _recursive;
  private ArrayList<String> _chains;
  private ArrayList<OrderingTerm> _orderingterms;
  private Expression _limitingterm;
  private Expression _offsetterm;

  public SelectStatement(SimpleSelect simpleSelect){
    _primary = simpleSelect;
    _chained = new ArrayList<>();
    _recursive = new ArrayList<>();
    _chains = new ArrayList<>();
    _orderingterms = new ArrayList<>();
    _limitingterm = null;
    _offsetterm = null;
  }

  public void addChainedSelect(SimpleSelect simpleSelect, String string){
    _chained.add(simpleSelect);
    _chains.add(string);
  }

  public void setRecursiveSelect(SimpleSelect firstQuery, SimpleSelect secondQuery){
    _recursive.add(firstQuery);
    _recursive.add(secondQuery);
  }

  public void addOrderingTerm(OrderingTerm term){
    _orderingterms.add(term);
  }

  public void setLimitingTerm(Expression e){
    _limitingterm = e;
  }

  public void setOffsetterm(Expression e){
    _offsetterm = e;
  }

  public ArrayList<ResultColumn> getResultColumns(){
    return _primary == null? null:_primary.getResultColumns();
  }

  public void setVariables(ArrayList<JavaVar> vars){
    _variables = vars;
  }

  public ArrayList<JavaVar> getVariables(){
    return _variables;
  }

  public ArrayList<String> getTables(){
    if(_primary == null){
      return null;
    }
    ArrayList<TableOrSubquery> tables = _primary.getTablesAndSubqueries();
    ArrayList<String> out = new ArrayList<>();
    for(TableOrSubquery table: tables){
      out.add(table.getName());
    }
    return out;
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    if(_recursive.size()>0){
      sb.append("WITH RECURSIVE\n");
      sb.append("Initial Query:\n");
      sb.append(_recursive.get(0));
      sb.append("Recursive Query:\n");
      sb.append(_recursive.get(1));
      sb.append("Outside Query:\n");
    }
    sb.append(_primary);
    if(_chained.size()>0){
      for(int i = 0; i< _chained.size(); i++){
        sb.append(_chains.get(i)+"\n");
        sb.append(_chained.get(i));
      }
    }
    for(OrderingTerm _order: _orderingterms){
      sb.append("ORDER BY\n");
      sb.append(_order.toString("\t"));
    }
    if(_limitingterm != null){
      sb.append("LIMIT\n");
      sb.append(_limitingterm.toString("\t"));
    }
    if(_offsetterm != null){
      sb.append("OFFSET\n");
      sb.append(_offsetterm.toString("\t"));
    }
    return sb.toString();
  }

  protected String toString(String initial){
    StringBuilder sb = new StringBuilder();
    if(_recursive.size()>0){
      sb.append(initial+"WITH RECURSIVE\n");
      sb.append(initial+"Initial Query:\n");
      sb.append(_recursive.get(0).toString(initial));
      sb.append(initial+"Recursive Query:\n");
      sb.append(_recursive.get(1).toString(initial));
      sb.append(initial+"Outside Query:\n");
    }
    sb.append(_primary.toString(initial));
    if(_chained.size()>0){
      for(int i = 0; i< _chained.size(); i++){
        sb.append(initial+_chains.get(i)+"\n");
        sb.append(_chained.get(i).toString(initial));
      }
    }
    for(OrderingTerm _order: _orderingterms){
      sb.append(initial+"ORDER BY\n");
      sb.append(_order.toString(initial+"\t"));
    }
    if(_limitingterm != null){
      sb.append(initial+"LIMIT\n");
      sb.append(_limitingterm.toString(initial+"\t"));
    }
    if(_offsetterm != null){
      sb.append(initial+"OFFSET\n");
      sb.append(_offsetterm.toString(initial+"\t"));
    }
    return sb.toString();
  }

}
