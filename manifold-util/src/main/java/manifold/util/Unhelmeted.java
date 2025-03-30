package manifold.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static manifold.util.JdkAccessUtil.useInternalUnsafe;

public class Unhelmeted
{
  private static Unhelmeted UNHELMETED = null;
  private static Object UNSAFE = null;

  public static Unhelmeted getUnhelmeted()
  {
    return UNHELMETED == null ? UNHELMETED = new Unhelmeted() : UNHELMETED;
  }

  private static Object getUnsafe()
  {
    if( UNSAFE != null )
    {
      return UNSAFE;
    }

    try
    {
      if( useInternalUnsafe() )
      {
        Field theUnsafe = Unsafe.class.getDeclaredField( "theInternalUnsafe" );
        theUnsafe.setAccessible( true );
        return UNSAFE = theUnsafe.get( null );
//        return UNSAFE = Class.forName( "jdk.internal.misc.Unsafe" ).getMethod( "getUnsafe" ).invoke( null );
      }
      else
      {
        Field theUnsafe = Unsafe.class.getDeclaredField( "theUnsafe" );
        theUnsafe.setAccessible( true );
        return UNSAFE = theUnsafe.get( null );
      }
    }
    catch( Throwable t )
    {
      throw new RuntimeException( "The 'Unsafe' class is not accessible" );
    }
  }

  public void putObjectVolatile( Object o, long offset, Object x )
  {
    if( useInternalUnsafe() )
    {
      try
      {
        Method objectFieldOffset = getUnsafe().getClass().getMethod( useInternalUnsafe()
                                                                     ? "putReferenceVolatile"
                                                                     : "putObjectVolatile", Object.class, long.class, Object.class );
        objectFieldOffset.setAccessible( true );
        objectFieldOffset.invoke( UNSAFE, o, offset, x );
      }
      catch( Throwable e )
      {
        throw new RuntimeException( e );
      }
    }
    else
    {
      ((Unsafe)getUnsafe()).putObjectVolatile( o, offset, x );
    }
  }

  public void putBooleanVolatile( Object o, long offset, boolean x )
  {
    if( useInternalUnsafe() )
    {
      try
      {
        Method putBooleanVolatile = getUnsafe().getClass().getMethod( "putBooleanVolatile", Object.class, long.class, boolean.class );
        putBooleanVolatile.setAccessible( true );
        putBooleanVolatile.invoke( UNSAFE, o, offset, x );
//      IUnsafe.instance().putBooleanVolatile( getUnsafe(), o, offset, x );
      }
      catch( Throwable e )
      {
        throw new RuntimeException( e );
      }
    }
    else
    {
      ((Unsafe)getUnsafe()).putBooleanVolatile( o, offset, x );
    }

  }

  public long staticFieldOffset( Field field )
  {
    if( useInternalUnsafe() )
    {
      try
      {
        Method objectFieldOffset = getUnsafe().getClass().getMethod( "staticFieldOffset", Field.class );
        objectFieldOffset.setAccessible( true );
        return (long)objectFieldOffset.invoke( UNSAFE, field );
      }
      catch( Throwable e )
      {
        throw new RuntimeException( e );
      }
    }
    else
    {
      return ((Unsafe)getUnsafe()).staticFieldOffset( field );
    }
  }

  public long objectFieldOffset( Field field )
  {
    if( useInternalUnsafe() )
    {
      try
      {
        Method objectFieldOffset = getUnsafe().getClass().getMethod( "objectFieldOffset", Field.class );
        objectFieldOffset.setAccessible( true );
        return (long)objectFieldOffset.invoke( UNSAFE, field );
      }
      catch( Throwable e )
      {
        throw new RuntimeException( e );
      }
    }
    else
    {
      return ((Unsafe)getUnsafe()).objectFieldOffset( field );
    }
  }

}
