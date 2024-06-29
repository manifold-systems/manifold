package manifold.sql.schema.duckdb.base;

import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.Dependencies;
import manifold.sql.schema.h2.base.DdlServerTest;

public abstract class DuckdbDdlServerTest extends DdlServerTest
{
  @Override
  protected DbConfig getDbConfig()
  {
    return Dependencies.instance().getDbConfigProvider().loadDbConfig( "DuckdbSakila", getClass() );
  }
}
