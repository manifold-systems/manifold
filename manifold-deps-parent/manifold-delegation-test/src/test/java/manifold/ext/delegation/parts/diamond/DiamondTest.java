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

package manifold.ext.delegation.parts.diamond;

import junit.framework.TestCase;

import static manifold.ext.delegation.parts.diamond.Student.Program.BS;
import static manifold.ext.delegation.parts.diamond.Teacher.Department.Science;

/**
 * Together PersonPart, StudentPart, TeacherPart, and TaPart form a classic diamond pattern with Person
 * at the top. This test exercises {@code @link(share=Person.class)} in {@link TaPart} to safely handle this use-case.
 */
public class DiamondTest extends TestCase
{
  public void testDiamond()
  {
    PersonPart fred = new PersonPart( "Fred" );
    Student taStudent = new StudentPart( fred, BS );
    TA ta = new TaPart( taStudent, Science );

    assertEquals( "Fred", ta.getName() );
    assertEquals( BS, ta.getProgram() );
    assertEquals( Science, ta.getDepartment() );

    assertEquals( "TA", ta.getTitle() );
    assertEquals( "TA Fred", ta.getTitledName() );
  }

  public void testMoreThisReplacement()
  {
    PersonPart fred = new PersonPart( "Fred" );
    Student taStudent = new StudentPart( fred, BS );
    TA ta = new TaPart( taStudent, Science );

    assertEquals( "TA Fred", ta.getTitledName2() );
    assertEquals( "TA Fred", ta.getTitledName3() );

    assertEquals( "TA", fred.getTitleFromThisArg() );
    assertEquals( "TA", fred.getTitleFromQualThisArg() );

    assertEquals( "TA", fred.getPersonFromThisReturn().getTitle() );
    assertEquals( "TA", fred.getPersonFromQualThisReturn().getTitle() );

    assertEquals( "TA", fred.getPersonFromThisAssignment().getTitle() );
    assertEquals( "TA", fred.getPersonFromQualThisAssignment().getTitle() );

    assertEquals( "TA", fred.getPersonFromThisParens().getTitle() );
    assertEquals( "TA", fred.getPersonFromQualThisParens().getTitle() );

    assertEquals( "TA", fred.getPersonFromThisCast().getTitle() );
    assertEquals( "TA", fred.getPersonFromQualThisCast().getTitle() );
  }
}
