package extensions.java.util.List;

import manifold.ext.api.Extension;
import manifold.ext.api.This;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;

/**
 */
@Extension
public class ListExt3_Test {
  public static Stream foo(@This List list) { return null; }

  // error, @This must be first
  public static Stream thisNotFirst(int i, @This List list) { return null; }

  // error, expecting type java.util.List for @This
  public static Stream expectingList(@This ArrayList list) { return null; }

  // warning, maybe missing @This?
  public static Stream maybeMissingThis(List list) { return null; }

  // error, @This method must be static
  public Stream mustBeStaticThis(@This List list) { return null; }

  // error, @Extension method must be static
  @Extension
  public Stream mustBeStaticExtension() { return null; }

  // error, expecting type java.util.List for @This
  private static Stream mustNotBePrivate(@This List list) { return null; }

}
