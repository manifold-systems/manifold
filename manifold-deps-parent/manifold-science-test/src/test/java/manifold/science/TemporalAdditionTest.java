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

package manifold.science;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import manifold.science.measures.TimeUnit;
import org.junit.Test;

import static manifold.science.util.UnitConstants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class TemporalAdditionTest
{
  @Test
  public void testPlusWithDateBasedTime()
  {
    LocalDateTime date = LocalDateTime.of( 2018, 10, 17, 17, 35 );

    assertEquals( date + Period.of( 1, 0, 0 ), date + 1yr );
    assertEquals( date + Period.of( 1, 1, 0 ), date + 1yr  + 1mo );
    assertEquals( date + Period.of( 1, 1, 1 ), date + 1yr  + 1mo + 1 day );

    assertEquals( date + Period.of( 1, 1, 1 ) + Duration.ofHours( 1 ),
      date + 1yr  + 1mo + 1 day + 1 hr );
    assertEquals( date + Period.of( 1, 1, 1 ) + Duration.ofHours( 1 ) + Duration.ofMinutes( 1 ),
      date + 1yr  + 1mo + 1 day + 1 hr + 1 min );
    assertEquals( date + Period.of( 1, 1, 1 ) + Duration.ofHours( 1 ) + Duration.ofMinutes( 1 ) + Duration.ofSeconds( 1 ),
      date + 1yr  + 1mo + 1 day + 1 hr + 1 min + 1 s );
    assertEquals( date + Period.of( 1, 1, 1 ) + Duration.ofHours( 1 ) + Duration.ofMinutes( 1 ) + Duration.ofSeconds( 1, 1 ),
      date + 1yr  + 1mo + 1 day + 1 hr + 1 min + 1 s + 1 ns );
  }

  @Test
  public void testMinusWithDateBasedTime()
  {
    LocalDateTime date = LocalDateTime.of( 2018, 10, 17, 17, 35 );

    assertEquals( date - Period.of( 1, 0, 0 ), date - 1yr );
    assertEquals( date - Period.of( 1, 1, 0 ), date - 1yr  - 1mo );
    assertEquals( date - Period.of( 1, 1, 1 ), date - 1yr  - 1mo - 1 day );

    assertEquals( date - Period.of( 1, 1, 1 ) - Duration.ofHours( 1 ),
                  date - 1yr  - 1mo - 1 day - 1 hr );
    assertEquals( date - Period.of( 1, 1, 1 ) - Duration.ofHours( 1 ) - Duration.ofMinutes( 1 ),
                  date - 1yr  - 1mo - 1 day - 1 hr - 1 min );
    assertEquals( date - Period.of( 1, 1, 1 ) - Duration.ofHours( 1 ) - Duration.ofMinutes( 1 ) - Duration.ofSeconds( 1 ),
                  date - 1yr  - 1mo - 1 day - 1 hr - 1 min - 1 s );
    assertEquals( date - Period.of( 1, 1, 1 ) - Duration.ofHours( 1 ) - Duration.ofMinutes( 1 ) - Duration.ofSeconds( 1, 1 ),
                  date - 1yr  - 1mo - 1 day - 1 hr - 1 min - 1 s - 1 ns );
  }

  @Test
  public void testPlusWithNonDateBasedTime()
  {
    LocalDateTime date = LocalDateTime.of( 2018, 10, 17, 17, 35 );

    Period yearPeriod = Period.of( 1, 0, 0 );
    Duration yearDuration = ChronoUnit.YEARS.getDuration();
    
    // A Time value expressed in non-date based units e.g., Seconds, works using the raw amount of time as opposed to
    // the calendar amount derived from a date-based time amount.
    assertNotEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Planck ) );
    assertNotEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Femto ) );
    assertNotEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Pico ) );
    assertNotEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Nano ) );
    assertNotEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Micro ) );
    assertNotEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Milli ) );
    assertNotEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Second ) );
    assertNotEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Minute ) );
    assertNotEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Hour ) );

    assertEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Planck ) );
    assertEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Femto ) );
    assertEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Pico) );
    assertEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Nano ) );
    assertEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Micro ) );
    assertEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Milli ) );
    assertEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Second ) );
    assertEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Minute ) );
    assertEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Hour ) );
    
    // A Time value expressed in date-based units such as Month works in terms of calendar value
    assertEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Day ) );
    assertEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Week ) );
    assertEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Month ) );
    assertEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Year ) );
    assertEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Decade ) );
    assertEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Century ) );
    assertEquals( date + yearPeriod, date + (1 yr).to( TimeUnit.Era ) );

    assertNotEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Day ) );
    assertNotEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Week ) );
    assertNotEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Month ) );
    assertNotEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Year ) );
    assertNotEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Decade ) );
    assertNotEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Century ) );
    assertNotEquals( date + yearDuration, date + (1 yr).to( TimeUnit.Era ) );
  }

  @Test
  public void testMinusWithNonDateBasedTime()
  {
    LocalDateTime date = LocalDateTime.of( 2018, 10, 17, 17, 35 );

    Period yearPeriod = Period.of( 1, 0, 0 );
    Duration yearDuration = ChronoUnit.YEARS.getDuration();
    
    // A Time value expressed in non-date based units e.g., Seconds, works using the raw amount of time as opposed to
    // the calendar amount derived from a date-based time amount.
    assertNotEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Planck ) );
    assertNotEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Femto ) );
    assertNotEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Pico ) );
    assertNotEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Nano ) );
    assertNotEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Micro ) );
    assertNotEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Milli ) );
    assertNotEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Second ) );
    assertNotEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Minute ) );
    assertNotEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Hour ) );

    assertEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Planck ) );
    assertEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Femto ) );
    assertEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Pico) );
    assertEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Nano ) );
    assertEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Micro ) );
    assertEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Milli ) );
    assertEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Second ) );
    assertEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Minute ) );
    assertEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Hour ) );
    
    // A Time value expressed in date-based units such as Month works in terms of calendar value
    assertEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Day ) );
    assertEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Week ) );
    assertEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Month ) );
    assertEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Year ) );
    assertEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Decade ) );
    assertEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Century ) );
    assertEquals( date - yearPeriod, date - (1 yr).to( TimeUnit.Era ) );

    assertNotEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Day ) );
    assertNotEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Week ) );
    assertNotEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Month ) );
    assertNotEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Year ) );
    assertNotEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Decade ) );
    assertNotEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Century ) );
    assertNotEquals( date - yearDuration, date - (1 yr).to( TimeUnit.Era ) );
  }
}
