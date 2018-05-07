package manifold.api.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.SOURCE)
public @interface Precompiles
{
  Precompile[] value();
}
