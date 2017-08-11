package msql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 6/24/2015.
 */
public class ColumnConstraint extends Constraint{
  private ArrayList<String> columns;
  private String name;
  private boolean _type;

  public ColumnConstraint(){columns = new ArrayList<String>();}

  public ColumnConstraint(String s){
    name = s;
    columns = new ArrayList<String>();
  }

  public ColumnConstraint(String s, String s2){
    name = s;
    columns = new ArrayList<String>();
    columns.add(s2);
  }

  public ColumnConstraint(String s, String s2, boolean b){
    name = s;
    columns = new ArrayList<String>();
    columns.add(s2);
    _type = b;
  }

  public void setExpression(Expression e){}

  public void addColumnName(String s){columns.add(s);}

  public void setName(String s){name = s;}

  public String getName(){return name;}

  public String toString(){
    StringBuilder sb = new StringBuilder("<Constraint>");
    if(_type){
      sb.append("PRIMARY KEY ");
    } else {
      sb.append("UNIQUE ");
    }
    for(String s: columns){
      sb.append(s+" ");
    }
    sb.append('\n');
    return sb.toString();
  }

  public String toString(String initial){
    StringBuilder sb = new StringBuilder(initial+"<Constraint>");
    if(_type){
      sb.append("PRIMARY KEY ");
    } else {
      sb.append("UNIQUE ");
    }
    for(String s: columns){
      sb.append(s+" ");
    }
    sb.append('\n');
    return sb.toString();
  }
}
