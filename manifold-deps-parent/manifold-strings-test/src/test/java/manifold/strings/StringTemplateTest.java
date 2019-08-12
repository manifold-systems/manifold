/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.strings;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import junit.framework.TestCase;
import manifold.strings.api.DisableStringLiteralTemplates;
import manifold.util.ReflectUtil;


import static org.junit.Assert.assertNotEquals;

public class StringTemplateTest extends TestCase
{
  Integer _num = 10;

  public void testSimple()
  {
    int integer = 5;

    String value = "It is $integer o'clock";
    assertEquals( "It is 5 o'clock", value );

    value = "$integer$integer";
    assertEquals( "55", value );

    value = "$integer${integer}";
    assertEquals( "55", value );

    value = "$integer.$integer";
    assertEquals( "5.5", value );

    value = "$integer.${integer}";
    assertEquals( "5.5", value );

    value = "$integer";
    assertEquals( "5", value );

    value = " $integer";
    assertEquals( " 5", value );

    value = " $integer ";
    assertEquals( " 5 ", value );

    value = "$integer ";
    assertEquals( "5 ", value );

    value = "\n$integer";
    assertEquals( "\n5", value );

    value = "\n$integer\n";
    assertEquals( "\n5\n", value );

    value = "$integer\n";
    assertEquals( "5\n", value );
  }

  public void testExprEasy()
  {
    int integer = 5;

    String value = "It is ${integer} o'clock";
    assertEquals( "It is 5 o'clock", value );

    value = "${integer}${integer}";
    assertEquals( "55", value );

    value = "${integer}$integer";
    assertEquals( "55", value );

    value = "${integer}.${integer}";
    assertEquals( "5.5", value );

    value = "$integer.${integer}";
    assertEquals( "5.5", value );

    value = "${integer}";
    assertEquals( "5", value );

    value = " ${integer}";
    assertEquals( " 5", value );

    value = " ${integer} ";
    assertEquals( " 5 ", value );

    value = "${integer} ";
    assertEquals( "5 ", value );

    value = "\n${integer}";
    assertEquals( "\n5", value );

    value = "\n${integer}\n";
    assertEquals( "\n5\n", value );

    value = "${integer}\n";
    assertEquals( "5\n", value );

    value = "$_num";
    assertEquals( "10", value );

    value = "$this";
    assertEquals( "whatever", value );
  }

  public void testExprMore()
  {
    String value = "${\"hi\"}";
    assertEquals( "hi", value );

    value = "${5 + 3}";
    assertEquals( "8", value );

    value = "${_num}";
    assertEquals( "10", value );

    value = "${this._num}";
    assertEquals( "10", value );

    List<Class> classes = Arrays.asList( String.class, Integer.class );

    value = "Count: ${classes.size()}";
    assertEquals( "Count: 2", value );

    value = "Name: ${classes.get(1).getSimpleName()}";
    assertEquals( "Name: Integer", value );

    value = "${classes.size()}${classes.size()}";
    assertEquals( "22", value );

    value = "${classes.size() + classes.size() + 2}";
    assertEquals( "6", value );
  }

  public void testEscape()
  {
    String value = "\$5"; 
    assertEquals( "${'$'}5", value );
    
    value = "\${6}"; 
    assertEquals( "${'$'}{6}", value );

    value = "\${7}\${8}";
    assertEquals( "${'$'}{7}${'$'}{8}", value );

    value = "${7}\${8}";
    assertEquals( "7${'$'}{8}", value );

    value = "\${7}${8}";
    assertEquals( "${'$'}{7}8", value );
    
    int num1 = 7;
    int num2 = 8;

    value = "\$num1\$num2";
    assertEquals( "${'$'}num1${'$'}num2", value );

    value = "$num1\$num2";
    assertEquals( "7${'$'}num2", value );

    value = "\$num1$num2";
    assertEquals( "${'$'}num18", value );
  }
  
  @DisableStringLiteralTemplates
  public void testDisableStringLiteralTemplates_Method()
  {
    String value = "${\"hi\"}";
    assertNotEquals( "hi", value );

    @DisableStringLiteralTemplates(false)
    String value2 = "${\"hi\"}";
    assertEquals( "hi", value2 );
  }

  public void testDisableStringLiteralTemplates_Statement()
  {
    String value = "${\"hi\"}";
    assertEquals( "hi", value );

    @DisableStringLiteralTemplates
    String value2 = "${\"hi\"}";
    assertNotEquals( "hi", value2 );

    String value3 = "${\"hi\"}";
    assertEquals( "hi", value3 );
  }

  @Retention( RetentionPolicy.RUNTIME )
  private @interface MyAnno
  {
    String value();
  }
  @MyAnno( "${verbatim}" )
  public void testAnnotationsNotSupported()
  {
    Method m = Objects.requireNonNull( ReflectUtil.method( getClass(), "testAnnotationsNotSupported" ) ).getMethod();
    assertEquals( "\${verbatim}", m.getAnnotation( MyAnno.class ).value() );
  }

  public void testInner()
  {
    assertEquals( "hi", new TestInner().testMe() );
    assertNotEquals( "hi", new TestInnerDisabled().testMe() );
    assertEquals( "hi", new TestInnerDisabled().testMeEnabled() );
    assertNotEquals( "hi", new TestInnerDisabled_Method().testMe() );
  }

  static class TestInner
  {
    String testMe()
    {
      return "${\"hi\"}";
    }
  }

  @DisableStringLiteralTemplates
  static class TestInnerDisabled
  {
    String testMe()
    {
      return "${\"hi\"}";
    }

    @DisableStringLiteralTemplates(false)
    String testMeEnabled()
    {
      return "${\"hi\"}";
    }
  }

  static class TestInnerDisabled_Method
  {
    @DisableStringLiteralTemplates
    String testMe()
    {
      return "${\"hi\"}";
    }
  }

  @DisableStringLiteralTemplates
  static class TestDisableInner
  {
    public void testDisableStringLiteralTemplates_Method()
    {
      String value = "${\"hi\"}";
      assertNotEquals( "hi", value );
    }
  }

  public String toString()
  {
    return "whatever";
  }
}
