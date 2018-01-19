package manifold.ext.extensions.java.util.Map;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import manifold.ext.ExtensionTransformer;
import manifold.ext.api.Extension;
import manifold.ext.api.ICallHandler;
import manifold.ext.api.This;

/**
 * Interface extension for java.util.Map to add ICallHandler support.
 */
@SuppressWarnings("unused")
@Extension
public abstract class MapStructExt implements ICallHandler
{
  @SuppressWarnings("unused")
  public static <K,V> Object call( @This Map<K,V> thiz, Class iface, String name, String actualName, Class returnType, Class[] paramTypes, Object[] args )
  {
    assert paramTypes.length == args.length;

    Object value = ICallHandler.UNHANDLED;
    if( returnType != void.class && paramTypes.length == 0 )
    {
      value = getValue( thiz, name, actualName, returnType, paramTypes, args );
    }
    if( value == ICallHandler.UNHANDLED )
    {
      if( returnType == void.class )
      {
        value = setValue( thiz, name, actualName, paramTypes, args );
      }
      if( value == ICallHandler.UNHANDLED )
      {
        value = invoke( thiz, name, returnType, paramTypes, args );
      }
    }
    if( value == ICallHandler.UNHANDLED )
    {
      value = ExtensionTransformer.invokeUnhandled( thiz, iface, name, returnType, paramTypes, args );
    }
    if( value == ICallHandler.UNHANDLED )
    {
      throw new RuntimeException( "Missing method: " + name + "(" + Arrays.toString( paramTypes ) + ")" );
    }
    return value;
  }

  private static Object getValue( Map thiz, String name, String actualName, Class returnType, Class[] paramTypes, Object[] args )
  {
    Object value;
    value = getValue( thiz, name, actualName, "get", returnType, paramTypes, args );
    if( value == ICallHandler.UNHANDLED )
    {
      value = getValue( thiz, name, actualName, "is", returnType, paramTypes, args );
    }
    return value;
  }

  private static Object getValue( Map thiz, String name, String actualName, String prefix, Class returnType, Class[] paramTypes, Object[] args )
  {
    int getLen = prefix.length();
    if( name.length() > getLen && name.startsWith( prefix ) )
    {
      char c = name.charAt( getLen );
      if( c == '_' && name.length() > getLen + 1 )
      {
        getLen++;
        c = Character.toUpperCase( name.charAt( getLen ) );
      }
      if( Character.isUpperCase( c ) )
      {
        if( actualName != null )
        {
          return thiz.get( actualName );
        }

        String key = name.substring( getLen );
        if( thiz.containsKey( key ) )
        {
          return thiz.get( key );
        }
        key = Character.toLowerCase( c ) + name.substring( 1 );
        if( thiz.containsKey( key ) )
        {
          return thiz.get( key );
        }
        return null;
      }
    }
    return ICallHandler.UNHANDLED;
  }

  private static Object setValue( Map thiz, String name, String actualName, Class[] paramTypes, Object[] args )
  {
    int setLen = "set".length();
    if( paramTypes.length == 1 && name.length() > setLen && name.startsWith( "set" ) )
    {
      char c = name.charAt( setLen );
      if( c == '_' && name.length() > setLen + 1 )
      {
        setLen++;
        c = Character.toUpperCase( name.charAt( setLen ) );
      }
      String key;
      if( Character.isUpperCase( c ) )
      {
        if( actualName != null )
        {
          key = actualName;
        }
        else
        {
          String upperKey = name.substring( setLen );
          if( thiz.containsKey( upperKey ) )
          {
            key = upperKey;
          }
          else
          {
            String lowerKey = Character.toLowerCase( c ) + name.substring( 1 );
            if( thiz.containsKey( lowerKey ) )
            {
              key = lowerKey;
            }
            else
            {
              key = upperKey;
            }
          }
        }
        //noinspection unchecked
        thiz.put( key, args[0] );
        return null;
      }
    }
    return ICallHandler.UNHANDLED;
  }

  private static Object invoke( Map thiz, String name, Class returnType, Class[] paramTypes, Object[] args )
  {
    Object value = thiz.get( name );
    if( value == null )
    {
      return ICallHandler.UNHANDLED;
    }
    try
    {
      for( Method m : value.getClass().getMethods() )
      {
        if( !m.isDefault() && !Modifier.isStatic( m.getModifiers() ) )
        {
          m.setAccessible( true );
          return m.invoke( value, args );
        }
      }
      return ICallHandler.UNHANDLED;
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }
}
