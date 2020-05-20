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

package manifold.science.extensions.java.time.LocalDateTime;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import manifold.science.measures.Time;
import manifold.science.measures.TimeUnit;
import manifold.science.util.Rational;

@Extension
public class ManLocalDateTimeExt
{
  public static LocalDateTime plus( @This LocalDateTime thiz, Time time )
  {
    if( time.getDisplayUnit().isDateBased() )
    {
      // Extract the Period from the date-based time and add that
      Rational years = time.toBaseNumber() / TimeUnit.Year.getSeconds();
      int wholeYears = years.wholePart().intValue();
      Rational months = years.fractionPart() * TimeUnit.Year.getSeconds() / TimeUnit.Month.getSeconds();
      int wholeMonths = months.wholePart().intValue();
      Rational days = months.fractionPart() * TimeUnit.Month.getSeconds() / TimeUnit.Day.getSeconds();
      int wholeDays = days.wholePart().intValue();
      Period period = Period.of( wholeYears, wholeMonths, wholeDays );
      LocalDateTime newDate = thiz + period;

      // Now add the remaining fractional day part, if non-zero, as a Duration (added to the date-time's time component)
      Rational seconds = days.fractionPart() * TimeUnit.Day.getSeconds();
      if( seconds != Rational.ZERO )
      {
        long wholeSeconds = seconds.wholePart().longValue();
        long nanos = (seconds.fractionPart() * 1e+9).longValue();
        newDate = newDate + Duration.ofSeconds( wholeSeconds, nanos );
      }

      return newDate;
    }

    return thiz + (TemporalAmount)time;
  }

  public static LocalDateTime minus( @This LocalDateTime thiz, Time time )
  {
    if( time.getDisplayUnit().isDateBased() )
    {
      // Extract the Period from the date-based time and add that
      Rational years = time.toBaseNumber() / TimeUnit.Year.getSeconds();
      int wholeYears = years.wholePart().intValue();
      Rational months = years.fractionPart() * TimeUnit.Year.getSeconds() / TimeUnit.Month.getSeconds();
      int wholeMonths = months.wholePart().intValue();
      Rational days = months.fractionPart() * TimeUnit.Month.getSeconds() / TimeUnit.Day.getSeconds();
      int wholeDays = days.wholePart().intValue();
      Period period = Period.of( wholeYears, wholeMonths, wholeDays );
      LocalDateTime newDate = thiz - period;

      // Now add the remaining fractional day part, if non-zero, as a Duration (added to the date-time's time component)
      Rational seconds = days.fractionPart() * TimeUnit.Day.getSeconds();
      if( seconds != Rational.ZERO )
      {
        long wholeSeconds = seconds.wholePart().longValue();
        long nanos = (seconds.fractionPart() * 1e+9).longValue();
        newDate = newDate - Duration.ofSeconds( wholeSeconds, nanos );
      }

      return newDate;
    }

    return thiz - (TemporalAmount)time;
  }
}
