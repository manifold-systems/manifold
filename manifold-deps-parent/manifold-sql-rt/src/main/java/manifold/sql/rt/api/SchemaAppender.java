package manifold.sql.rt.api;

import manifold.json.rt.api.DataBindings;
import manifold.util.ManExceptionUtil;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBConnection;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Consumer;

public abstract class SchemaAppender
{
  private final String _schema;
  private final String _table;
  private DuckDBAppender _appender;

  public SchemaAppender( String schema, String table )
  {
    _schema = schema;
    _table = table;
  }

  // called from generated code
  @SuppressWarnings( "unused" )
  protected void appendRow( DataBindings bindings )
  {
    try
    {
      _appender.beginRow();
      appendBindings( _appender, bindings );
      _appender.endRow();
    }
    catch( SQLException e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }

  public <T extends SchemaAppender> void execute( Connection c, Consumer<T> consumer ) throws SQLException
  {
    DuckDBConnection duckdbConnection = c.unwrap( DuckDBConnection.class );
    try( DuckDBAppender appender = duckdbConnection.createAppender( _schema, _table ) )
    {
      _appender = appender;
      //noinspection unchecked
      consumer.accept( (T)this );
    }
    catch( Exception e )
    {
      throw new SQLException( e );
    }
  }

  private void appendBindings( DuckDBAppender appender, DataBindings bindings ) throws SQLException
  {
    for( Map.Entry<String, Object> entry : bindings.entrySet() )
    {
      Object value = entry.getValue();
      if( value == null )
      {
        appender.append( null );
      }
      else if( value instanceof BigDecimal )
      {
        appender.appendBigDecimal( (BigDecimal)value );
      }
      else if( value instanceof BigInteger )
      {
        appender.append( ((BigInteger)value).longValueExact() );
      }
      else if( value instanceof Long )
      {
        appender.append( (Long)value );
      }
      else if( value instanceof Integer )
      {
        appender.append( (Integer)value );
      }
      else if( value instanceof Short )
      {
        appender.append( (Short)value );
      }
      else if( value instanceof Byte )
      {
        appender.append( (Byte)value );
      }
      else if( value instanceof Double )
      {
        appender.append( (Double)value );
      }
      else if( value instanceof Float )
      {
        appender.append( (Float)value );
      }
      else if( value instanceof Boolean )
      {
        appender.append( (Boolean)value );
      }
      else if( value instanceof LocalDateTime )
      {
        appender.appendLocalDateTime( (LocalDateTime)value );
      }
      else if( value instanceof OffsetDateTime )
      {
        appender.appendLocalDateTime( ((OffsetDateTime)value).toLocalDateTime() );
      }
      else if( value instanceof LocalDate )
      {
        appender.appendLocalDateTime( ((LocalDate)value).atStartOfDay() );
      }
      else
      {
        appender.append( value.toString() );
      }
    }
  }
}
