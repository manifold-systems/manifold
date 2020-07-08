/*
 * Copyright (c) 2020 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package abc;

/**
 * Used for testing that compile-time constant field initializers are preserved in extension class code gen. Necessary
 * since the Java compiler substitutes the field refs with the constant values.
 */
public class ClassWithConstants
{
  public static final boolean BOOL_VALUE1 = true;
  public static final boolean BOOL_VALUE2 = false;
  public static final boolean BOOL_VALUE3 = same(true);

  public static final byte BYTE_VALUE1 = 10;
  public static final byte BYTE_VALUE2 = same(Byte.MAX_VALUE);

  public static final short SHORT_VALUE1 = 1000;
  public static final short SHORT_VALUE2 = -1000;
  public static final short SHORT_VALUE3 = same(Short.MAX_VALUE);

  public static final int INT_VALUE1 = 32768;
  public static final int INT_VALUE2 = -32769;
  public static final int INT_VALUE3 = Integer.MAX_VALUE;
  public static final int INT_VALUE4 = same(Integer.MAX_VALUE);

  public static final long LONG_VALUE1 = 8575799808933029326L;
  public static final long LONG_VALUE2 = same(LONG_VALUE1);

  public static final float FLOAT_VALUE1 = 3.4028235e+38f;
  public static final float FLOAT_VALUE2 = Float.MIN_VALUE;
  public static final float FLOAT_VALUE3 = same(Float.MIN_VALUE);

  public static final double DOUBLE_VALUE1 = 1.7976931348623157e+308;
  public static final double DOUBLE_VALUE2 = same(Double.MAX_VALUE);

  public static final char CHAR_VALUE1 = 's';
  public static final char CHAR_VALUE2 = '\n';
  public static final char CHAR_VALUE3 = '\u263A'; // smiley
  public static final char CHAR_VALUE4 = same('\u263A');

  public static final String STRING_VALUE0 = null;
  public static final String STRING_VALUE1 = "";
  public static final String STRING_VALUE2 = "abc";
  public static final String STRING_VALUE3 = "\u263Aabc\u263A\ndef";
  public static final String STRING_VALUE4 = same("\u263Aabc\u263A\ndef");

  public static final ClassWithConstants OBJ_VALUE1 = null;
  public static final ClassWithConstants OBJ_VALUE2 = new ClassWithConstants();

  // facilitates testing non-compile-time constant values
  private static <E> E same(E o)
  {
    return o;
  }
}
