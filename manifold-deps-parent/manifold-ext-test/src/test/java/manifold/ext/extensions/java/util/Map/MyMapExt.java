package manifold.ext.extensions.java.util.Map;

import java.util.Map;
import manifold.ext.api.Extension;
import manifold.ext.api.Self;
import manifold.ext.api.This;

@Extension
public class MyMapExt
{
  public static <K,V> @Self Map<K,V> add( @This Map<K,V> thiz, K k, V v )
  {
    thiz.put( k, v );
    return thiz;
  }
}
