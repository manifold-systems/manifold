package msql.parser.ast;

import msql.parser.Token;

import java.util.ArrayList;

/**
 * Created by klu on 6/22/2015.
 */
public class Factor {
  private ArrayList<Term> _terms;
  /*This is to keep track of which tokens we have to swallow/pass through*/
  private ArrayList<Token> swallowedTokens = new ArrayList<>();

  /*Contains data about operators in this factor: 0-TIMES; 1-DIVIDE; 2-MOD*/
  private ArrayList<Integer> _operators;

  public Factor() {
    _terms = new ArrayList<Term>();
    _operators = new ArrayList<Integer>();
  }

  public Factor(Term t) {
    _terms = new ArrayList<Term>();
    _terms.add(t);
    _operators = new ArrayList<Integer>();
  }

  private void addTerm(Term t) {
    _terms.add(t);
  }

  private void addOperator(char operator) {
    switch (operator) {
      case 'T':
        _operators.add(0);
        break;
      case 'S':
        _operators.add(1);
        break;
      case 'M':
        _operators.add(2);
        break;
      default:
        _operators.add(-1);
        break;
    }
  }

  public void add(String operator, Term t) {
    char o = operator.charAt(0);
    addOperator(o);
    addTerm(t);
  }

  public ArrayList<Term> getTerms() {
    return _terms;
  }

  public ArrayList<Integer> getOperators() {
    return _operators;
  }

  public void addToken(Token t){swallowedTokens.add(t);}

  public ArrayList<Token> getSwallowedTokens(){return swallowedTokens;}

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("<Factor>\n");
    sb.append('\t');
    sb.append(_terms.get(0));
    for (int i = 0; i < _operators.size(); i++) {
      switch (_operators.get(i)) {
        case 0:
          sb.append("\t * ");
          break;
        case 1:
          sb.append("\t / ");
          break;
        case 2:
          sb.append("\t % ");
          break;
      }
      sb.append(_terms.get(i + 1));
    }
    return sb.toString();
  }

  protected String toString(String initial) {
    StringBuilder sb = new StringBuilder(initial + "<Factor>\n");
    sb.append(_terms.get(0).toString(initial + "\t"));
    for (int i = 0; i < _operators.size(); i++) {
      switch (_operators.get(i)) {
        case 0:
          sb.append(initial + "\t *\n");
          break;
        case 1:
          sb.append(initial + "\t /\n");
          break;
        case 2:
          sb.append(initial + "\t %\n");
          break;
      }
      sb.append(_terms.get(i + 1).toString(initial + "\t"));
    }
    return sb.toString();
  }
}
