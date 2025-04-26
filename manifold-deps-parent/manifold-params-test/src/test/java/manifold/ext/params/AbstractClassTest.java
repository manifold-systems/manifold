/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.ext.params;

import junit.framework.TestCase;


public class AbstractClassTest extends TestCase
{
  public void testAbstractClass()
  {
    MyAbstractClassImpl myAbstractClassImpl = new MyAbstractClassImpl();
    String result = myAbstractClassImpl.abstractMethod( name:"scott" );
    assertEquals( "scott", result );
    result = myAbstractClassImpl.abstractMethod( "scott" );
    assertEquals( "scott", result );
    result = myAbstractClassImpl.abstractMethod();
    assertEquals( "joe", result );

    MyAbstractClass myAbstractClass = new MyAbstractClassImpl();
    result = myAbstractClass.abstractMethod( name:"scott" );
    assertEquals( "scott", result );
    result = myAbstractClass.abstractMethod( "scott" );
    assertEquals( "scott", result );
    result = myAbstractClass.abstractMethod();
    assertEquals( "joe", result );
  }

  static abstract class MyAbstractClass
  {
    abstract String abstractMethod( String name = "joe" );
  }

  static class MyAbstractClassImpl extends MyAbstractClass
  {
    @Override
    String abstractMethod( String name )
    {
      return name;
    }
  }
}