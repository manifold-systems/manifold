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

package manifold.sql.query.jdbc;

import manifold.rt.api.util.ManStringUtil;
import manifold.sql.api.Parameter;
import manifold.sql.query.api.Command;
import manifold.sql.query.type.SqlIssueContainer;
import manifold.sql.query.type.SqlScope;
import manifold.sql.rt.api.ConnectionProvider;
import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.util.DbUtil;
import manifold.sql.rt.util.DriverInfo;
import manifold.sql.schema.api.Schema;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static manifold.sql.util.StatementUtil.replaceNamesWithQuestion;

/**
 * Metadata required to generate a parameterized, executable type for a non-Select SQL command,
 * primarily Insert, Update, Delete
 */
public class JdbcCommand implements Command
{
  private final SqlScope _scope;
  private final String _source;
  private final String _name;
  private final String _escapedDdlName;
  private final List<Parameter> _parameters;
  private final SqlIssueContainer _issues;

  public JdbcCommand( SqlScope scope, String simpleName, String command )
  {
    _scope = scope;
    List<ParamInfo> paramNames = ParameterParser.getParameters( command );
    _source = replaceNamesWithQuestion( command, paramNames );
    _name = simpleName;
    _parameters = new ArrayList<>();
    Schema schema = _scope.getSchema();
    _issues = new SqlIssueContainer( schema == null ? DriverInfo.ERRANT : schema.getDriverInfo(),
      new ArrayList<>(), ManStringUtil.isCrLf( _source ) );

    if( _scope.isErrant() )
    {
      _escapedDdlName = _name;
      return;
    }

    ConnectionProvider cp = Dependencies.instance().getConnectionProvider();
    String ddlName = null;
    try( Connection c = cp.getConnection( scope.getDbconfig() ) )
    {
      ddlName = DbUtil.enquoteIdentifier( _name, c.getMetaData() );
      build( c, paramNames );
    }
    catch( SQLException e )
    {
      _issues.addIssues( Collections.singletonList( e ) );
    }
    _escapedDdlName = ddlName;
  }

  private void build( Connection c, List<ParamInfo> paramNames ) throws SQLException
  {
    DatabaseMetaData metadata = c.getMetaData();
    try( PreparedStatement ps = c.prepareStatement( _source ) )
    {
      ParameterMetaData paramMetaData = ps.getParameterMetaData();
      int paramCount = paramMetaData.getParameterCount();
      if( !paramNames.isEmpty() && paramCount != paramNames.size() )
      {
        throw new SQLException( "Parameter name count does not match '?' param count. Query: " + _name + "\n" + _source );
      }
      for( int i = 1; i <= paramCount; i++ )
      {
        String name = paramNames.isEmpty() ? null : paramNames.get( i - 1 ).getName().substring( 1 );
        JdbcParameter<JdbcCommand> param = new JdbcParameter<>( i, name, this, paramMetaData, metadata );
        _parameters.add( param );
      }
    }
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public String getEscapedDdlName() {
    return _escapedDdlName;
  }

  @Override
  public String getSqlSource()
  {
    return _source;
  }

  public List<Parameter> getParameters()
  {
    return _parameters;
  }

  @Override
  public SqlIssueContainer getIssues()
  {
    return _issues;
  }
}
