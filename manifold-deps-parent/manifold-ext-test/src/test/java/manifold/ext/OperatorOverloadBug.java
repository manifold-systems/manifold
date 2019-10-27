/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.ext;

import org.junit.Test;


import static org.junit.Assert.assertEquals;

public class OperatorOverloadBug
{
  @Test
  public void testLargeStringOfOperations()
  {
    final String a1 = "a";
    final String a2 = "b";
    final String a3 = "c";
    final String a4 = "d";
    final String a5 = "e";
    final String a6 = "f";
    final String a7 = "g";
    final String a8 = "h";
    final String a9 = "i";
    final String a10 = "j";
    final String a11 = "k";
    final String a12 = "l";

    final String b1 = "m";
    final String b2 = "n";
    final String b3 = "o";
    final String b4 = "p";
    final String b5 = "q";
    final String b6 = "r";
    final String b7 = "s";
    final String b8 = "t";
    final String b9 = "u";
    final String b10 = "v";
    final String b11 = "w";
    final String b12 = "x";
    final String b13 = "y";
    final String b14 = "z";

    assertEquals( "abcdefghijklmnopqrstuvwxyz",
      a1 + a2 + a3 + a4 + a5 + a6 + a7 + a8 + a9 + a10 + a11 + a12 +
      b1 + b2 + b3 + b4 + b5 + b6 + b7 + b8 + b9 + b10 + b11 + b12 + b13 + b14 );
  }

}