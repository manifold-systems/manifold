package manifold.sql.schema.db2;

import manifold.sql.DdlServerTest;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.Dependencies;

public class Db2DdlServerTest extends DdlServerTest
{
  @Override
  protected DbConfig getDbConfig()
  {
    return Dependencies.instance().getDbConfigProvider().loadDbConfig( "Db2Sakila", getClass() );
  }
}