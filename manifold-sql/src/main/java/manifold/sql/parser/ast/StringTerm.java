package manifold.sql.parser.ast;

/**
 * Created by klu on 6/22/2015.
 */
public class StringTerm extends Term {
  private String val;

  public StringTerm() {
    val = "";
  }

  public StringTerm(String s) {
    val = s;
  }

  public String getString() {
    return val;
  }

  public void setString(String s) {
    val = s;
  }

  public void setNegative(boolean isNeg) {
  }

  public String toString() {
    return "<Term> " + val + "\n";
  }

  protected String toString(String initial) {
    return initial + toString();
  }
}
