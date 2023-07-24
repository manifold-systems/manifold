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

public class UpdateContext<T extends ResultRow>
{
  private final TxScope _txScope;
  private final T _table;
  private final String _configName;
  private final String _ddlTableName;
  private final Set<String> _pkCols;
  private final Set<String> _ukCols;
  private final Map<String, Integer> _allColsWithJdbcType;

  public UpdateContext( TxScope txScope, T table, String ddlTableName, String configName,
                        Set<String> pkCols, Set<String> ukCols, Map<String,Integer> allColsWithJdbcType )
  {
    _txScope = txScope;
    _table = table;
    _ddlTableName = ddlTableName;
    _configName = configName;
    _pkCols = pkCols;
    _ukCols = ukCols;
    _allColsWithJdbcType = allColsWithJdbcType;
  }

  public TxScope getTxScope()
  {
    return _txScope;
  }

  public T getTable()
  {
    return _table;
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

  public Map<String, Integer> getAllColsWithJdbcType()
  {
    return _allColsWithJdbcType;
  }

  public String getConfigName()
  {
    return _configName;
  }
}
