package manifold.ext.structural;

import manifold.ext.rt.api.Structural;

import java.math.BigDecimal;

@Structural
public interface SqlNumber {
  BigDecimal getNumber();
  double something(double d, int i);

  default SqlNumber plus(SqlNumber operand) {
    return (SqlNumber) getNumber().add(operand.getNumber());
  }
}
