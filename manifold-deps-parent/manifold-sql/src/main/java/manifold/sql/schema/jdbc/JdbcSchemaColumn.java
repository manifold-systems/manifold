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

package manifold.sql.schema.jdbc;

import manifold.sql.schema.api.SchemaColumn;
import manifold.sql.schema.jdbc.oneoff.SqliteTypeMapping;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcSchemaColumn implements SchemaColumn
{
  private final JdbcSchemaTable _table;
  private final int _position;
  private final String _name;
  private final int _jdbcType;
  private final boolean _isNullable;
  private final boolean _isAutoIncrement;
  private final boolean _isGenerated;
  private final boolean _isPrimaryKeyPart;
  private final boolean _isId;
  private final String _defaultValue;
  private final int _decimalDigits;
  private final int _numPrecRadix;
  private JdbcSchemaColumn _fk;
  private final int _size;

  public JdbcSchemaColumn( int colIndex, JdbcSchemaTable jdbcSchemaTable, ResultSet rs, List<String> primaryKey ) throws SQLException
  {
    _position = colIndex;
    _table = jdbcSchemaTable;
    _name = rs.getString( "COLUMN_NAME" );
    _jdbcType = oneOffCorrections( rs.getInt( "DATA_TYPE" ), rs, _table.getSchema().getDatabaseProductName() );
    _isNullable = rs.getInt( "NULLABLE" ) == DatabaseMetaData.columnNullable;
    _isAutoIncrement = "YES".equals( rs.getString( "IS_AUTOINCREMENT" ) );
    _isGenerated = "YES".equals( rs.getString( "IS_GENERATEDCOLUMN" ) );
    _isPrimaryKeyPart = primaryKey.contains( _name );
    _isId = _isPrimaryKeyPart && primaryKey.size() == 1;
    _defaultValue = rs.getString( "COLUMN_DEF" );
    _size = rs.getInt( "COLUMN_SIZE" );
    _decimalDigits = rs.getInt( "DECIMAL_DIGITS" );
    _numPrecRadix = rs.getInt( "NUM_PREC_RADIX" );
  }

  private int oneOffCorrections( int jdbcType, ResultSet rs, String productName ) throws SQLException
  {
    Integer corrected = new SqliteTypeMapping().getJdbcType( productName, rs );
    return corrected != null ? corrected : jdbcType;
  }

  @Override
  public JdbcSchemaTable getTable()
  {
    return _table;
  }

  @Override
  public int getPosition()
  {
    return _position;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public boolean isNullable()
  {
    return _isNullable;
  }

  @Override
  public int getJdbcType()
  {
    return _jdbcType;
  }

  @Override
  public boolean isId()
  {
    return _isId;
  }

  @Override
  public boolean isPrimaryKeyPart()
  {
    return _isPrimaryKeyPart;
  }

  @Override
  public boolean isAutoIncrement()
  {
    return _isAutoIncrement;
  }

  @Override
  public boolean isGenerated()
  {
    return _isGenerated;
  }

  @Override
  public String getDefaultValue()
  {
    return _defaultValue;
  }

  public JdbcSchemaColumn getForeignKey()
  {
    return _fk;
  }
  void setForeignKey( JdbcSchemaColumn fk )
  {
    _fk = fk;
  }

  @Override
  public int getSize()
  {
    return _size;
  }

  @Override
  public int getScale()
  {
    return _decimalDigits;
  }

  @Override
  public int getNumPrecRadix()
  {
    return _numPrecRadix;
  }
}
