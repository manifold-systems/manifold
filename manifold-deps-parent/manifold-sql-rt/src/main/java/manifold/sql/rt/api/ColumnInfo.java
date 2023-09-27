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

public class ColumnInfo
{
  private final String _name;
  private final int _jdbcType;
  private final String _sqlType;
  // See COLUMN_SIZE in java.sql.DatabaseMetaData
  private final Integer _size; // width for char/bin types, precision for numeric types, null when not applicable

  public ColumnInfo( String name, int jdbcType, String sqlType, Integer size )
  {
    _name = name;
    _jdbcType = jdbcType;
    _sqlType = sqlType;
    _size = size;
  }

  public String getName()
  {
    return _name;
  }

  public int getJdbcType()
  {
    return _jdbcType;
  }

  public String getSqlType()
  {
    return _sqlType;
  }

  public Integer getSize()
  {
    return _size;
  }
}
