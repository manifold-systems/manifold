package manifold.sql.schema.jdbc.oneoff;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

/**
 * A lightweight ResultSet intended for metadata API implementation use.
 */
public class LightResultSet implements ResultSet
{
  private final List<String> _columns;
  private final Map<String, List<Object>> _table;
  private int _rowCount;
  private int _csr;


  public LightResultSet( List<String> columns )
  {
    _columns = new ArrayList<>( columns );
    _table = new LinkedHashMap<>();
    columns.forEach( name -> _table.put( name, new ArrayList<>() ) );
    _rowCount = 0;
    _csr = -1;
  }

  public void addRow( Map<String, Object> row ) throws SQLException
  {
    for( Map.Entry<String, Object> entry : row.entrySet() )
    {
      List<Object> column = _table.get( entry.getKey() );
      if( column == null )
      {
        throw new SQLException( "No such column: " + entry.getKey() );
      }
      column.add( entry.getValue() );
    }
    _rowCount++;
  }

  @Override
  public boolean next() throws SQLException
  {
    if( _csr >= _rowCount - 1 )
    {
      if( _csr == _rowCount - 1 )
      {
        _csr++; // position to impl isAfterLast()
      }
      return false;
    }
    _csr++;
    return true;
  }

  @Override
  public void close() throws SQLException
  {
  }

  @Override
  public boolean wasNull() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public String getString( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getString( _columns.get( columnIndex - 1 ) );
  }

  private void columnIndexCheck( int columnIndex ) throws SQLException
  {
    if( columnIndex <= 0 )
    {
      throw new SQLException( "Invalid columnIndex: " + columnIndex );
    }
    if( columnIndex > _table.size() )
    {
      throw new SQLException(
        "columnIndex > table size: columnIndex: " + columnIndex + ", table size: " + _table.size() );
    }
  }

