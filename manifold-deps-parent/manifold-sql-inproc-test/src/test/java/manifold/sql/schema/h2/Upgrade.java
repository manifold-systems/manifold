package manifold.sql.schema.h2;

import java.util.Properties;

/**
 * Run this when you upgrade the H2 driver and the format changes
 */
public class Upgrade
{
  public static void main( String[] args ) throws Exception
  {
    org.h2.tools.Upgrade.upgrade(
      "jdbc:h2:file:C:\\manifold-systems\\manifold\\manifold-deps-parent\\manifold-sql-inproc-test\\src\\test\\resources\\samples\\db\\h2-sales",
      new Properties(),
      214 ); // old version, from: 2.1.214  to: 2.2.220
  }
}
