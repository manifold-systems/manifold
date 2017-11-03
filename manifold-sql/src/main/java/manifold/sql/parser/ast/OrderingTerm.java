package manifold.sql.parser.ast;


import manifold.sql.parser.Token;

import java.util.ArrayList;

/**
 * Created by klu on 8/5/2015.
 */
public class OrderingTerm {
  private Expression _internalexpr;
  private ArrayList<Token> _tokens;

  public OrderingTerm(Expression e){
    _internalexpr = e;
    _tokens = new ArrayList<>();
  }

  public void addToken(Token t){
    _tokens.add(t);
  }

  @Override
  public String toString(){
    StringBuilder sb = new StringBuilder("<Ordering Term>\n");
    sb.append(_internalexpr.toString("\t"));
    for(Token t: _tokens){
      sb.append("\t"+t.getText().toUpperCase()+"\n");
    }
    return sb.toString();
  }

  protected String toString(String initial){
    StringBuilder sb = new StringBuilder(initial+"<Ordering Term>\n");
    sb.append(_internalexpr.toString(initial+"\t"));
    for(Token t: _tokens){
      sb.append(initial+"\t"+t.getText().toUpperCase()+"\n");
    }
    return sb.toString();
  }

}
