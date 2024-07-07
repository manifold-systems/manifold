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
import net.sf.jsqlparser.statement.select.*;

import java.util.Arrays;
import java.util.List;

public class JdbcSqlAnalyzer implements SqlAnalyzer
{
  private static final List<String> _queryStarts = Arrays.asList( "select", "with", "from", "pivot", "unpivot" );
  @Override
  public Statement makeStatement( String queryName, SqlScope scope, String sql )
  {
    boolean isQuery;
    try
    {
      // using this parser only to distinguish between query statements and non-query statements
      net.sf.jsqlparser.statement.Statement statement = CCJSqlParserUtil.parse( sql );
      isQuery = statement instanceof Select || statement instanceof Pivot || statement instanceof UnPivot;
    }
    catch( JSQLParserException e )
    {
      // todo: Maybe do light parsing to determine type of statement
      isQuery = _queryStarts.stream()
        .anyMatch( start -> sql.trim().toLowerCase().startsWith( start ) );
    }

    return isQuery
      ? new JdbcQueryTable( scope, queryName, sql )
      : new JdbcCommand( scope, queryName, sql );
  }
}
