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

package manifold.api.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;


import static manifold.api.util.ManStringUtil.isAlpha;
import static manifold.api.util.ManStringUtil.isNumeric;

public class ManDateTimeUtil
{
  private static final DateTimeFormatter[] DATE_TIME_FORMATTERS = {
    DateTimeFormatter.ISO_DATE_TIME,
    DateTimeFormatter.RFC_1123_DATE_TIME,
    DateTimeFormatter.ISO_LOCAL_DATE_TIME,
    DateTimeFormatter.ISO_OFFSET_DATE_TIME,
    DateTimeFormatter.ofLocalizedDateTime( FormatStyle.FULL ),
    DateTimeFormatter.ofLocalizedDateTime( FormatStyle.LONG ),
    DateTimeFormatter.ofLocalizedDateTime( FormatStyle.MEDIUM ),
    DateTimeFormatter.ofLocalizedDateTime( FormatStyle.SHORT ),
  };

  private static final DateTimeFormatter[] DATE_FORMATTERS = {
    DateTimeFormatter.ISO_DATE,
    DateTimeFormatter.ISO_LOCAL_DATE,
    DateTimeFormatter.ISO_OFFSET_DATE,
    DateTimeFormatter.ofLocalizedDate( FormatStyle.FULL ),
    DateTimeFormatter.ofLocalizedDate( FormatStyle.LONG ),
    DateTimeFormatter.ofLocalizedDate( FormatStyle.MEDIUM ),
    DateTimeFormatter.ofLocalizedDate( FormatStyle.SHORT ),
  };

  private static final DateTimeFormatter[] TIME_FORMATTERS = {
    DateTimeFormatter.ISO_TIME,
    DateTimeFormatter.ISO_LOCAL_TIME,
    DateTimeFormatter.ISO_OFFSET_TIME,
    DateTimeFormatter.ofLocalizedTime( FormatStyle.FULL ),
    DateTimeFormatter.ofLocalizedTime( FormatStyle.LONG ),
    DateTimeFormatter.ofLocalizedTime( FormatStyle.MEDIUM ),
    DateTimeFormatter.ofLocalizedTime( FormatStyle.SHORT ),
  };


  public static LocalDateTime parseDateTime( String data )
  {
    if( data.length() > 80 || data.length() < 6 || isAlpha( data ) || isNumeric( data ) )
    {
      return null;
    }

    for( DateTimeFormatter formatter: DATE_TIME_FORMATTERS )
    {
      try
      {
        return LocalDateTime.parse( data, formatter );
      }
      catch( DateTimeParseException ignore )
      {
      }
    }
    return null;
  }

  public static LocalDate parseDate( String data )
  {
    if( data.length() > 60 || data.length() < 4 || isAlpha( data ) || isNumeric( data ) )
    {
      return null;
    }

    for( DateTimeFormatter formatter: DATE_FORMATTERS )
    {
      try
      {
        return LocalDate.parse( data, formatter );
      }
      catch( DateTimeParseException ignore )
      {
      }
    }
    return null;
  }

  public static LocalTime parseTime( String data )
  {
    if( data.length() > 30 || isAlpha( data ) || isAlpha( data ) || isNumeric( data ) )
    {
      return null;
    }

    for( DateTimeFormatter formatter: TIME_FORMATTERS )
    {
      try
      {
        return LocalTime.parse( data, formatter );
      }
      catch( DateTimeParseException ignore )
      {
      }
    }
    return null;
  }
}
