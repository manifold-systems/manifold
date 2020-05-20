package manifold.ext.extensions.java.util.ArrayList;

import java.util.ArrayList;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;

/**
 */
@Extension
public class MyArrayListExt {
    public static <E> String stuff(@This ArrayList<E> thiz )
    {
      return "ok";
    }
}
