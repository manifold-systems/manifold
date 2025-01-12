package manifold.extensions.abc.MyObject;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.Intercept;
import manifold.ext.rt.api.This;
import abc.MyObject;

@Extension
public class MyObjectExt {

    @Extension
    public static String sayHello( String firstName, String lastName )
    {
        return MyObject.sayHello(firstName + " " + lastName);
    }

    @Intercept
    @Extension
    public static String sayHello( String name )
    {
        return name == null ? "hello unknown" : MyObject.sayHello( name );
    }

    @Extension
    public static String test( String firstName )
    {
        return "test " + firstName;
    }

    @Intercept
    public static String repeatSelf( @This MyObject myObject, int times )
    {
        return times < 0  ? "negative amount" : myObject.repeatSelf( times );
    }
}
