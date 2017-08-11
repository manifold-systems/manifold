package msql.parser.ast;

import msql.parser.Token;

import java.util.ArrayList;

/**
 * Created by klu on 6/22/2015.
 */
public class Operand {
  private ArrayList<Summand> _summands;
  /*This is to keep track of which tokens we have to swallow/pass through*/
  private ArrayList<Token> swallowedTokens = new ArrayList<>();

  public Operand() {
    _summands = new ArrayList<Summand>();
  }

  public Operand(Summand s) {
    _summands = new ArrayList<Summand>();
    _summands.add(s);
  }

  public void addSummand(Summand s) {
    _summands.add(s);
  }

  public ArrayList<Summand> getSummands() {
    return _summands;
  }

  public void addToken(Token t){swallowedTokens.add(t);}

  public ArrayList<Token> getSwallowedTokens(){return swallowedTokens;}

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("<Operand>\n");
    for (Summand s : _summands) {
      sb.append('\t');
      sb.append(s);
    }
    return sb.toString();
  }

  protected String toString(String initial) {
    StringBuilder sb = new StringBuilder(initial + "<Operand>\n");
    for (Summand s : _summands) {
      sb.append(s.toString(initial + "\t"));
    }
    return sb.toString();
  }
}
