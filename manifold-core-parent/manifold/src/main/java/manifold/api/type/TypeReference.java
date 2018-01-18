package manifold.api.type;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.SOURCE)
@Repeatable(TypeReferences.class)
public @interface TypeReference
{
  String value();
}
