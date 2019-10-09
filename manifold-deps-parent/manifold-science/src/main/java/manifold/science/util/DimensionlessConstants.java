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

public interface DimensionlessConstants
{
  /**
   * PI at 100 decimal places
   */
  Rational pi = Rational.get(
    "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679" );

  /**
   * PHI at 100 decimal places
   */
  Rational phi = Rational.get(
    "1.6180339887498948482045868343656381177203091798057628621354486227052604628189024497072072041893911374" );

  /**
   * The mole, abbreviated mol, is an SI unit which measures the number of particles in a specific substance.
   */
  Rational mol = Rational.get( "6.02214076e23" );
}
