package manifold.sql.parser.ast;

import manifold.sql.parser.Token;

import java.util.ArrayList;

/**
 * Created by klu on 6/22/2015.
 */
public class Summand {
  private ArrayList<Factor> _factors;
  /*Contains data about the operators: FALSE - -; TRUE - +*/
  private ArrayList<Boolean> _operators;
  /*This is to keep track of which tokens we have to swallow/pass through*/
  private ArrayList<Token> swallowedTokens = new ArrayList<>();

  public Summand() {
    _factors = new ArrayList<Factor>();
    _operators = new ArrayList<Boolean>();
  }

  public Summand(Factor f) {
    _factors = new ArrayList<Factor>();
    _factors.add(f);
    _operators = new ArrayList<Boolean>();
  }

  private void addFactor(Factor f) {
    _factors.add(f);
  }

  private void addOperator(boolean op) {
    _operators.add(op);
  }

  public void add(String operator, Factor f) {
    char op = operator.charAt(0);
    if (op == 'M') {
      addOperator(false);
    } else if (op == 'P') {
      addOperator(true);
    }
    addFactor(f);
  }

  public ArrayList<Factor> getFactors() {
    return _factors;
  }

  public ArrayList<Boolean> getOperators() {
    return _operators;
  }

  public void addToken(Token t){swallowedTokens.add(t);}

  public ArrayList<Token> getSwallowedTokens(){return swallowedTokens;}

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("<Summand>\n");
    sb.append('\t');
    sb.append(_factors.get(0));
    for (int i = 0; i < _operators.size(); i++) {
      if (_operators.get(i)) {
        sb.append("\t + ");
      } else {
        sb.append("\t - ");
      }
      sb.append(_factors.get(i + 1));
    }
    return sb.toString();
  }

  protected String toString(String initial) {
    StringBuilder sb = new StringBuilder(initial + "<Summand>\n");
    sb.append(_factors.get(0).toString(initial + "\t"));
    for (int i = 0; i < _operators.size(); i++) {
      if (_operators.get(i)) {
        sb.append(initial + "\t +\n");
      } else {
        sb.append(initial + "\t -\n");
      }
      sb.append(_factors.get(i + 1).toString(initial + "\t"));
    }
    return sb.toString();
  }
}
