package manifold.sql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 8/5/2015.
 */
public class ExpressionArray {
  private ArrayList<Expression> _expressions;

  public ExpressionArray(Expression e){
    _expressions = new ArrayList<>();
  }

  public void addExpression(Expression e){
    _expressions.add(e);
  }

  public ArrayList<Expression> getExpressions(){return _expressions;}

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder();
    for(Expression e: _expressions){
      sb.append(e);
    }
    return sb.toString();
  }

  protected String toString(String initial){
    StringBuilder sb = new StringBuilder();
    for(Expression e: _expressions){
      sb.append(e.toString(initial));
    }
    return sb.toString();
  }
}
