package manifold.ext.extensions.java.util.Map;

import java.util.Map;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.Self;
import manifold.ext.rt.api.This;

@Extension
public class MyMapExt
{
  public static <K,V> @Self Map<K,V> add( @This Map<K,V> thiz, K k, V v )
  {
    thiz.put( k, v );
    return thiz;
  }
}
