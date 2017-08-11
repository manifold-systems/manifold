package msql.parser.ast;

/**
 * Created by klu on 7/6/2015.
 */
public class TableOrSubquery {
  private String _name;
  private Object _containedObject;
  private Expression _joinexpression;

  public TableOrSubquery(String name){
    _name = name;
    _containedObject = null;
    _joinexpression = null;
  }

  public TableOrSubquery(ValuesClause values){
    _name = "@@Subquery";
    _containedObject = values;
    _joinexpression = null;
  }

  public TableOrSubquery(SelectStatement selection){
    _name = "@@Subquery";
    _containedObject = selection;
    _joinexpression = null;
  }

  public void setJoinExpression(Expression e){
    _joinexpression = e;
  }

  public String getName(){return _name;}

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder("<Table Or Subquery>");
    if(_containedObject == null){
      sb.append("Table: " + _name + "\n");
    } else {
      sb.append("\n\t<Subquery>\n");
      if(_containedObject instanceof ValuesClause){
        sb.append(((ValuesClause) _containedObject).toString("\t"));
      } else if(_containedObject instanceof SelectStatement){
        sb.append(((SelectStatement) _containedObject).toString("\t"));
      }
    }
    if(_joinexpression != null){
      sb.append("\tJOIN ON\n");
      sb.append(_joinexpression.toString("\t"));
    }
    return sb.toString();
  }

  protected String toString(String initial){
    StringBuilder sb = new StringBuilder(initial+"<Table Or Subquery>");
    if(_containedObject == null){
      sb.append("Table: " + _name + "\n");
    } else {
      sb.append("\n"+initial+"\t<Subquery>\n");
      if(_containedObject instanceof ValuesClause){
        sb.append(((ValuesClause) _containedObject).toString(initial+"\t"));
      } else if(_containedObject instanceof SelectStatement){
        sb.append(((SelectStatement) _containedObject).toString(initial+"\t"));
      }
    }
    if(_joinexpression != null){
      sb.append(initial+"\tJOIN ON\n");
      sb.append(_joinexpression.toString(initial+"\t"));
    }
    return sb.toString();
  }
}
