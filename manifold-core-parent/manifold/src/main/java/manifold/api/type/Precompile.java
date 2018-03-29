package manifold.api.type;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use {@code @Precompile} to instruct Manifold to precompile classes from a specified type manifold.
 * This is useful for cases where a type manifold produces a static API for others to use.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Precompiles.class)
public @interface Precompile
{
  /**
   * The Type Manifold class defining the domain of types to compile from.
   */
  Class<? extends ITypeManifold> typeManifold();

  /**
   * A regular expression defining the range of types that should be compiled
   * from {@code typeManifold} via {@link ITypeManifold#getAllTypeNames()}.
   * The default value {@code "*."} compiles <i>all</i> types.
   */
  String typeNames() default ".*";
}