  @Override
  public boolean getBoolean( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getBoolean( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public byte getByte( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getByte( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public short getShort( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getShort( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public int getInt( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getInt( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public long getLong( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getLong( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public float getFloat( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getFloat( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public double getDouble( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getDouble( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public BigDecimal getBigDecimal( int columnIndex, int scale ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getBigDecimal( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public byte[] getBytes( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getBytes( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public Date getDate( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getDate( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public Time getTime( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getTime( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public Timestamp getTimestamp( int columnIndex ) throws SQLException
  {
    columnIndexCheck( columnIndex );
    return getTimestamp( _columns.get( columnIndex - 1 ) );
  }

  @Override
  public InputStream getAsciiStream( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public InputStream getUnicodeStream( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public InputStream getBinaryStream( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public String getString( String columnLabel ) throws SQLException
  {
    Object value = getColumnValue( columnLabel );
    if( value == null || value instanceof String )
    {
      return (String)value;
    }
    else
    {
      return String.valueOf( value );
    }
  }

  private Object getColumnValue( String columnLabel ) throws SQLException
  {
    List<Object> column = _table.get( columnLabel );
    if( column == null )
    {
      throw new SQLException( "No such column: '" + columnLabel + "'" );
    }
    return column.get( _csr );
  }

  @Override
  public boolean getBoolean( String columnLabel ) throws SQLException
  {
    Object value = getColumnValue( columnLabel );
    if( value == null )
    {
      throw new SQLException( "Expecting primitive value but found null" );
    }
    else if( value instanceof Boolean )
    {
      return (Boolean)value;
    }
    throw new SQLException( columnLabel + " is not a boolean value" );
  }

  @Override
  public byte getByte( String columnLabel ) throws SQLException
  {
    return (byte)getInt( columnLabel );
  }

  @Override
  public short getShort( String columnLabel ) throws SQLException
  {
    return (short)getInt( columnLabel );
  }

  @Override
  public int getInt( String columnLabel ) throws SQLException
  {
    return (int)getLong( columnLabel );
  }

  @Override
  public long getLong( String columnLabel ) throws SQLException
  {
    Object value = getColumnValue( columnLabel );
    if( value instanceof Number )
    {
      return ((Number)value).longValue();
    }
    else
    {
      try
      {
        return Long.parseLong( value.toString() );
      }
      catch( NumberFormatException nfe )
      {
        throw new SQLException( nfe );
      }
    }
  }

  @Override
  public float getFloat( String columnLabel ) throws SQLException
  {
    Object value = getColumnValue( columnLabel );
    if( value instanceof Number )
    {
      return ((Number)value).floatValue();
    }
    else
    {
      try
      {
        return Float.parseFloat( value.toString() );
      }
      catch( NumberFormatException nfe )
      {
        throw new SQLException( nfe );
      }
    }
  }

  @Override
  public double getDouble( String columnLabel ) throws SQLException
  {
    Object value = getColumnValue( columnLabel );
    if( value instanceof Number )
    {
      return ((Number)value).doubleValue();
    }
    else
    {
      try
      {
        return Double.parseDouble( value.toString() );
      }
      catch( NumberFormatException nfe )
      {
        throw new SQLException( nfe );
      }
    }
  }

  @Override
  public BigDecimal getBigDecimal( String columnLabel, int scale ) throws SQLException
  {
    Object value = getColumnValue( columnLabel );
    if( value == null || value instanceof BigDecimal )
    {
      return (BigDecimal)value;
    }
    else
    {
      try
      {
        return new BigDecimal( value.toString() );
      }
      catch( NumberFormatException nfe )
      {
        throw new SQLException( nfe );
      }
    }
  }

  @Override
  public byte[] getBytes( String columnLabel ) throws SQLException
  {
    return (byte[])getColumnValue( columnLabel );
  }

  @Override
  public Date getDate( String columnLabel ) throws SQLException
  {
    return (Date)getColumnValue( columnLabel );
  }

  @Override
  public Time getTime( String columnLabel ) throws SQLException
  {
    return (Time)getColumnValue( columnLabel );
  }

  @Override
  public Timestamp getTimestamp( String columnLabel ) throws SQLException
  {
    return (Timestamp)getColumnValue( columnLabel );
  }

  @Override
  public InputStream getAsciiStream( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public InputStream getUnicodeStream( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public InputStream getBinaryStream( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public SQLWarning getWarnings() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void clearWarnings() throws SQLException
  {

  }

  @Override
  public String getCursorName() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public ResultSetMetaData getMetaData() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Object getObject( int columnIndex ) throws SQLException
  {
    return null;
  }

  @Override
  public Object getObject( String columnLabel ) throws SQLException
  {
    return getColumnValue( columnLabel );
  }

  @Override
  public int findColumn( String columnLabel ) throws SQLException
  {
    int i = _columns.indexOf( columnLabel );
    if( i < 0 )
    {
      throw new SQLException( "No such column: " + columnLabel );
    }
    return i + 1;
  }

  @Override
  public Reader getCharacterStream( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Reader getCharacterStream( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public BigDecimal getBigDecimal( int columnIndex ) throws SQLException
  {
    return null;
  }

  @Override
  public BigDecimal getBigDecimal( String columnLabel ) throws SQLException
  {
    return (BigDecimal)getColumnValue( columnLabel );
  }

  @Override
  public boolean isBeforeFirst() throws SQLException
  {
    return _csr < 0;
  }

  @Override
  public boolean isAfterLast() throws SQLException
  {
    return _csr > _rowCount - 1;
  }

  @Override
  public boolean isFirst() throws SQLException
  {
    return _csr == 0;
  }

  @Override
  public boolean isLast() throws SQLException
  {
    return _csr == _rowCount - 1;
  }

  @Override
  public void beforeFirst() throws SQLException
  {
    _csr = -1;
  }

  @Override
  public void afterLast() throws SQLException
  {
    _csr = _rowCount;
  }

  @Override
  public boolean first() throws SQLException
  {
    _csr = 0;
    return isAfterLast();
  }

  @Override
  public boolean last() throws SQLException
  {
    _csr = _rowCount - 1;
    return _csr >= 0;
  }

  @Override
  public int getRow() throws SQLException
  {
    return _csr + 1;
  }

  @Override
  public boolean absolute( int row ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean relative( int rows ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean previous() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void setFetchDirection( int direction ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int getFetchDirection() throws SQLException
  {
    return FETCH_FORWARD;
  }

  @Override
  public void setFetchSize( int rows ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int getFetchSize() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int getType() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int getConcurrency() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean rowUpdated() throws SQLException
  {
    return false;
  }

  @Override
  public boolean rowInserted() throws SQLException
  {
    return false;
  }

  @Override
  public boolean rowDeleted() throws SQLException
  {
    return false;
  }

  @Override
  public void updateNull( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBoolean( int columnIndex, boolean x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateByte( int columnIndex, byte x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateShort( int columnIndex, short x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateInt( int columnIndex, int x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateLong( int columnIndex, long x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateFloat( int columnIndex, float x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateDouble( int columnIndex, double x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBigDecimal( int columnIndex, BigDecimal x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateString( int columnIndex, String x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBytes( int columnIndex, byte[] x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateDate( int columnIndex, Date x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateTime( int columnIndex, Time x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateTimestamp( int columnIndex, Timestamp x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateAsciiStream( int columnIndex, InputStream x, int length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBinaryStream( int columnIndex, InputStream x, int length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateCharacterStream( int columnIndex, Reader x, int length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateObject( int columnIndex, Object x, int scaleOrLength ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateObject( int columnIndex, Object x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNull( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBoolean( String columnLabel, boolean x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateByte( String columnLabel, byte x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateShort( String columnLabel, short x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateInt( String columnLabel, int x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateLong( String columnLabel, long x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateFloat( String columnLabel, float x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateDouble( String columnLabel, double x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBigDecimal( String columnLabel, BigDecimal x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateString( String columnLabel, String x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBytes( String columnLabel, byte[] x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateDate( String columnLabel, Date x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateTime( String columnLabel, Time x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateTimestamp( String columnLabel, Timestamp x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateAsciiStream( String columnLabel, InputStream x, int length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBinaryStream( String columnLabel, InputStream x, int length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateCharacterStream( String columnLabel, Reader reader, int length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateObject( String columnLabel, Object x, int scaleOrLength ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateObject( String columnLabel, Object x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void insertRow() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateRow() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void deleteRow() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void refreshRow() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void cancelRowUpdates() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void moveToInsertRow() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void moveToCurrentRow() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Statement getStatement() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Object getObject( int columnIndex, Map<String, Class<?>> map ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Ref getRef( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Blob getBlob( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Clob getClob( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Array getArray( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Object getObject( String columnLabel, Map<String, Class<?>> map ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Ref getRef( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Blob getBlob( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Clob getClob( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Array getArray( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Date getDate( int columnIndex, Calendar cal ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Date getDate( String columnLabel, Calendar cal ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Time getTime( int columnIndex, Calendar cal ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Time getTime( String columnLabel, Calendar cal ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Timestamp getTimestamp( int columnIndex, Calendar cal ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Timestamp getTimestamp( String columnLabel, Calendar cal ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public URL getURL( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public URL getURL( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateRef( int columnIndex, Ref x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateRef( String columnLabel, Ref x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBlob( int columnIndex, Blob x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBlob( String columnLabel, Blob x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateClob( int columnIndex, Clob x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateClob( String columnLabel, Clob x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateArray( int columnIndex, Array x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateArray( String columnLabel, Array x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public RowId getRowId( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public RowId getRowId( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateRowId( int columnIndex, RowId x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateRowId( String columnLabel, RowId x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int getHoldability() throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean isClosed() throws SQLException
  {
    return false;
  }

  @Override
  public void updateNString( int columnIndex, String nString ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNString( String columnLabel, String nString ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNClob( int columnIndex, NClob nClob ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNClob( String columnLabel, NClob nClob ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public NClob getNClob( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public NClob getNClob( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public SQLXML getSQLXML( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public SQLXML getSQLXML( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateSQLXML( int columnIndex, SQLXML xmlObject ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateSQLXML( String columnLabel, SQLXML xmlObject ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public String getNString( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public String getNString( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Reader getNCharacterStream( int columnIndex ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Reader getNCharacterStream( String columnLabel ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNCharacterStream( int columnIndex, Reader x, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNCharacterStream( String columnLabel, Reader reader, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateAsciiStream( int columnIndex, InputStream x, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBinaryStream( int columnIndex, InputStream x, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateCharacterStream( int columnIndex, Reader x, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateAsciiStream( String columnLabel, InputStream x, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBinaryStream( String columnLabel, InputStream x, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateCharacterStream( String columnLabel, Reader reader, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBlob( int columnIndex, InputStream inputStream, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBlob( String columnLabel, InputStream inputStream, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateClob( int columnIndex, Reader reader, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateClob( String columnLabel, Reader reader, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNClob( int columnIndex, Reader reader, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNClob( String columnLabel, Reader reader, long length ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNCharacterStream( int columnIndex, Reader x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNCharacterStream( String columnLabel, Reader reader ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateAsciiStream( int columnIndex, InputStream x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBinaryStream( int columnIndex, InputStream x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateCharacterStream( int columnIndex, Reader x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateAsciiStream( String columnLabel, InputStream x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBinaryStream( String columnLabel, InputStream x ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateCharacterStream( String columnLabel, Reader reader ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBlob( int columnIndex, InputStream inputStream ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateBlob( String columnLabel, InputStream inputStream ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateClob( int columnIndex, Reader reader ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateClob( String columnLabel, Reader reader ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNClob( int columnIndex, Reader reader ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void updateNClob( String columnLabel, Reader reader ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public <T> T getObject( int columnIndex, Class<T> type ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public <T> T getObject( String columnLabel, Class<T> type ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public <T> T unwrap( Class<T> iface ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean isWrapperFor( Class<?> iface ) throws SQLException
  {
    throw new SQLFeatureNotSupportedException();
  }
}
