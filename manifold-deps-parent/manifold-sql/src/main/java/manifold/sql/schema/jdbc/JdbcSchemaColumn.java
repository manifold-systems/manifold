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

import manifold.sql.rt.api.Dependencies;
import manifold.sql.rt.api.TypeProvider;
import manifold.sql.rt.util.DbUtil;
import manifold.sql.schema.api.SchemaColumn;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JdbcSchemaColumn implements SchemaColumn
{
  private final JdbcSchemaTable _table;
  private final int _position;
  private final String _name;
  private final String _escapedName;
  private int _jdbcType;
  private final String _sqlType;
  private final boolean _isNullable;
  private final boolean _isAutoIncrement;
  private final boolean _isGenerated;
  private final boolean _isPrimaryKeyPart;
  private final String _nonNullUniqueKeyName;
  private final boolean _isNonNullUniqueId;
  private final String _defaultValue;
  private final int _decimalDigits;
  private final int _numPrecRadix;
  private String _columnType;
  private JdbcSchemaColumn _fk;
  private final int _size;

  public JdbcSchemaColumn( int colIndex, JdbcSchemaTable jdbcSchemaTable, ResultSet rs, List<String> primaryKey,
                           Map<String, Set<String>> uniqueKeys, String columnType, DatabaseMetaData dbMetadata ) throws SQLException
  {
    _position = colIndex;
    _table = jdbcSchemaTable;
    _name = rs.getString( "COLUMN_NAME" );
    _escapedName= DbUtil.enquoteIdentifier(_name, dbMetadata);
    _isNullable = rs.getInt( "NULLABLE" ) == DatabaseMetaData.columnNullable;
    _isAutoIncrement = "YES".equalsIgnoreCase( rs.getString( "IS_AUTOINCREMENT" ) );
    _isGenerated = "YES".equalsIgnoreCase( rs.getString( "IS_GENERATEDCOLUMN" ) );
    _isPrimaryKeyPart = primaryKey.contains( _name );
    _nonNullUniqueKeyName = uniqueKeys.entrySet().stream()
      .filter( e -> e.getValue().contains( _name ) )
      .map( e -> e.getKey() )
      .findFirst().orElse( null );
    boolean isNonNullUniqueSoloKey = uniqueKeys.values().stream().anyMatch( cols -> cols.contains( _name ) && cols.size() == 1 );
    _isNonNullUniqueId = _isPrimaryKeyPart && primaryKey.size() == 1 || isNonNullUniqueSoloKey;
    _defaultValue = rs.getString( "COLUMN_DEF" );
    _size = rs.getInt( "COLUMN_SIZE" );
    _decimalDigits = rs.getInt( "DECIMAL_DIGITS" );
    _numPrecRadix = rs.getInt( "NUM_PREC_RADIX" );
    TypeProvider typeProvider = Dependencies.instance().getTypeProvider();
    _jdbcType = getSchemaColumnType( rs, dbMetadata, typeProvider, columnType );
    _sqlType = rs.getString( "TYPE_NAME" );
    _columnType = columnType;
  }

  private int getSchemaColumnType( ResultSet rs, DatabaseMetaData dbMetadata, TypeProvider typeProvider, String columnType ) throws SQLException
  {
    if( columnType.equals( SQLXML.class.getTypeName() ) )
    {
      // db2 tries to use its own class, we override that to use SQLXML, here we ensure the jdbc type is consistent with that
      return Types.SQLXML;
    }
    return typeProvider.getSchemaColumnType( _isNonNullUniqueId, rs, dbMetadata );
  }

  @Override
  public JdbcSchemaTable getOwner()
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
  public String getEscapedName()
  {
    return _escapedName;
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
  public String getSqlType()
  {
    return _sqlType;
  }

  public String getColumnClassName()
  {
    return _columnType;
  }

  @Override
  public boolean isNonNullUniqueId()
  {
    return _isNonNullUniqueId;
  }

  @Override
  public boolean isPrimaryKeyPart()
  {
    return _isPrimaryKeyPart;
  }

  @Override
  public String getNonNullUniqueKeyName()
  {
    return _nonNullUniqueKeyName;
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
    if( _fk != null )
    {
      // fk type must match pk type, otherwise
      // e.g., oracle, there can be Number/BigDecimal fk types trying to compare with Number(10)/java.lang.Integer pk types etc.
      _jdbcType = _fk._jdbcType;
      _columnType = _fk._columnType;
    }
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

  @Override
  public boolean canBeNull()
  {
    // oracle true to form returns NO for "IS_AUTOINCREMENT" even when the column is GENERATED ALWAYS AS IDENTITY,
    // therefore we have to settle and treat all non-null unique ids as auto-increment, which are 99% of the time :\
    return isNonNullUniqueId() || getForeignKey() != this && SchemaColumn.super.canBeNull();
  }
}
