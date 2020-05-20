package abc;

import java.util.List;
import manifold.ext.rt.api.Structural;

/**
 */
@Structural
public interface IGenericThing<T extends CharSequence>
{
  <E extends List<T>> E foo( T t, E e );
}
