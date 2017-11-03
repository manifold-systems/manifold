package manifold.sql.parser.ast;

/**
 * Created by klu on 6/22/2015.
 */
public class QuestionTerm extends Term {
  private long intNum;

  public QuestionTerm() {
  }

  public QuestionTerm(long l) {
    intNum = l;
  }

  public void setIntNum(int i) {
    intNum = (long) i;
  }

  public long getIntNum() {
    return intNum;
  }

  public void setIntNum(long i) {
    intNum = i;
  }

  public String toString() {
    return "<Term> ? " + intNum + "\n";
  }

  protected String toString(String initial) {
    return initial + toString();
  }

  public void setNegative(boolean isNeg) {
  }
}
