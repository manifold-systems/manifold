package extensions.java.util.ArrayList;

import java.util.ArrayList;
import manifold.ext.api.Extension;
import manifold.ext.api.This;

/**
 */
@Extension
public class MyArrayListExt {
    public static <E> String stuff(@This ArrayList<E> thiz )
    {
      return "ok";
    }
}
