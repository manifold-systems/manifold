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

public @part class PersonPart implements Person
{
  private final String _name;

  public PersonPart( String name )
  {
    _name = name;
  }

  @Override
  public String getName()
  {
    return _name;
  }

  @Override
  public String getTitle()
  {
    return "Good-time";
  }

  public String getTitledName2()
  {
    return getTitle() + // implicit 'this'
      " " +
      this.getName();   // explicit 'this'
  }

  public String getTitledName3()
  {
    foo( this );
    foo( PersonPart.this );

    return new Roundabout().getMyTitle_Implicit() + // implicit 'PersonPart.this'
      " " +
      new Roundabout().getMyName_Explicit();   // explicit 'PersonPart.this'
  }

  private void foo( Person p )
  {

  }

  private class Roundabout
  {
    String getMyTitle_Implicit()
    {
      return getTitle();
    }

    String getMyName_Explicit()
    {
      return PersonPart.this.getName();
    }
  }
}
