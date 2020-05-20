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

package manifold.ext.structural;

import java.time.chrono.ChronoLocalDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import junit.framework.TestCase;
import manifold.ext.extensions.java.time.chrono.ChronoLocalDateTime.Date_To_ChronoLocalDateTime;


/**
 * Tests registered IProxyFactory service, see {@link Date_To_ChronoLocalDateTime} and its
 * registered service in {@code META-INF/services/manifold.ext.rt.api.IDynamicProxyFactory}
 */
public class ProxyFactoryTest extends TestCase
{
  public void testDate_To_ChronoLocalDateTime()
  {
    //noinspection deprecation
    Date from = new Date( 1982, 6, 4 ); // Date month is 0-based
    ChronoLocalDateTime castedDate = (ChronoLocalDateTime)from;
    assertEquals( Date.class, castedDate.getClass() ); // still a date, only a Chrono upon Chrono invocation
    ChronoLocalDateTime newDate = castedDate.plus( 1, ChronoUnit.MONTHS );
    assertEquals( 8, newDate.get( ChronoField.MONTH_OF_YEAR ) );
  }
}
