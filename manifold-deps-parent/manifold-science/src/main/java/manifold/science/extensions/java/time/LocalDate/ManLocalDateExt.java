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

package manifold.science.extensions.java.time.LocalDate;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import manifold.science.measures.Time;
import manifold.science.measures.TimeUnit;
import manifold.science.util.Rational;

@Extension
public class ManLocalDateExt
{
  public static LocalDate plus( @This LocalDate thiz, Time time )
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
      return thiz + period;
    }

    return thiz + (TemporalAmount)time;
  }

  public static LocalDate minus( @This LocalDate thiz, Time time )
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
      return thiz - period;
    }

    return thiz - (TemporalAmount)time;
  }
}
