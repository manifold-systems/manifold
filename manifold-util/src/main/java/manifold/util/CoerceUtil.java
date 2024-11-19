package manifold.util;

public class CoerceUtil
{
  public static Object coerceBoxed( Object value, Class<?> type )
  {
    if( type == Boolean.class || type == boolean.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).intValue() != 0;
      }
      return Boolean.parseBoolean( value.toString() );
    }

    if( type == Byte.class || type == byte.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).byteValue() != 0;
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? (byte)1 : (byte)0;
      }
      return Byte.parseByte( value.toString() );
    }

    if( type == Character.class || type == char.class )
    {
      if( value instanceof Number )
      {
        return (char)((Number)value).intValue();
      }
      String s = value.toString();
      return s.isEmpty() ? (char)0 : s.charAt( 0 );
    }

    if( type == Short.class || type == short.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).shortValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? (short)1 : (short)0;
      }
      return Short.parseShort( value.toString() );
    }

    if( type == Integer.class || type == int.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).intValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1 : 0;
      }
      return Integer.parseInt( value.toString() );
    }

    if( type == Long.class || type == long.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).longValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1L : 0L;
      }
      return Long.parseLong( value.toString() );
    }

    if( type == Float.class || type == float.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).floatValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1f : 0f;
      }
      return Float.parseFloat( value.toString() );
    }

    if( type == Double.class || type == double.class )
    {
      if( value instanceof Number )
      {
        return ((Number)value).doubleValue();
      }
      if( value instanceof Boolean )
      {
        return ((Boolean)value) ? 1d : 0d;
      }
      return Double.parseDouble( value.toString() );
    }
    return null;
  }
}
