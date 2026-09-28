package manifold.ext.parts.rt.api.tags;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention( RetentionPolicy.SOURCE )
public @interface provided
{
  Class<?>[] value();
}
