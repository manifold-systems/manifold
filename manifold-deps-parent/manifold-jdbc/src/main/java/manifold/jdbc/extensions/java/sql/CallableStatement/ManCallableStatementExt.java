package manifold.jdbc.extensions.java.sql.CallableStatement;

import manifold.ext.api.Extension;
import manifold.ext.api.This;
import org.jetbrains.annotations.Nullable;

import java.sql.CallableStatement;
import java.sql.SQLException;

@Extension
public class ManCallableStatementExt
{

  @Nullable
  public static Integer getIntOrNull( @This CallableStatement thiz, int columnIndex ) throws SQLException
  {
    int value = thiz.getInt( columnIndex );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }

  @Nullable
  public static Integer getIntOrNull( @This CallableStatement thiz, String columnLabel ) throws SQLException
  {
    int value = thiz.getInt( columnLabel );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }

  @Nullable
  public static Long getLongOrNull( @This CallableStatement thiz, int columnIndex ) throws SQLException
  {
    long value = thiz.getLong( columnIndex );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }

  @Nullable
  public static Long getLongOrNull( @This CallableStatement thiz, String columnLabel ) throws SQLException
  {
    long value = thiz.getLong( columnLabel );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }

  @Nullable
  public static Double getDoubleOrNull( @This CallableStatement thiz, int columnIndex ) throws SQLException
  {
    double value = thiz.getDouble( columnIndex );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }

  @Nullable
  public static Double getDoubleOrNull( @This CallableStatement thiz, String columnLabel ) throws SQLException
  {
    double value = thiz.getDouble( columnLabel );
    if( thiz.wasNull() )
    {
      return null;
    }
    return value;
  }
}
