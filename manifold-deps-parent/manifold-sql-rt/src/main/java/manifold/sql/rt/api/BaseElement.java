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

public interface BaseElement
{
  String getName();
  int getPosition();
  int getSize();
  int getScale();
  boolean isNullable();
  int getJdbcType();
  String getSqlType();
  String getColumnClassName();

  default boolean isGenerated()
  {
    return false;
  }

  default boolean isAutoIncrement()
  {
    return false;
  }

  /**
   * Returns true if the column's value can be null, particularly in the interim between create and commit where generated
   * or auto-increment schema columns are not yet unassigned values from the db.
   */
  default boolean canBeNull()
  {
    return isNullable() || isGenerated() || isAutoIncrement() ||
      getForeignKey() != null && getForeignKey().canBeNull(); // a newly created ref could be assigned to the fk
  }

  default BaseElement getForeignKey()
  {
    return null;
  }
}
