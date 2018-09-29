package manifold.api.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use {@code @IncrementalCompile} to instruct Manifold to compile select resources incrementally.
 * For example, this annotation facilitates incremental compilation of changed resource files from
 * an IDE where the IDE knows which resource files have changed and need re/compile.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.SOURCE)
public @interface IncrementalCompile
{
  /**
   * The qualified name of the driver class, which must implement {@link IIncrementalCompileDriver}.
   * Note this is not a {@code Class<? extends IIncrementalCompileDriver>} because the driver
   * class is likely not in the classpath of the compiler.
   */
  String driverClass();

  /**
   * The identity hash of the driver
   */
  int driverInstance();
}
