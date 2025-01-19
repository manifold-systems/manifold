package manifold.ext;

import abc.InterceptObject;
import junit.framework.TestCase;

public class InterceptTest extends TestCase
{

    public void testInterceptStaticMethod()
    {
        String name = null;
        // intercept null name
        assertEquals( "hello unknown", InterceptObject.sayHello( name ) );
        // delegate non-null name to InterceptObject method
        assertEquals( "hello scott", InterceptObject.sayHello( "scott" ) );
    }

    public void testNonInterceptedStaticMethod()
    {
        // call static method
        assertEquals( "hello John Doe", InterceptObject.sayHello( "John", "Doe" ) );
    }

    public void testInterceptNonStaticMethod()
    {
        InterceptObject interceptObject = new InterceptObject( "12-" );
        // intercept, followed by call to static method
        assertEquals( "12-12-", interceptObject.repeatSelf( 2 ) );
        // intercept negative amount
        assertEquals( "negative amount", interceptObject.repeatSelf( -1 ) );
    }
}
