package manifold.ext.typealias;

import manifold.ext.typealias.impl.EnumA;
import manifold.ext.typealias.impl.EnumB;
import manifold.ext.typealias.impl.GenericC;
import org.junit.Test;

import java.util.HashMap;


public class ParentAliasTest
{

    @Test
    public void testSimple()
    {
    }

    void method1(EnumA value) {
        method2(value);
    }

    void method2(EnumB value) {
        method1(value);
    }

    void method3(GenericC value) {
        method4(value);
    }

    void method4(HashMap<String, String> value) {
        method3(value);
    }

  void method5(manifold.ext.typealias.impl.GenericC value) {
    method4(value);
  }
}
