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

import manifold.sql.api.Statement;
import manifold.sql.query.api.SqlAnalyzer;
import manifold.sql.query.type.SqlScope;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.Select;

public class JdbcSqlAnalyzer implements SqlAnalyzer
{
  @Override
  public Statement makeStatement( String queryName, SqlScope scope, String sql )
  {
    boolean isQuery;
    try
    {
      // using this parser only to distinguish between SELECT statements and NON-SELECT statements
      net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse( sql );
      isQuery = statement instanceof Select;
    }
    catch( JSQLParserException e )
    {
      // treat as query and pass through to JDBC for error handling.
      // todo: Maybe do light parsing to determine type of statement (Insert, Update, Delete, ..., or Select)?
      isQuery = sql.trim().toLowerCase().startsWith( "select" );
    }

    return isQuery
      ? new JdbcQueryTable( scope, queryName, sql )
      : new JdbcCommand( scope, queryName, sql );
  }
}
