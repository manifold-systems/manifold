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

package manifold.sql.schema.api;

import manifold.sql.api.Column;

import java.sql.Types;

public interface SchemaColumn extends Column
{
  SchemaTable getTable();
  boolean isNonNullUniqueId();
  boolean isPrimaryKeyPart();
  String getNonNullUniqueKeyName();
  boolean isAutoIncrement();
  boolean isGenerated();
  String getDefaultValue();
  SchemaColumn getForeignKey();
  int getNumPrecRadix();

  default boolean isSqliteRowId()
  {
    // a generated, auto-increment id, but since sqlite is retarded at all levels...
    return getTable().getSchema().getDatabaseProductName().equalsIgnoreCase( "sqlite" ) &&
        isNonNullUniqueId() && isPrimaryKeyPart() && getJdbcType() == Types.INTEGER;
  }
}
