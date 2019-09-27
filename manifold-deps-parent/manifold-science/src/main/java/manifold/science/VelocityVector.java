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

import manifold.science.util.Rational;

public final class VelocityVector extends Vector<Velocity, VelocityUnit, VelocityVector>
{
  public VelocityVector( Velocity magnitude, Angle angle )
  {
    super( magnitude, angle );
  }

  @Override
  public VelocityVector make( Velocity magnitude, Angle angle )
  {
    return new VelocityVector( magnitude, angle );
  }

  @Override
  public VelocityVector copy( Rational magnitude )
  {
    return new VelocityVector(
      new Velocity( magnitude, getMagnitude().getBaseUnit(), getMagnitude().getUnit() ), getAngle() );
  }
}
