package manifold.ext;

import junit.framework.TestCase;
import abc.MyObject;

public class InterceptTest extends TestCase
{

    public void testInterceptStaticMethod()
    {
        String name = null;
        // intercept null name
        assertEquals( "hello unknown", MyObject.sayHello( name ) );
        // delegate non-null name to MyObject method
        assertEquals( "hello scott", MyObject.sayHello( "scott" ) );
    }

    public void testNonInterceptedStaticMethod()
    {
        // call static method
        assertEquals( "hello John Doe", MyObject.sayHello( "John", "Doe" ) );
    }

    public void testInterceptNonStaticMethod()
    {
        MyObject myObject = new MyObject( "12-" );
        // intercept, followed by call to static method
        assertEquals( "12-12-", myObject.repeatSelf( 2 ) );
        // intercept negative amount
        assertEquals( "negative amount", myObject.repeatSelf( -1 ) );
    }

    public void testTest()
    {
        // non-intercepted extension method
        assertEquals( "test 123", MyObject.test( "123" ) );
    }
}
