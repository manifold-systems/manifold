package manifold.ext.props;

import junit.framework.TestCase;
import manifold.ext.props.rt.api.PublicDefault;
import manifold.ext.props.stuff.PublicDefaultClass;
import manifold.ext.props.stuff.PublicDefaultClass.defaultClass;

public class PublicDefaultTest  extends TestCase {
    public void testPublicDefaultVarAccess()
    {
        PublicDefaultClass defaultClass = new PublicDefaultClass(10,0,0,0);
        assertEquals(defaultClass.PublicVar, 0);
        assertEquals(defaultClass.defaultPublicVar, 10);
    }

    public void testPublicDefaultFunctionAccess()
    {
        PublicDefaultClass defaultClass = new PublicDefaultClass(10,0,0,0);
        assertEquals(defaultClass.defaultAddFunction(2,3), 5);
    }

    public void testPublicDefaultClassAccess()
    {
        PublicDefaultClass.defaultClass defaultClass = new PublicDefaultClass.defaultClass();
        assertEquals(defaultClass.reachablePublicVar, 0);
        //static is auto added
        PublicDefaultClass.publicClass publicClass = new PublicDefaultClass.publicClass();

    }
}
