package msql.parser.ast;

/**
 * Created by klu on 6/22/2015.
 */
public class AlgebraicTerm extends Term {
  private long intNum;
  private double decNum;
  private boolean n = false;

  public AlgebraicTerm() {
    intNum = 0;
    decNum = 0.0;
  }

  public AlgebraicTerm(long i) {
    intNum = i;
    decNum = 0.0;
  }

  public AlgebraicTerm(double d) {
    intNum = 0;
    decNum = d;
  }

  public void setVal(long i) {
    if (decNum != 0) {
      decNum = 0;
    }
    intNum = i;
  }

  public void setVal(int i) {
    if (decNum != 0) {
      decNum = 0;
    }
    intNum = (long) i;
  }

  public void setVal(double d) {
    if (intNum != 0) {
      intNum = 0;
    }
    decNum = d;
  }

  public long getIntVal() {
    return n ? -1 * intNum : intNum;
  }

  public double getDoubleVal() {
    return n ? -1 * decNum : decNum;
  }

  public void setNegative(boolean isNeg) {
    n = isNeg;
  }

  public String toString() {
    String out;
    if (intNum == 0) {
      return "<Term> " + (n ? "-" + decNum : decNum) + "\n";
    } else {
      return "<Term> " + (n ? "-" + intNum : intNum) + "\n";
    }
  }

  protected String toString(String initial) {
    return initial + toString();
  }

}
