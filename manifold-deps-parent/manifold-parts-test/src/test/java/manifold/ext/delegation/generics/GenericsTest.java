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

package manifold.ext.delegation.generics;

import junit.framework.TestCase;
import manifold.ext.delegation.rt.api.link;
import manifold.ext.delegation.rt.api.part;

public class GenericsTest extends TestCase
{
  public void testGenerics()
  {
    TA<String> ta = new TAPart<>( new StudentPart<>( new PersonPart<>( "Fred" ), "CS" ) );
    String titledName = ta.getTitledName();
    assertEquals( "TA Fred", titledName );
    String foo = ta.foo( "hi" );
    assertEquals( "hi", foo );
  }

  interface Person<E>
  {
    E foo( E e );

    String getName();

    String getTitle();

    String getTitledName();
  }

  interface Teacher<F> extends Person<F>
  {
    String getDept();
  }

  interface Student<G> extends Person<G>
  {
    String getMajor();
  }

  interface TA<H> extends Student<H>, Teacher<H>
  {
  }

  static @part class PersonPart<I> implements Person<I>
  {
    private final String _name;

    public PersonPart( String name )
    {
      _name = name;
    }

    public String getName()
    {
      return _name;
    }

    public String getTitle()
    {
      return "Person";
    }

    public String getTitledName()
    {
      return getTitle() + " " + getName();
    }

    public I foo( I e )
    {
      return e;
    }
  }

  static @part class TeacherPart<J> implements Teacher<J>
  {
    @link Person<J> _person;
    private final String _dept;

    public TeacherPart( Person<J> p, String dept )
    {
      _person = p;
      _dept = dept;
    }

    public String getTitle()
    {
      return "Teacher";
    }

    public String getDept()
    {
      return _dept;
    }
  }

  static @part class StudentPart<K> implements Student<K>
  {
    @link Person<K> _person;
    private final String _major;

    public StudentPart( Person p, String major )
    {
      _person = p;
      _major = major;
    }

    public String getTitle()
    {
      return "Student";
    }

    public String getMajor()
    {
      return _major;
    }
  }

  static @part class TAPart<L> implements TA<L>
  {
    @link(share=Person.class) Student<L> _student;
    @link Teacher<L> _teacher;

    public TAPart( Student<L> student )
    {
      _student = student;
      _teacher = new TeacherPart<>( _student, "Math" );
    }

    public String getTitle()
    {
      return "TA";
    }
  }

  static @part class TeacherPart_ifaceFromAnno<L> implements Teacher<L> {
    @link({Teacher.class}) Teacher<L> _student;
    public String getTitle() { return "TA"; }
  }
}