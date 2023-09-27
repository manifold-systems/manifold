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

import manifold.rt.api.Bindings;

import java.util.function.Function;

public class QueryContext<T extends ResultRow>
{
  private final TxScope _txScope;
  private final Class<T> _queryClass;
  private final ColumnInfo[] _paramInfo;
  private final Bindings _params;
  private final String _configName;
  private final Function<TxBindings, T> _makeRow;
  private final String _ddlTableName;

  public QueryContext( TxScope txScope, Class<T> queryClass, String ddlTableName,
                       ColumnInfo[] paramInfo, Bindings params,
                       String configName, Function<TxBindings, T> makeRow )
  {
    _txScope = txScope;
    _queryClass = queryClass;
    _ddlTableName = ddlTableName;
    _paramInfo = paramInfo;
    _params = params;
    _configName = configName;
    _makeRow = makeRow;
  }

  public TxScope getTxScope()
  {
    return _txScope;
  }

  public Class<T> getQueryClass()
  {
    return _queryClass;
  }

  public String getDdlTableName()
  {
    return _ddlTableName;
  }

  public ColumnInfo[] getParamInfo()
  {
    return _paramInfo;
  }

  public Bindings getParams()
  {
    return _params;
  }

  public String getConfigName()
  {
    return _configName;
  }

  public Function<TxBindings, T> getRowMaker()
  {
    return _makeRow;
  }
}
