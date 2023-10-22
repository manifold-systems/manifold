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

package manifold.sql.rt.api;

import java.util.Map;
import java.util.Set;

public class TableInfo
{
  private final String _ddlTableName;
  private final Set<String> _pkCols;
  private final Set<String> _ukCols;
  private final Map<String, ColumnInfo> _allCols;

  public TableInfo( String ddlTableName, Set<String> pkCols, Set<String> ukCols, Map<String,ColumnInfo> allCols )
  {
    _ddlTableName = ddlTableName;
    _pkCols = pkCols;
    _ukCols = ukCols;
    _allCols = allCols;
  }

  public String getDdlTableName()
  {
    return _ddlTableName;
  }

  public Set<String> getPkCols()
  {
    return _pkCols;
  }

  /** a non-null unique key */
  public Set<String> getUkCols()
  {
    return _ukCols;
  }

  public Map<String, ColumnInfo> getAllCols()
  {
    return _allCols;
  }
}
