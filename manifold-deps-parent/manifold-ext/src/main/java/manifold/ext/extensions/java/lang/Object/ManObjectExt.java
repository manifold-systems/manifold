package manifold.ext.extensions.java.lang.Object;

import manifold.ext.api.Extension;
import manifold.ext.api.JailBreak;
import manifold.ext.api.Self;
import manifold.ext.api.This;

@Extension
public class ManObjectExt
{
  public static @JailBreak @Self Object jailbreak( @This Object thiz )
  {
    return thiz;
  }
}
