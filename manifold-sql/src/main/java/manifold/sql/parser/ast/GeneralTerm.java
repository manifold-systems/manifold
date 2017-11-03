package manifold.sql.parser.ast;

import java.util.ArrayList;

/**
 * Created by klu on 6/22/2015.
 */
public class GeneralTerm extends Term {
  private SelectStatement _select;
  private ExpressionArray _array;
  private boolean isNegative;

  public GeneralTerm() {
  }

  public GeneralTerm(SelectStatement s) {
    _select = s;
    _array = null;
  }

  public GeneralTerm(ExpressionArray a) {
    _array = a;
    _select = null;
  }

  public ArrayList<Expression> getExpressions() {
    return _array.getExpressions();
  }

  public void setExpression(ExpressionArray a) {
    _array = a;
  }

  public SelectStatement getSelect() {
    return _select;
  }

  public void setSelect(SelectStatement s) {
    _select = s;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("<Term>\n");
    if (_array != null) {
      sb.append("\t");
      sb.append(_array);
    }
    if (_select != null) {
      sb.append("\t");
      sb.append(_select);
    }
    return sb.toString();
  }

  public void setNegative(boolean isNeg) {
    isNegative = isNeg;
  }

  protected String toString(String initial) {
    StringBuilder sb = new StringBuilder(initial + "<Term>\n");
    if (_array != null) {
      sb.append(_array.toString(initial + "\t"));
    }
    if (_select != null) {
      sb.append("\t");
      sb.append(_select);
    }
    return sb.toString();
  }

}
