package manifold.ext.extensions.java.math.BigDecimal;

import manifold.ext.rt.api.IProxyFactory;
import manifold.ext.structural.SqlNumber;

import java.math.BigDecimal;

public class BigDecimal_To_SqlNumber implements IProxyFactory<BigDecimal, SqlNumber>
{
  @Override
  public SqlNumber proxy( BigDecimal bd, Class<SqlNumber> aClass) {
    return new Proxy(bd);
  }

  public static class Proxy implements SqlNumber {
    private final BigDecimal _bd;

    public Proxy(BigDecimal bd) {
      _bd = bd;
    }

    @Override
    public BigDecimal getNumber() {
      return _bd.getNumber();
    }
  }
}
