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

package manifold.science;

import manifold.science.api.AbstractMeasure;
import manifold.science.measures.*;
import org.junit.Test;

import java.io.*;

import static manifold.science.util.UnitConstants.*;
import static org.junit.Assert.assertEquals;

public class SerializationTest
{
  @Test
  public void testUnary() throws Exception
  {
    Length length = 56 m;
    assertSerialization( length );
  }

  @Test
  public void testProduct() throws Exception
  {
    Area area = 4 ft * 6 m;
    assertSerialization( area );
  }

  @Test
  public void testQuotient() throws Exception
  {
    Velocity velocity = 195 mi/hr;
    assertSerialization( velocity );

    // test that s/s does *not* cancel to 1, resulting in: 25 m
    Acceleration acc = 25 m/s/s;
    assertSerialization( acc );
  }

  @Test
  public void testMixed() throws Exception
  {
    Force force = 56 kg m/s/s;
    assertSerialization( force );

    Energy energy = 100 N m;
    assertSerialization( energy );
  }

  private void assertSerialization( AbstractMeasure<?,?> object ) throws Exception
  {
    assertEquals( teleclone( object ), object );
  }

  private Object teleclone( Serializable source ) throws Exception
  {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream os = new ObjectOutputStream( bytes );
    os.writeObject( source );
    ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray() ) );
    return in.readObject();
  }
}

