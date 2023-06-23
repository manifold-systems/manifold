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

import manifold.sql.query.api.QueryColumn;
import manifold.sql.query.api.QueryTable;
import manifold.sql.query.type.SqlScope;
import manifold.sql.rt.api.TypeMap;
import manifold.sql.rt.api.ConnectionNotifier;
import manifold.sql.schema.api.Schema;
import manifold.util.ManExceptionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JdbcQueryTable implements QueryTable
{
  private final SqlScope _scope;
  private final String _source;
  private final String _name;
  private final Map<String, JdbcQueryColumn> _columns;
  private final List<JdbcQueryParameter> _parameters;

  public JdbcQueryTable( SqlScope scope, String simpleName, String query )
  {
    _scope = scope;
    List<ParamInfo> paramNames = ParameterParser.getParameters( query );
    _source = replaceNamesWithQuestion( query, paramNames );
    _name = simpleName;
    _columns = new LinkedHashMap<>();
    _parameters = new ArrayList<>();

    if( _scope.isErrant() )
    {
      return;
    }

    try( Connection c = DriverManager.getConnection( scope.getDbconfig().getBuildUrlOtherwiseRuntimeUrl() ) )
    {
      build( c, paramNames );
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  private static String replaceNamesWithQuestion( String source, List<ParamInfo> params )
  {
    StringBuilder procSource = new StringBuilder( source );
    for( int i = params.size()-1; i >=0; i-- )
    {
      ParamInfo param = params.get( i );
      String name = param.getName();
      procSource.replace( param.getPos(), param.getPos() + name.length(), "?" );
    }
    return procSource.toString();
  }

  private void build( Connection c, List<ParamInfo> paramNames )
  {
    for( ConnectionNotifier p : ConnectionNotifier.PROVIDERS.get() )
    {
      p.init( c );
    }

    try( PreparedStatement preparedStatement = c.prepareStatement( _source ) )
    {
      // todo: handle warnings, make them compiler warnings

      ResultSetMetaData rsMetaData = preparedStatement.getMetaData();
      int columnCount = rsMetaData.getColumnCount();
      for( int i = 1; i <= columnCount; i++ )
      {
        JdbcQueryColumn col = new JdbcQueryColumn( i, this, rsMetaData );
        _columns.put( col.getName(), col );
      }

      ParameterMetaData paramMetaData = preparedStatement.getParameterMetaData();
      int paramCount = paramMetaData.getParameterCount();
      if( !paramNames.isEmpty() && paramCount != paramNames.size() )
      {
        //## todo: this should be compile error
        throw new SQLException( "Parameter name count does not match '?' param count. Query: " + _name + "\n" + _source );
      }
      for( int i = 1; i <= paramCount; i++ )
      {
        String name = paramNames.isEmpty() ? null : paramNames.get( i - 1 ).getName().substring( 1 );
        JdbcQueryParameter param = new JdbcQueryParameter( i, name, this, paramMetaData );
        _parameters.add( param );
      }
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  @Override
  public String getQuerySource()
  {
    return _source;
  }

  @Override
  public Schema getSchema()
  {
    return _scope.getSchema();
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public Map<String, JdbcQueryColumn> getColumns()
  {
    return _columns;
  }

  @Override
  public QueryColumn getColumn( String columnName )
  {
    return _columns.get( columnName );
  }

  @Override
  public List<JdbcQueryParameter> getParameters()
  {
    return _parameters;
  }

  public TypeMap getTypeMap()
  {
    return _scope.getSchema().getTypeMap();
  }
}
