package msql.parser.ast;

import msql.parser.Token;

import java.util.ArrayList;

/**
 * Created by klu on 6/22/2015.
 */
public class Expression {
  private ArrayList<AndCondition> _conditions;
  /*This is to keep track of which tokens we have to swallow/pass through*/
  private ArrayList<Token> swallowedTokens = new ArrayList<>();

  public Expression() {
    _conditions = new ArrayList<AndCondition>();
  }

  public Expression(AndCondition c) {
    _conditions = new ArrayList<AndCondition>();
    _conditions.add(c);
  }

  public void addCondition(AndCondition c) {
    _conditions.add(c);
  }

  public ArrayList<AndCondition> getConditions() {
    return _conditions;
  }

  public void addToken(Token t){swallowedTokens.add(t);}

  public ArrayList<Token> getSwallowedTokens(){return swallowedTokens;}

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("<Expression>\n");
    for (AndCondition c : _conditions) {
      sb.append(c.toString("\t"));
    }
    return sb.toString();
  }

  protected String toString(String initial) {
    StringBuilder sb = new StringBuilder(initial + "<Expression>\n");
    for (AndCondition c : _conditions) {
      sb.append(c.toString(initial + "\t"));
    }
    return sb.toString();
  }

}
