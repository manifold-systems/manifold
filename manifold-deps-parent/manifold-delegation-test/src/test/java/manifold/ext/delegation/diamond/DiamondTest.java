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

package manifold.ext.delegation.diamond;

import junit.framework.TestCase;

import static manifold.ext.delegation.diamond.Student.Program.BS;
import static manifold.ext.delegation.diamond.Teacher.Department.Science;

public class DiamondTest extends TestCase
{
  public void testDiamond()
  {
    Student taStudent = new StudentPart( new PersonPart( "Fred" ), BS );
    TA ta = new TaPart( taStudent, Science );

    assertEquals( "Fred", ta.getName() );
    assertEquals( BS, ta.getProgram() );
    assertEquals( Science, ta.getDepartment() );

    assertEquals( "Mr.", ta.getTitle() );
    assertEquals( "Mr. Fred", ta.getTitledName() ); // wow
    assertEquals( "Mr. Fred", ta.getTitledName2() ); // wow
    assertEquals( "Mr. Fred", ta.getTitledName3() ); // wow
  }
}
