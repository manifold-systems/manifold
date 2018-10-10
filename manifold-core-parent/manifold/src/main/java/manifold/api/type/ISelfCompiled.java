package manifold.api.type;

import java.io.OutputStream;

public interface ISelfCompiled
{
  default boolean isSelfCompile()
  {
    return false;
  }

  default void compileInto( OutputStream os )
  {
    throw new UnsupportedOperationException();
  }
}
