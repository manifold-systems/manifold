package msql.parser.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by klu on 6/23/2015.
 */
public class Constraint {
  public enum constraintType{CHECK,UNIQUE,FOREIGN,PRIMARY,PRIMARYHASH};
  public enum referentialAction{CASCADE,RESTRICT,NO_ACTION,SET_DEFAULT,SET_NULL};
  private Expression expr;
  private List<String> columnNames;
  private List<String> referentialColumnNames;
  private constraintType type;
  private String name;
  private String referentialName;
  private referentialAction onDelete;
  private referentialAction onUpdate;

  public void setOnUpdate(referentialAction onUpdate) {
    this.onUpdate = onUpdate;
  }

  public void setOnDelete(referentialAction onDelete) {

    this.onDelete = onDelete;
  }

  public referentialAction getOnDelete() {

    return onDelete;
  }

  public referentialAction getOnUpdate() {

    return onUpdate;
  }




  public Constraint(){
    referentialColumnNames = new ArrayList<>();
    columnNames = new ArrayList<>();
  }

  public void setReferentialName(String s){
    referentialName = s;
  }

  public String getReferentialName(){
    return referentialName;
  }

  public void setName(String s){
    name = s;
  }

  public String getName(){
    return name;
  }

  public void setType(constraintType _type){
    type = _type;
  }

  public constraintType getType(){
    return type;
  }

  public void appendReferentialColumn(String col){
    referentialColumnNames.add(col);
  }

  public void setReferentialColumnNames(List<String> l){
    referentialColumnNames = l;
  }

  public void appendColumn(String col){
    columnNames.add(col);
  }

  public void setColumnNames(List<String> l){
    columnNames = l;
  }

  public Expression getExpr() {
    return expr;
  }

  public List<String> getColumnNames(){
    return columnNames;
  }

  public List<String> getReferentialColumnNames(){
    return referentialColumnNames;
  }

  public void setExpr(Expression expr) {
    this.expr = expr;
  }
}
