package msql.parser.ast;

/**
 * Created by klu on 6/24/2015.
 */
public class CheckConstraint extends Constraint{
  private Expression _internal;
  private String name;

  public CheckConstraint(){
    _internal = null;
  }

  public CheckConstraint(String n){
    name = n;
    _internal = null;
  }

  public CheckConstraint(Expression e){
    _internal = e;
  }

  public CheckConstraint(String n, Expression e){
    name = n;
    _internal = e;
  }

  public void setExpression(Expression e){
    _internal = e;
  }

  public void addColumnName(String s){}

  public void setName(String s){
    name = s;
  }

  public String getName(){
    return name;
  }

  public String toString(){
    StringBuilder sb = new StringBuilder("<Constraint>CHECK\n");
    sb.append(_internal.toString("\t"));
    return sb.toString();
  }

  protected String toString(String initial){
    StringBuilder sb = new StringBuilder(initial+"<Constraint>CHECK\n");
    sb.append(_internal.toString(initial+"\t"));
    return sb.toString();
  }


}
