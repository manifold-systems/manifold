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

package manifold.ext.props.infer;

import org.junit.Test;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ObjectTest
{
  @Test
  public void testJreInterfaceInferredProperties()
  {
    // use "key" and "value" inferred properties on java.util.Map
    Map<String,String> map = new LinkedHashMap<>();
    map.put( "a", "1" );
    map.put( "b", "2" );
    Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
    Map.Entry entry = iterator.next();
    assertEquals( "a", entry.key );
    assertEquals( "1", entry.value );
    entry = iterator.next();
    assertEquals( "b", entry.key );
    assertEquals( "2", entry.value );
  }

  @Test
  public void testJreClassInferredProperties()
  {
    LocalDateTime ldt = LocalDateTime.of( 1986, 6, 12, 3, 30 );
    assertEquals( 1986, ldt.year );
    assertEquals( Month.JUNE, ldt.month );
    assertEquals( 12, ldt.dayOfMonth );
    assertEquals( 3, ldt.hour );
    assertEquals( 30, ldt.minute );

    Calendar calendar = Calendar.instance; // call getInstance() static
    if (calendar.firstDayOfWeek == Calendar.SUNDAY) {  // call getFirstDayOfWeek()
      calendar.firstDayOfWeek = Calendar.MONDAY; // call setFirstDayOfWeek()
    }
    if (!calendar.isLenient) { // call isLenient()
      calendar.isLenient = true; // call setLenient()
    }
  }
}
