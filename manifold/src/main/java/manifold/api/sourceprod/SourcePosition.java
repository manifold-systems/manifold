package manifold.api.sourceprod;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used for a ISourceProducer to map a generated Java
 * feature to the corresponding resource file location.  For example, an
 * IDE can use this annotation to implement a "Go to declaration" feature.
 * <p/>
 * Note the constant fields mirror the annotation's method names and are
 * used to access the values of the annotation during compile-time.  They
 * exist primarily as a stopgap until Java gets its act together an provides
 * method literals to safely access method names.
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.SOURCE)
public @interface SourcePosition
{
  String URL = "url";

  String url();

  String OFFSET = "offset";

  int offset() default -1;

  String LENGTH = "length";

  int length() default -1;

  String TYPE = "type";

  String type() default "";

  String FEATURE = "feature";

  String feature();

  String LINE = "line";

  int line() default -1;
}
