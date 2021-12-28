package manifold.ext.structural;

import java.math.BigDecimal;

// structurally implements SqlNumber
public class FooNumber {
  BigDecimal _bd;

  public FooNumber(String s) {
    _bd = new BigDecimal(s);
  }

  //structurally implements SqlNumber#getNumber()
  public BigDecimal getNumber() {
    return _bd;
  }

  public String toString() {return _bd.toString();}
}
