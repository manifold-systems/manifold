package manifold.sql.parser.ast;

import manifold.sql.parser.Token;

import java.util.ArrayList;

/**
 * Created by klu on 6/22/2015.
 */
public class Case {
  private Expression _initial;
  private ArrayList<WhenThen> _whenThens;
  private Expression _else;
  /*This is to keep track of which tokens we have to swallow/pass through*/
  private ArrayList<Token> swallowedTokens = new ArrayList<>();

  public Case() {
    _initial = null;
    _whenThens = new ArrayList<WhenThen>();
    _else = null;
  }

  public Case(Expression init, ArrayList<WhenThen> wt, Expression el) {
    _initial = init;
    _whenThens = wt;
    _else = el;
  }

  public void addWhenThen(Expression when, Expression then) {
    _whenThens.add(new WhenThen(when, then));
  }

  public Expression getInitial() {
    return _initial;
  }

  public void setInitial(Expression init) {
    _initial = init;
  }

  public ArrayList<Expression[]> getWhenThenClauses() {
    ArrayList<Expression[]> clauses = new ArrayList<Expression[]>();
    for (WhenThen wt : _whenThens) {
      Expression[] currentClause = {wt._when, wt._then};
      clauses.add(currentClause);
    }
    return clauses;
  }

  public Expression getElse() {
    return _else;
  }

  public void setElse(Expression el) {
    _else = el;
  }

  public void addToken(Token t){swallowedTokens.add(t);}

  public ArrayList<Token> getSwallowedTokens(){return swallowedTokens;}

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("<Case>\n");
    if (_initial != null) {
      sb.append('\t');
      sb.append(_initial);
    }
    for (WhenThen wt : _whenThens) {
      sb.append("\tWHEN ");
      sb.append(wt._when);
      sb.append("\tTHEN ");
      sb.append(wt._then);
    }
    if (_else != null) {
      sb.append("\tELSE ");
      sb.append(_else);
    }
    return sb.toString();
  }

  protected String toString(String initial) {
    StringBuilder sb = new StringBuilder(initial + "<Case>\n");
    if (_initial != null) {
      sb.append(_initial.toString(initial + "\t"));
    }
    for (WhenThen wt : _whenThens) {
      sb.append(initial + "\tWHEN\n");
      sb.append(wt._when.toString(initial + "\t"));
      sb.append(initial + "\tTHEN\n");
      sb.append(wt._then.toString(initial + "\t"));
    }
    if (_else != null) {
      sb.append(initial + "\tELSE\n");
      sb.append(_else.toString(initial + "\t"));
    }
    return sb.toString();
  }

  private class WhenThen {
    private Expression _when;
    private Expression _then;

    private WhenThen(Expression w, Expression t) {
      _when = w;
      _then = t;
    }
  }

}
