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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Foo
{
  public auto callHere()
  {
//    if( true )
//    {
//      // compile error: 'tuple' requires non-null value
//      return null;
//    }
//    if( true )
//    {
//      // compile error: Cannot return 'tuple', return a more specific type
//      tuple value = callMethod();
//      return value;
//    }

    if( true )
    {
      return explicitTupleConstruction();
    }
    // ok, same type
    return explicitTupleConstruction();

//    // compile error: Return statements require the same 'tuple' type
//    return new tuple() {public int myField = 5;};
  }

  public auto indirectReturn()
  {
    auto res = new Object() {public int myField = 5;};
    return res;
  }

  public auto explicitTupleConstruction()
  {
    return new Object() {public int myField = 5;};
  }

  public auto nonlabeledMultiReturn()
  {
    String name = "Helga";
    int age = 2;
    return name, age;
  }

  public auto labeledMultiReturn()
  {
    String n = "Helga";
    int a = 2;
    return Name: n, Age: a;
  }

  public auto fromNonlabeledTupleExpression()
  {
    String name = "Helga";
    int age = 2;
    auto myTuple = (name, age);
    return myTuple;
  }
  public auto fromLabeledTupleExpression()
  {
    String name = "Helga";
    int age = 2;
    auto myTuple = (Name: "Helga", Age: age);
    return myTuple;
  }

  public auto fromGenericTupleExpression()
  {
    List<String> list = new ArrayList<>();
    for( int i = 0; i < 10; i++ )
    {
      list.add( "Helga" + i );
    }
    return list.stream().map( e -> (name: e, age: 2) )
      .collect( Collectors.toList() );
  }

  public auto refCycle()
  {
    Abc abc = new Abc();
    return abc.refCycle();
  }

  public auto fib( int n )
  {
    if (n <= 1) return n;
    return fib(n - 1) + fib(n - 2);
  }

//  public auto headRecursion( int i )
//  {
//    i--;
//    if( i > 0 )
//    {
//      return headRecursion( i ).append( " " );
//    }
//    return new StringBuilder();
//  }
}
