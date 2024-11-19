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

import junit.framework.TestCase;
import manifold.ext.rt.api.Structural;

public class CoercionTest extends TestCase
{
  public void testStructuralInterfaceCoercion()
  {
    // coerce tuple to NameAge structural interface
    NameAge nameAge = (name:"Scott", age:100, male:true);

    assertEquals( "Scott", nameAge.getName() );
    assertEquals( 100, nameAge.getAge() );
    assertTrue( nameAge.isMale() );

    nameAge.setName( "Bob" );
    assertEquals( "Bob", nameAge.getName() );

    nameAge.setAge( 99 );
    assertEquals( 99, nameAge.getAge() );

    nameAge.setMale( true );
    assertTrue( nameAge.isMale() );
  }

  public void testStructuralInterfaceCoercionWithCovariantTypes()
  {
    NameAge2 nameAge = (name:"Scott", age:100, male:true);

    assertEquals( "Scott", nameAge.getName() );
    assertEquals( 100.0, nameAge.getAge(), 0d );

    nameAge.setName( "Bob" );
    assertEquals( "Bob", nameAge.getName() );

    nameAge.setAge( 99 );
    assertEquals( 99.0, nameAge.getAge(), 0d );
  }

  public void testArgs()
  {
    testArgs((), 0, 0, 0, 255);
    testArgs((red:1), 1, 0, 0, 255);
    testArgs((green:2), 0, 2, 0, 255);
    testArgs((blue:3), 0, 0, 3, 255);
    testArgs((red:1, green:2), 1, 2, 0, 255);
    testArgs((red:1, blue:3), 1, 0, 3, 255);
    testArgs((green:2, blue:3), 0, 2, 3, 255);
    testArgs((red:1, green:2, blue:3), 1, 2, 3, 255);
    testArgs((green:2, blue:3, red:1), 1, 2, 3, 255);
    testArgs((opacity:96), 0, 0, 0, 96);
  }

  @Structural
  interface Color {
    default int getRed() {return 0;}
    default int getGreen() {return 0;}
    default int getBlue() {return 0;}
    default int getOpacity() {return 255;}
  }
  private void testArgs( Color args, int red, int green, int blue, int opacity )
  {
    assertEquals( args.getRed(), red );
    assertEquals( args.getGreen(), green );
    assertEquals( args.getBlue(), blue );
    assertEquals( args.getOpacity(), opacity );
  }

  @Structural
  interface NameAge
  {
    String getName();
    void setName(String name);

    int getAge();
    void setAge( int age );
    
    boolean isMale();
    void setMale( boolean male );
  }

  @Structural
  interface NameAge2
  {
    CharSequence getName();
    void setName(String name);

    double getAge();
    void setAge( int age );
  }
}
