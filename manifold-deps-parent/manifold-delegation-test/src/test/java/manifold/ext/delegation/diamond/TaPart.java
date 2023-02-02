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

import manifold.ext.delegation.rt.api.part;
import manifold.ext.delegation.rt.api.link;

/**
 * TaPart demonstrates how "diamond" patterns work with delegate sharing.
 * <p/>
 * TaPart shares its Student part with its Teacher part to disambiguate the delegation of the Person interface shared between
 * them.
 */
public @part class TaPart implements TA // TA is a "diamond" interface
{
  @link(share = true) Student _student; // 'share = true' disambiguates Person shared between TA's Student and Teacher parts
  @link Teacher _teacher;


  public TaPart( Student student, Department department )
  {
    // student is shared as the Person part of the Teacher. Because Person, Student, and Teacher are all @part classes
    // they share the same 'self' which will be the instance of this TaPart class.
    _student = student;
    _teacher = new TeacherPart( _student, department );
  }

  @Override
  public String getTitle()
  {
    return "TA";
  }
}
