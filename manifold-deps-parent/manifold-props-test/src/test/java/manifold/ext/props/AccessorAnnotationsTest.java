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
import manifold.ext.props.rt.api.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class AccessorAnnotationsTest extends TestCase
{
  class Foo {
    @prop(annos=@sampleAnno(name="foo", flags=1), param=@sampleAnno(name="p", flags=0))
    public String name;

    @get(annos={@sampleAnno(name="bar", flags=2)})
    @set(annos=@sampleAnno(name="baz", flags=3), param=@sampleAnno(name="boz", flags=4))
    public String asdf;
  }

  public void testAccessorAnnotations() throws NoSuchMethodException
  {
    Foo w = new Foo();
    w.name = "hi";
    assertEquals( "hi", w.name );
    w.asdf = "bye";
    assertEquals( "bye", w.asdf );

    sampleAnno getterAnno = Foo.class.getMethod( "getName" ).getAnnotation( sampleAnno.class );
    assertEquals( "foo", getterAnno.name() );
    assertEquals( 1, getterAnno.flags() );
    Method setter = Foo.class.getMethod( "setName", String.class );
    sampleAnno setterAnno = setter.getAnnotation( sampleAnno.class );
    assertEquals( "foo", setterAnno.name() );
    assertEquals( 1, setterAnno.flags() );
    assertEquals( 1, setter.getParameterAnnotations()[0].length );
    sampleAnno paramAnno = (sampleAnno)setter.getParameterAnnotations()[0][0];
    assertEquals( "p", paramAnno.name() );
    assertEquals( 0, paramAnno.flags() );

    getterAnno = Foo.class.getMethod( "getAsdf" ).getAnnotation( sampleAnno.class );
    assertEquals( "bar", getterAnno.name() );
    assertEquals( 2, getterAnno.flags() );
    setter = Foo.class.getMethod( "setAsdf", String.class );
    setterAnno = setter.getAnnotation( sampleAnno.class );
    assertEquals( "baz", setterAnno.name() );
    assertEquals( 3, setterAnno.flags() );
    assertEquals( 1, setter.getParameterAnnotations()[0].length );
    paramAnno = (sampleAnno)setter.getParameterAnnotations()[0][0];
    assertEquals( "boz", paramAnno.name() );
    assertEquals( 4, paramAnno.flags() );
  }
}
