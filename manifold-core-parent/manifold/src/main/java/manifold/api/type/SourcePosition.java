package manifold.api.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is used for an {@link ITypeManifold} to map a generated Java
 * feature to the corresponding resource file location.  If possible,
 * include this annotation for all generated methods and fields, as this
 * annotation is vital for most of the features in the IntelliJ plugin and
 * other IDE tooling.
 * <p/>
 * Note the constant fields mirror the annotation's method names and are
 * used to access the values of the annotation during compile-time.  They
 * exist primarily as a stopgap until Java provides method literals to
 * safely access method names...
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.SOURCE)
public @interface SourcePosition
{
  String DEFAULT_KIND = "feature";

  String URL = "url";
  /** The location of the resource containing the feature "declaration" */
  String url();

  String OFFSET = "offset";
  /** The offset of the feature declaration from the beginning of the file */
  int offset() default -1;

  String LENGTH = "length";
  /** The length of the feature declaration */
  int length() default -1;

  String TYPE = "type";
  /** The qualified type of the feature */
  String type() default "";

  String FEATURE = "feature";
  /** The name of the feature */
  String feature();

  String KIND = "kind";
  /** What kind of feature is this according to the resource's schema taxonomy? Optional. */
  String kind() default DEFAULT_KIND;

  String LINE = "line";
  /** The line where the feature begins. Optional. */
  int line() default -1;
}
