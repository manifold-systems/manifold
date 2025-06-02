package manifold.sql.rt.impl;

import com.zaxxer.hikari.HikariDataSource;
import manifold.ext.delegation.rt.api.link;

import java.sql.Connection;
import java.sql.SQLException;

public class NonPooledConnection implements Connection
{
  @link Connection _c;
  private final HikariDataSource _ds;

  public NonPooledConnection( HikariDataSource ds, Connection c )
  {
    _ds = ds;
    _c = c;
  }

  @Override
  public void close() throws SQLException
  {
    _c.close();
    // close the data source as well to avoid pooled connection
    _ds.close();
  }
}
