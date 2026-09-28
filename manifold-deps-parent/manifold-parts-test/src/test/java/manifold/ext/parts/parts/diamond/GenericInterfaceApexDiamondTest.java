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
public class GenericInterfaceApexDiamondTest extends TestCase
{
  public void testDiamond()
  {
    QandS<String> qas = new QandS<>();
    assertEquals( "QPart.m", qas.m() );
    assertEquals( "QPart.m QPart.q", qas.q() );
    assertEquals( "QPart.m QPart.q QandS.s", qas.s() );
  }

  public void testSinglePath()
  {
    Outer<String> outer = new Outer<>();
    assertEquals( "QPart.m QPart.q Middle.s", outer.s() );
  }

  public void testOuterSdoesNotWireMIntoQ()
  {
    Outer2<String> outer2 = new Outer2<>();
    assertEquals( "QPart.m QPart.q QandS.s", outer2.s() );
    assertEquals( "Outer2.m SPart.s QandS.z", outer2.z("hi") );
  }

  public void testLinkSuperInterfaceOfPart()
  {
    Foo<String> foo = new Foo<>();
    assertEquals( "Foo.m QPart.q", foo.foo() );
  }
  static class Foo<T> implements M<T> {
    @link M<T> m = new QPart<>();
    String foo() {
      return ((QPart<T>)m).q();
    }
    public String m() {
      return "Foo.m";
    }
  }

  interface M<E> { String m(); }
  interface Q<A> extends M<A> {A z(A a); String q();}
  interface S<B> extends M<B> {B z(B b); String s();}

  static @part class QPart<T> implements Q<T> {
    public String m() { return "QPart.m"; }
    public String q() { return m() + " QPart.q"; }
    public T z(T t) { return t; }
  }
  static @part class SPart<U> implements S<U> {
    public String m() { return "SPart.m"; }
    public String s() { return m() + " SPart.s"; }
    public U z(U t) { return t; }
  }

  static @part class QandS<QS> implements Q<QS>, S<QS> {
    @link(share=M.class) Q<QS> q = new QPart<>();
    @link S<QS> s = new SPart<>();
    public QS z(QS qs) { return (QS)(s.s() + " QandS.z"); }

    public String s() { return q() + " QandS.s"; }

//    public String foo() {
//      // Ambiguous call. 'm()' is declared in multiple interfaces of this part (Q, S),
//      // which dispatch independently and may reach different implementations: qualify with the intended interface e.g. ((Q)this).m()
//      return m(); // compile error: Ambiguous call.
//    }
  }

  static @part class Middle<R> implements Q<R>, S<R> {
    @link Q<R> q = new QPart<>();

    public String s() { return q() + " Middle.s"; }
  }

  static class Outer<O> implements S<O> {
    @link S<O> s = new Middle<>();

    public String m() { return "Outer.m"; }
  }

  static class Outer2<O> implements S<O> {
    @link S<O> s = new QandS<>();

    public String m() { return "Outer2.m"; }
  }
}
