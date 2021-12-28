package manifold.ext.structural;

import manifold.ext.rt.api.Structural;

import java.math.BigDecimal;

@Structural
public interface SqlNumber {
  BigDecimal getNumber();

  default SqlNumber plus(SqlNumber operand) {
    return (SqlNumber) getNumber().add(operand.getNumber());
  }
}
