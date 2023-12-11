/*
 * Copyright (c) 2023 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.sql.schema.h2.base;

import manifold.rt.api.util.StreamUtil;
import manifold.sql.DbResourceFileTest;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.util.DriverInfo;
import manifold.sql.rt.util.SqlScriptRunner;
import org.junit.After;
import org.junit.Before;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

import static manifold.sql.rt.util.DriverInfo.Oracle;

/**
 * Before each test method, drops and recreates db (or schema) based on a DDL resource file specified by "dbDdl" entry
 * in DbConfig file. All connections, tx scopes, and DbConfigs are cleared/closed after each test method. Effectively,
 * each test method has a brand-new db as defined by the DDL script.
 * <p/>
 * Note, for embedded db systems such as H2 it's best to derive tests from {@link DbResourceFileTest}. This base class
 * is designed for db servers running in a separate process, although it can be used for embedded/in-process DBMS.
 */
public abstract class DdlServerTest
{
  /**
   * Provide the DbConfig corresponding with the derived test.
   */
  protected abstract DbConfig getDbConfig();

  /**
   * Executes the DDL resource file specified in the DbConfig "dbDdl" entry. Note, the DDL file must be idempotent wrt
   * database/schema creation. Basically, it should drop and recreate the db/schema or otherwise provide a fresh schema
   * as if just created e.g., auto-increment columns zeroed, etc.
   */
  @Before
  public void prepare() throws IOException
  {
    DbConfig dbConfig = getDbConfig();
    String ddl = dbConfig.getDbDdl();
    if( ddl == null )
    {
      throw new RuntimeException( "Missing 'dbDdl' entry from DbConfig: " + dbConfig.getName() );
    }
    if( !ddl.startsWith( "/" ) && !ddl.startsWith( "\\" ) )
    {
      ddl = "/" + ddl;
    }
    try( InputStream stream = getClass().getResourceAsStream( ddl ) )
    {
      if( stream == null )
      {
        throw new RuntimeException( "No resource file found matching: " + ddl );
      }

      String script = StreamUtil.getContent( new InputStreamReader( stream ) );

      ConnectionProvider cp = Dependencies.instance().getConnectionProvider();
      try( Connection c = cp.getConnection( dbConfig.getName(), getClass() ) )
      {
        boolean isOracle = DriverInfo.lookup( c.getMetaData() ) == Oracle;
        SqlScriptRunner.runScript( c, script,
          // this is the only way to let drop user fail and continue running the script
          isOracle ? (s, e) -> s.toLowerCase().contains( "drop user " ) : null );
      }
      catch( SQLException e )
      {
        throw new RuntimeException( e );
      }
    }
  }

  @After
  public void cleanup()
  {
    // close and clear db connections
    Dependencies.instance().getConnectionProvider().closeAll();

    // clear default tx scopes
    Dependencies.instance().getDefaultTxScopeProvider().clear();

    // clear DbConfigs. Forces dbconfig to initialize and exec ddl on first connection
    Dependencies.instance().getDbConfigProvider().clear();
  }

  protected void loadData( String dataResourcePath ) throws IOException
  {
    SqlScriptRunner.runScript( dataResourcePath, getDbConfig(), getClass() );
  }
}
