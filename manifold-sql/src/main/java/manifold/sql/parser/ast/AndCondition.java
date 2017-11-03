package manifold.sql.parser.ast;

import manifold.sql.parser.Token;

import java.util.ArrayList;

/**
 * Created by klu on 6/22/2015.
 */
public class AndCondition {
  private ArrayList<Condition> _conditions;
  /*This is to keep track of which tokens we have to swallow/pass through*/
  private ArrayList<Token> swallowedTokens = new ArrayList<>();

  public AndCondition() {
    _conditions = new ArrayList<Condition>();
  }

  public AndCondition(Condition c) {
    _conditions = new ArrayList<Condition>();
    _conditions.add(c);
  }

  public void addCondition(Condition c) {
    _conditions.add(c);
  }

  public ArrayList<Condition> getConditions() {
    return _conditions;
  }

  public void addToken(Token t){swallowedTokens.add(t);}

  public ArrayList<Token> getSwallowedTokens(){return swallowedTokens;}

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("<AndCondition>\n");
    for (Condition c : _conditions) {
      sb.append('\t');
      sb.append(c);
    }
    return sb.toString();
  }

  protected String toString(String initial) {
    StringBuilder sb = new StringBuilder(initial + "<AndCondition>\n");
    for (Condition c : _conditions) {
      sb.append(c.toString(initial + "\t"));
    }
    return sb.toString();
  }
}
