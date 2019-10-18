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
   * Pi is the ratio: Circumference/Diameter.
   */
  Rational pi = Rational.get( "3.141592653589793" );

  /**
   * Phi is the ratio: A/B = B/C where A = B + C
   */
  Rational phi = Rational.get( "1.618033988749895" );

  /**
   * Fine structure constant, the coupling constant for the electromagnetic force.
   */
  Rational kA = Rational.get( "0.0072973525693" );

  /**
   * The mole is a unit which measures the number of particles in a substance.
   */
  Rational mol = Rational.get( "6.02214076e23" );
}
