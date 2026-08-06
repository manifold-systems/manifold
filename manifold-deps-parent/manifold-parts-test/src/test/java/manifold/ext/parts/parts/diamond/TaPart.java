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

import manifold.ext.parts.rt.api.part;
import manifold.ext.parts.rt.api.link;
import manifold.util.ReflectUtil;

/**
 * TaPart demonstrates how "diamond" patterns work with delegate sharing.
 * <p/>
 * TaPart shares its Student part with its Teacher part to disambiguate the delegation of the Person interface shared between
 * them.
 */
public @part class TaPart implements TA // TA is a "diamond" interface with Student and Teacher converging on Person
{
  @link(share = Person.class) Student _student; // 'share=Person.class' disambiguate Person shared between TA's Student and Teacher parts
  @link Teacher _teacher;


  public TaPart( Student student, Department department )
  {
    // student is shared as the Person part of the Teacher. However, because TeacherPart only uses the student to delegate
    // the Person interface impl, student is effectively unused in TeacherPart because Person calls route through the TaPart composite
    // which forwards to StudentPart. In other words, self-calls on Person methods inside TeacherPart are wired to the TaPart
    // composite as the receiver (self), which forwards to StudentPart. To test this, instead of passing _student into TeacherPart
    // as we normally would, we pass in a new Person. Note, this is a real use-case e.g., consider if Teacher were passed
    // into TaPart, its Person would be set, perhaps to the professor the TA is assisting: its Person must be bypassed.
    _teacher = new TeacherPart( new PersonPart( "Mr. Peabody" ), department );
//    _teacher = new TeacherPart( _student, department );
    _student = student;
  }

  @Override
  public String getTitle()
  {
    return "TA";
  }

  public String callTitledNameFromInsideTeacherPart()
  {
    return ((TeacherPart)ReflectUtil.field(this, "_teacher").get()).callTitledNameFromInsideTeacherPart();
  }
}
