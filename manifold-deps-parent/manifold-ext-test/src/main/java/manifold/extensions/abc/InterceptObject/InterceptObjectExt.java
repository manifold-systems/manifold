package manifold.extensions.abc.InterceptObject;

import abc.InterceptObject;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.Intercept;
import manifold.ext.rt.api.This;
import org.jspecify.annotations.Nullable;

@Extension
public class InterceptObjectExt
{

  @Extension
  public static String sayHello( String firstName, String lastName )
  {
    return InterceptObject.sayHello( firstName + " " + lastName );
  }

  @Intercept
  @Extension
  public static String sayHello( String name )
  {
    return name == null ? "hello unknown" : InterceptObject.sayHello( name );
  }

  @Extension
  public static String test( String firstName )
  {
    return "test " + firstName;
  }

  @Intercept
  public static String repeatSelf( @This InterceptObject interceptObject, int times )
  {
    return times < 0 ? "negative amount" : interceptObject.repeatSelf( times );
  }

  @Intercept
  public static String foo( @Nullable @This InterceptObject interceptObject )
  {
    return interceptObject == null ? null : interceptObject.foo();
  }
}
