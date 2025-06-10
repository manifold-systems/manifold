package manifold.extensions.abc.HasEscapedCharLiteral;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import abc.HasEscapedCharLiteral;

@Extension
public class HasEscapedCharLiteral_Ext {
  public static void helloWorld(@This HasEscapedCharLiteral thiz) {
    System.out.println("hello world!");
  }
}