package manifold.ext.extensions.java.lang.String;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import org.jspecify.annotations.Nullable;

import java.util.function.Function;

/**
 */
@Extension
public class MyGenericAnnotatedTypeVarsStringExt
{
  public static <T extends @Nullable Object> T test1( @This String text, Function<String, T> mapper) {
    return mapper.apply(text);
  }

  public static <T> T test2(@This String text, Function<String, @Nullable T> mapper) {
    return mapper.apply(text);
  }
}
