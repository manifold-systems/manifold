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

package manifold.science.measures;

import manifold.science.util.DimensionlessConstants;
import manifold.science.util.Rational;

public enum CommonDimensionlessUnit implements DimensionlessUnit
{
  /**
   * Pi is the ratio: Circumference/Diameter.
   */
  pi( DimensionlessConstants.pi, "pi", "\u03C0" ),

  /**
   * Phi is the ratio: A/B = B/C where A = B + C
   */
  phi( DimensionlessConstants.phi, "phi", "\u03A6" ),

  /**
   * Fine-structure constant, the coupling constant for the electromagnetic force.
   */
  kA( DimensionlessConstants.kA, "kA", "\u03B1" ),

  /**
   * The mole is a unit which measures the number of particles in a substance.
   */
  mol( DimensionlessConstants.mol, "mol", "mol" );

  private final Rational _amount;
  private final String _name;
  private final String _symbol;

  CommonDimensionlessUnit( Rational amount, String name, String symbol )
  {
    _amount = amount;
    _name = name;
    _symbol = symbol;
  }

  public Rational getAmount()
  {
    return _amount;
  }

  public String getUnitName()
  {
    return _name;
  }

  public String getUnitSymbol()
  {
    return _symbol;
  }
}
