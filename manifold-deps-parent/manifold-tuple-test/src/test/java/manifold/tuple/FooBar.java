/*
 * Copyright (c) 2022 - Manifold Systems LLC
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

package manifold.tuple;

import manifold.ext.rt.api.auto;

public class FooBar<T extends CharSequence>
{
  T t;

  public static void main( String[] args )
  {
    FooBar bar = new FooBar("a");
    auto asf = bar.whatever();
    System.out.println( asf.Nnn );
  }

  FooBar( T ch)
  {
    t = ch;
  }

  auto whatever()
  {
    String name = "hi";
    int age = 7;
    auto b = (Nnn: name, Aaa: age);
    return b;
  }
}
