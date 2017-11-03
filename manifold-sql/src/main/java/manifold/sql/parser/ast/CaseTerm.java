package manifold.sql.parser.ast;

/**
 * Created by klu on 6/22/2015.
 */
public class CaseTerm extends Term {
  private Case _case;

  public CaseTerm() {
    _case = null;
  }

  public CaseTerm(Case c) {
    _case = c;
  }

  public Case getCase() {
    return _case;
  }

  public void setCase(Case c) {
    _case = c;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("<Term>\n");
    if (_case != null) {
      sb.append(_case);
    }
    return sb.toString();
  }

  public void setNegative(boolean isNeg) {
  }

  protected String toString(String initial) {
    StringBuilder sb = new StringBuilder(initial + "<Term>\n");
    if (_case != null) {
      sb.append(_case.toString(initial + "\t"));
    }
    return sb.toString();
  }

}
