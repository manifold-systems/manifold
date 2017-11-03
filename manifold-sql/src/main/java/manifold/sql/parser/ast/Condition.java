package manifold.sql.parser.ast;

import manifold.sql.parser.Token;

import java.util.ArrayList;

/**
 * Created by klu on 6/22/2015.
 */
public class Condition {
  /*We express condition as a relationship between two operands, regardless of what the relationship is*/
  private Operand first, second;
  /*This is to keep track of which tokens we have to swallow/pass through*/
  private ArrayList<Token> swallowedTokens = new ArrayList<>();


  public Condition() {
    first = null;
    second = null;
  }

  public Condition(Operand o1, Operand o2) {
    first = o1;
    second = o2;
  }

  public Condition(Operand o) {
    first = o;
    second = null;
  }

  public Operand getFirst() {
    return first;
  }

  public void setFirst(Operand o) {
    first = o;
  }

  public Operand getSecond() {
    return second;
  }

  public void setSecond(Operand o) {
    second = o;
  }

  public void addToken(Token t){swallowedTokens.add(t);}

  public ArrayList<Token> getSwallowedTokens(){return swallowedTokens;}

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("<Condition>\n\t");
    sb.append(first);
    if (second != null) {
      sb.append('\t');
      sb.append(second);
    }
    return sb.toString();
  }

  protected String toString(String initial) {
    StringBuilder sb = new StringBuilder(initial + "<Condition>\n");
    sb.append(first.toString(initial + "\t"));
    if (second != null) {
      sb.append(second.toString(initial + "\t"));
    }
    return sb.toString();
  }
}
