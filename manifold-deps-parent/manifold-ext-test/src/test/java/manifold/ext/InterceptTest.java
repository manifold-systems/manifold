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

    public void testInterceptSuperMethod()
    {
        InterceptTest.InterceptObjectSub interceptObjectSub = new InterceptTest.InterceptObjectSub( "12-" );
        // Steps:
        // 1. interceptObjectSub increases the number by one,
        // 2. next the extension method is called,
        // 3. and finally the repeatSelf method if the InterceptObject class
        assertEquals( "12-12-", interceptObjectSub.repeatSelf( 1 ) );
        // intercept negative amount
        assertEquals( "negative amount", interceptObjectSub.repeatSelf( -2 ) );
    }

    public void testInterceptMethodThisNotFirstAnnotation(){
        InterceptTest.InterceptObjectSub interceptObjectSub = null;
        assertEquals( null, interceptObjectSub.foo() );
        assertEquals( "foo",  new InterceptTest.InterceptObjectSub( "" ).foo() );
    }

    public static class InterceptObjectSub extends InterceptObject {
        public InterceptObjectSub(String text) {
            super(text);
        }

        public String repeatSelf(int times)
        {
            return super.repeatSelf( times + 1 );
        }
    }
}
