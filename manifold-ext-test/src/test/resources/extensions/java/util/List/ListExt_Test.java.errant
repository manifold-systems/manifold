package extensions.java.util.List;

import manifold.ext.api.Extension;
import manifold.ext.api.This;

import java.util.List;
import java.util.stream.Stream;

/**
 */
@Extension
public class ListExt_Test {
  // compile error, shadows method in super interface: Collectino#stream()
  public static <E> Stream<E> stream(@This List<E> list) { return null; }
}
