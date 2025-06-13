package manifold.extensions.abc.ComplexGenerics;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import abc.ComplexGenerics;

@Extension
public class ComplexGenerics_Ext {
  public static void helloWorld(@This ComplexGenerics thiz) {
    System.out.println("hello world!");
  }
}