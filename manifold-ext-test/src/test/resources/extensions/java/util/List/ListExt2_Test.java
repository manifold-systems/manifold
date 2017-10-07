package extensions.java.util.List;

import manifold.ext.api.Extension;
import manifold.ext.api.This;

import java.util.List;
import java.util.stream.Stream;

/**
 */
@Extension
public class ListExt2_Test {
  // compile error, duplicates method in extended class: List#size()
  public static <E> int size( @This List<E> list ) {  return 0; }
}
