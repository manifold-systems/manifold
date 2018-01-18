package manifold.api.host;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A class annotated with this will not have the Manifold bootstrap
 * class initialization block injected.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface NoBootstrap
{
}
