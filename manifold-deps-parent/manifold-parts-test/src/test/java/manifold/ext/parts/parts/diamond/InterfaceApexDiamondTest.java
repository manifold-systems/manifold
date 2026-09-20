/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.ext.parts.parts.diamond;

import junit.framework.TestCase;
import manifold.ext.parts.rt.api.link;
import manifold.ext.parts.rt.api.part;

/**
 */
public class InterfaceApexDiamondTest extends TestCase
{
  public void testDiamond()
  {
    QandS qas = new QandS();
    assertEquals( "QPart.m", qas.m() );
    assertEquals( "QPart.m QPart.q", qas.q() );
    assertEquals( "QPart.m QPart.q QandS.s", qas.s() );
  }

  public void testSinglePath()
  {
    Outer outer = new Outer();
    assertEquals( "QPart.m QPart.q Middle.s", outer.s() );
  }

  public void testOuterSdoesNotWireMIntoQ()
  {
    Outer2 outer2 = new Outer2();
    assertEquals( "QPart.m QPart.q QandS.s", outer2.s() );
    assertEquals( "Outer2.m SPart.s QandS.z", outer2.z() );
  }

  public void testLinkSuperInterfaceOfPart()
  {
    Foo foo = new Foo();
    assertEquals( "Foo.m QPart.q", foo.foo() );
  }
  static class Foo implements M {
    @link M m = new QPart();
    String foo() {
      return ((QPart)m).q();
    }
    public String m() {
      return "Foo.m";
    }
  }

  interface M { String m(); }
  interface Q extends M {String z(); String q();}
  interface S extends M {String z(); String s();}

  static @part class QPart implements Q {
    public String m() { return "QPart.m"; }
    public String q() { return m() + " QPart.q"; }
    public String z() { return "QPart.z"; }
  }
  static @part class SPart implements S {
    public String m() { return "SPart.m"; }
    public String s() { return m() + " SPart.s"; }
    public String z() { return "SPart.z"; }
  }

  static @part class QandS implements Q, S {
    @link(share=M.class) Q q = new QPart();
    @link S s = new SPart();
    public String z() { return s.s() + " QandS.z"; }

    public String s() { return q() + " QandS.s"; }

//    public String foo() {
//      // Ambiguous call. 'm()' is declared in multiple interfaces of this part (Q, S),
//      // which dispatch independently and may reach different implementations: qualify with the intended interface e.g. ((Q)this).m()
//      return m(); // compile error: Ambiguous call.
//    }
  }

  static @part class Middle implements Q, S {
    @link Q q = new QPart();

    public String s() { return q() + " Middle.s"; }
  }

  static class Outer implements S {
    @link S s = new Middle();

    public String m() { return "Outer.m"; }
  }

  static class Outer2 implements S {
    @link S s = new QandS();

    public String m() { return "Outer2.m"; }
  }
}
