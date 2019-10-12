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

package manifold.science.util;

import static manifold.science.util.CoercionConstants.r;

public interface MetricFactorConstants
{
  // SI metric unit factors
  //
  Rational YOCTO = "1/1000000000000000000000000"r;
  Rational ZEPTO = "1/1000000000000000000000"r;
  Rational ATTO =  "1/1000000000000000000"r;
  Rational FEMTO = "1/1000000000000000"r;
  Rational PICO =  "1/1000000000000"r;
  Rational NANO =  "1/1000000000"r;
  Rational MICRO = "1/1000000"r;
  Rational MILLI = "1/1000"r;
  Rational CENTI = "1/100"r;
  Rational DECI =  "1/10"r;
  Rational DECA =  10r;
  Rational HECTO = 100r;
  Rational KILO =  1000r;
  Rational MEGA =  1000000r;
  Rational GIGA =  1000000000r;
  Rational TERA =  "1000000000000"r;
  Rational PETA =  "1000000000000000"r;
  Rational EXA =   "1000000000000000000"r;
  Rational ZETTA = "1000000000000000000000"r;
  Rational YOTTA = "1000000000000000000000000"r;

  // Byte unit factors for quantities of digital information (International Electrotechnical Commission)
  //
  Rational KIBI = 1024r; // kibibyte (prev. kilobyte)
  Rational MEBI = KIBI.pow( 2 );
  Rational GIBI = KIBI.pow( 3 );
  Rational TEBI = KIBI.pow( 4 );
  Rational PEBI = KIBI.pow( 5 );
  Rational EXBI = KIBI.pow( 6 );
  Rational ZEBI = KIBI.pow( 7 );
  Rational YOBI = KIBI.pow( 8 );
}
