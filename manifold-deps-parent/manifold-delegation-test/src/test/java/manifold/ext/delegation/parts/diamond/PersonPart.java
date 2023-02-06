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
    return new FromInnerClass().getMyTitle_Implicit() + // implicit 'PersonPart.this'
      " " +
      new FromInnerClass().getMyName_Explicit();   // explicit 'PersonPart.this'
  }

  public String getTitleFromThisArg()
  {
    return getTitleFromThisArg( this );
  }
  public String getTitleFromQualThisArg()
  {
    return getTitleFromThisArg( PersonPart.this );
  }
  private String getTitleFromThisArg( Person p )
  {
    return p.getTitle();
  }

  public Person getPersonFromThisReturn()
  {
    return this;
  }
  public Person getPersonFromQualThisReturn()
  {
    return PersonPart.this;
  }

  public Person getPersonFromThisAssignment()
  {
    //noinspection UnnecessaryLocalVariable
    Person p = this;
    return p;
  }
  public Person getPersonFromQualThisAssignment()
  {
    //noinspection UnnecessaryLocalVariable
    Person p = PersonPart.this;
    return p;
  }

  public Person getPersonFromThisParens()
  {
    //noinspection UnnecessaryLocalVariable
    Person p = (this);
    return p;
  }
  public Person getPersonFromQualThisParens()
  {
    //noinspection UnnecessaryLocalVariable
    Person p = (PersonPart.this);
    return p;
  }

  public Person getPersonFromThisCast()
  {
    //noinspection RedundantCast
    return (Person)this;
  }
  public Person getPersonFromQualThisCast()
  {
    // noinspection RedundantCast
    return (Person)PersonPart.this;
  }

  private class FromInnerClass
  {
    String getMyTitle_Implicit()
    {
      // implicit PersonPart.this usage
      return getTitle();
    }

    String getMyName_Explicit()
    {
      // explicit PersonPart.this usage
      return PersonPart.this.getName();
    }
  }
}
