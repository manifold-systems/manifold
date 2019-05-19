package manifold.jdbc.extensions.java.sql.ResultSet;

import manifold.ext.api.Extension;
import manifold.ext.api.This;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;

@Extension
public class ManResultSetExt
{

  @Nullable
  public static Integer getIntOrNull( @This ResultSet thiz, int columnIndex ) throws SQLException
  {
    int value = thiz.getInt( columnIndex );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }

  @Nullable
  public static Integer getIntOrNull( @This ResultSet thiz, String columnLabel ) throws SQLException
  {
    int value = thiz.getInt( columnLabel );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }

  @Nullable
  public static Long getLongOrNull( @This ResultSet thiz, int columnIndex ) throws SQLException
  {
    long value = thiz.getLong( columnIndex );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }

  @Nullable
  public static Long getLongOrNull( @This ResultSet thiz, String columnLabel ) throws SQLException
  {
    long value = thiz.getLong( columnLabel );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }

  @Nullable
  public static Double getDoubleOrNull( @This ResultSet thiz, int columnIndex ) throws SQLException
  {
    double value = thiz.getDouble( columnIndex );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }

  @Nullable
  public static Double getDoubleOrNull( @This ResultSet thiz, String columnLabel ) throws SQLException
  {
    double value = thiz.getDouble( columnLabel );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }
}
