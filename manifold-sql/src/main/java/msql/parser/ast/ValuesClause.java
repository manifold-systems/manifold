package msql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 8/5/2015.
 */
public class ValuesClause {
  private ArrayList<ArrayList<Expression>> _expressions;
  private int size;

  public ValuesClause(ArrayList<Expression> initial){
    _expressions = new ArrayList<>();
    _expressions.add(initial);
    size = initial.size();
  }

  public void addExpressions(ArrayList<Expression> expr){
    _expressions.add(expr);
  }

  public ArrayList<ArrayList<Expression>> getExpressions(){return _expressions;}

  public int getSize(){return size;}

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder("<Values>\n");
    for(ArrayList<Expression> exprgroup: _expressions){
      sb.append("\t<Expressions Group>\n");
      for(Expression expression: exprgroup){
        sb.append(expression.toString("\t\t"));
      }
    }
    return sb.toString();
  }

  protected String toString(String initial){
    StringBuilder sb = new StringBuilder(initial+"<Values>\n");
    for(ArrayList<Expression> exprgroup: _expressions){
      sb.append(initial+"\t<Expressions Group>\n");
      for(Expression expression: exprgroup){
        sb.append(expression.toString(initial+"\t\t"));
      }
    }
    return sb.toString();
  }

}
