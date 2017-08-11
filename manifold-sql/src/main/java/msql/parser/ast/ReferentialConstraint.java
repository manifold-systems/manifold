package msql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 6/24/2015.
 */
public class ReferentialConstraint extends Constraint{
  private String name;
  private ArrayList<String> _foreignKey;
  private ArrayList<String> _references;

  public ReferentialConstraint(){
    _foreignKey = new ArrayList<>();
    _references = new ArrayList<>();
  }

  public ReferentialConstraint(String n){
    name = n;
    _foreignKey = new ArrayList<>();
    _references = new ArrayList<>();
  }

  public ReferentialConstraint(String n, ArrayList<String> items){
    name = n;
    _foreignKey = items;
    _references = new ArrayList<>();
  }

  public void setExpression(Expression e){}

  public void addColumnName(String s){
    char c = s.charAt(s.length()-1);
    if(c == '`'){
      _foreignKey.add(s.substring(0,s.length()-1));
    } else if(c == '~'){
      _references.add(s.substring(0,s.length()-1));
    }
  }

  public void setName(String s){name = s;}

  public String getName(){return name;}

  public String toString(){
    StringBuilder sb = new StringBuilder("<Constraint>Foreign Key ");
    for(String s: _foreignKey){
      sb.append(s+" ");
    }
    sb.append("REFERENCES ");
    for(String s: _references){
      sb.append(s+" ");
    }
    return sb.toString();
  }

  protected String toString(String initial){
    StringBuilder sb = new StringBuilder(initial+"<Constraint>Foreign Key ");
    for(String s: _foreignKey){
      sb.append(s+" ");
    }
    sb.append("REFERENCES ");
    for(String s: _references){
      sb.append(s+" ");
    }
    return sb.toString();
  }
}
