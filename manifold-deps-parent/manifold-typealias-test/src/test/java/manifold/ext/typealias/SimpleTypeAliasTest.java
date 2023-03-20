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

package manifold.ext.typealias;

import manifold.ext.typealias.impl.*;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

//import java.util.function.;


import static org.junit.Assert.assertEquals;

public class SimpleTypeAliasTest
{
  EnumB memberB;
  manifold.ext.typealias.impl.EnumB memberC;

  ITransformType transformType;

  @Test
  public void testSimple()
  {
    ClassD z = ClassD.NONE;
    z.testq();

    IDirection dir = IDirection.UP;
    ITransformType transformType = ITransformType.NONE;

    EnumA a = manifold.ext.typealias.impl.EnumB.NONE;
    EnumB b = EnumA.NONE;
    manifold.ext.typealias.impl.EnumB c;
    a = EnumB.NONE;
    b = EnumB.NONE;
    c = EnumB.TRUE;

    c = EnumC.FALSE;

    memberB = EnumB.NONE;

    EnumB.values();
//
    assertEquals(a, b);
    assertEquals(EnumA.class, EnumB.class);

    Consumer<EnumB> df1 = e -> {
      if (e == EnumA.NONE) {
      }
    };

    Consumer<? super EnumB> df2 = e -> {
      if (e == EnumB.NONE) {
      }
    };

    Consumer<? extends EnumB> df3 = e -> {
      if (e == EnumA.NONE) {
      }
    };

    calloutA(a);
    calloutA(b);

    calloutB(a);
    calloutB(b);

    GenericC d = new GenericC();
    d.put("a", "b");

    calloutC(d);
    calloutD(d);

    Object EnumB = "";
    EnumB.toString();

    System.out.println(memberB);
  }

//  Object EnumB = "";

  private void calloutA(EnumA e) {
  }

  private EnumB calloutB(EnumB e) {
    return e;
  }

  private void calloutC(GenericC e) {
  }

  private void calloutD(HashMap<String, String> e) {
  }

}
