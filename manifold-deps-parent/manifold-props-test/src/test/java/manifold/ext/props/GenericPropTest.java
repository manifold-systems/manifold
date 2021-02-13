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
import manifold.ext.props.rt.api.prop;

import java.util.Arrays;
import java.util.List;

public class GenericPropTest extends TestCase
{
  interface IGenericProps {
    @prop List list_Raw;
    @prop List<String> list_String;
    @prop List<CharSequence> list_CharSequence;
    @prop List<?> list_Wildcard;
    @prop List<? extends CharSequence> list_Wildcard_Extends_CharSequence;
    @prop List<? super CharSequence> list_Wildcard_Super_CharSequence;
    @prop List<String>[] list_String_Array;
  }
  class GenericPropsImpl implements IGenericProps
  {
    @prop public List list_Raw;
    @prop public List<String> list_String;
    @prop public List<CharSequence> list_CharSequence;
    @prop public List<?> list_Wildcard;
    @prop public List<? extends CharSequence> list_Wildcard_Extends_CharSequence;
    @prop public List<? super CharSequence> list_Wildcard_Super_CharSequence;
    @prop public List<String>[] list_String_Array;

    // tests that List<String> and List<String>[] don't match with PropertyProcessor#ghettoErasure
    public void setList_String_Array( List<String> fooledya )
    {
      throw new RuntimeException();
    }
  }

  public void testFromInterface()
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
}
