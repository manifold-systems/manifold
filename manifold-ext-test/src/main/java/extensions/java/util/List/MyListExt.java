package extensions.java.util.List;

import java.util.List;
import java.util.function.Predicate;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

/**
 */
@Extension
public class MyListExt
{
  public static <E> E first( @This List<E> thiz )
  {
    return thiz.get( 0 );
  }
  public static <E> E first( @This List<E> thiz, Predicate<E> filter )
  {
    for( E e: thiz )
    {
      if( filter.test( e ) )
      {
        return e;
      }
    }
    return null;
  }

  public static <E, F extends E> List<E> append( @This List<E> thiz, E... things )
  {
    for( E e: things )
    {
      thiz.add( e );
    }
    return thiz;
  }

  public static <E, F extends E> F findByType( @This List<E> thiz, F f )
  {
    for( E e: thiz )
    {
      if( f.getClass().isAssignableFrom( e.getClass() ) )
      {
        return (F)e;
      }
    }
    return null;
  }

  public static <E,F extends List<E>> F find(@This List<E> thiz, F f )
  {
      return f;
  }
}
