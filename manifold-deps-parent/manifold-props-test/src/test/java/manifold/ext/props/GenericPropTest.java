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

package manifold.ext.props;

import junit.framework.TestCase;
import manifold.ext.props.rt.api.override;
import manifold.ext.props.rt.api.var;

import java.util.Arrays;
import java.util.List;

public class GenericPropTest extends TestCase
{
  interface IGenericProps<T> {
    @var List list_Raw;
    @var List<String> list_String;
    @var List<CharSequence> list_CharSequence;
    @var List<?> list_Wildcard;
    @var List<? extends CharSequence> list_Wildcard_Extends_CharSequence;
    @var List<? super CharSequence> list_Wildcard_Super_CharSequence;
    @var List<String>[] list_String_Array;
    @var List<T> list_t;
    @var T _t;
  }
  static class GenericPropsImpl implements IGenericProps<String>
  {
    @override @var List list_Raw;
    @override @var List<String> list_String;
    @override @var List<CharSequence> list_CharSequence;
    @override @var List<?> list_Wildcard;
    @override @var List<? extends CharSequence> list_Wildcard_Extends_CharSequence;
    @override @var List<? super CharSequence> list_Wildcard_Super_CharSequence;
    @override @var List<String>[] list_String_Array;
    @override @var List<String> list_t;
    @override @var String _t;

    // tests that List<String> and List<String>[] don't match with PropertyProcessor#ghettoErasure
    public void setList_String_Array( List<String> fooledya )
    {
      throw new RuntimeException();
    }
  }

  public void testFromInterface()
  {
    IGenericProps<String> props = new GenericPropsImpl();

    List list_Raw = Arrays.asList( "abc", 123 );
    props.list_Raw = list_Raw;
    assertEquals( list_Raw, props.list_Raw );

    List<CharSequence> list_CharSequence = Arrays.asList( "abc", new StringBuilder( "abc" ) );
    props.list_CharSequence = list_CharSequence;
    assertEquals( list_CharSequence, props.list_CharSequence );

    List<String> list_String = Arrays.asList( "abc", "def" );
    props.list_String = list_String;
    assertEquals( list_String, props.list_String );

    //noinspection UnnecessaryLocalVariable
    List<?> list_Wildcard = list_Raw;
    props.list_Wildcard = list_Wildcard;
    assertEquals( list_Wildcard, props.list_Wildcard );

    //noinspection UnnecessaryLocalVariable
    List<? extends CharSequence> list_Wildcard_Extends_CharSequence = list_CharSequence;
    props.list_Wildcard_Extends_CharSequence = list_Wildcard_Extends_CharSequence;
    assertEquals( list_Wildcard_Extends_CharSequence, props.list_Wildcard_Extends_CharSequence );

    List<Object> list_Wildcard_Super_CharSequence = Arrays.asList( "abc", 123 );
    props.list_Wildcard_Super_CharSequence = list_Wildcard_Super_CharSequence;
    assertEquals( list_Wildcard_Super_CharSequence, props.list_Wildcard_Super_CharSequence );

    //noinspection unchecked
    List<String>[] list_String_Array = new List[] {Arrays.asList( "abc", "def" )};
    props.list_String_Array = list_String_Array;
    assertEquals( list_String_Array, props.list_String_Array );

    String t = props._t;
  }

  public void testFromImpl()
  {
    IGenericProps props = new GenericPropsImpl();

    List list_Raw = Arrays.asList( "abc", 123 );
    props.list_Raw = list_Raw;
    assertEquals( list_Raw, props.list_Raw );

    List<CharSequence> list_CharSequence = Arrays.asList( "abc", new StringBuilder( "abc" ) );
    props.list_CharSequence = list_CharSequence;
    assertEquals( list_CharSequence, props.list_CharSequence );

    List<String> list_String = Arrays.asList( "abc", "def" );
    props.list_String = list_String;
    assertEquals( list_String, props.list_String );

    //noinspection UnnecessaryLocalVariable
    List<?> list_Wildcard = list_Raw;
    props.list_Wildcard = list_Wildcard;
    assertEquals( list_Wildcard, props.list_Wildcard );

    //noinspection UnnecessaryLocalVariable
    List<? extends CharSequence> list_Wildcard_Extends_CharSequence = list_CharSequence;
    props.list_Wildcard_Extends_CharSequence = list_Wildcard_Extends_CharSequence;
    assertEquals( list_Wildcard_Extends_CharSequence, props.list_Wildcard_Extends_CharSequence );

    List<Object> list_Wildcard_Super_CharSequence = Arrays.asList( "abc", 123 );
    props.list_Wildcard_Super_CharSequence = list_Wildcard_Super_CharSequence;
    assertEquals( list_Wildcard_Super_CharSequence, props.list_Wildcard_Super_CharSequence );

    //noinspection unchecked
    List<String>[] list_String_Array = new List[] {Arrays.asList( "abc", "def" )};
    props.list_String_Array = list_String_Array;
    assertEquals( list_String_Array, props.list_String_Array );
  }

  public void testSubGen()
  {
    SubGen subGen = new SubGen( Arrays.asList( "a", "b" ), 5 );
    assertEquals( Arrays.asList( "a", "b" ), subGen.names );
    assertEquals( 5, (int)subGen.result );
  }

  public void testSimpleGen()
  {
    SimpleGen.Child child = new SimpleGen.Child();
    child.foo = "hi";
    assertEquals( "hi", child.testMe() );
  }
}
