package manifold.ext.extensions.java.lang.StringBuffer;

import manifold.ext.api.Extension;
import manifold.ext.api.This;

/**
 */
@Extension
public class MyErrantStringBufferExt
{
  public static void thisIsOk( @This StringBuffer thiz ) {}

//  // error, overloads method in StringBuffer
//  public static void append( @This StringBuffer thiz ) {}
//
//  // error, overloads method in CharSequence
//  public static void chars( @This StringBuffer thiz ) {}
}
