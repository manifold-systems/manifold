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

import manifold.science.api.AbstractProductUnit;
import manifold.science.api.UnitCache;
import manifold.science.util.Rational;


import static manifold.science.MassUnit.Kilogram;
import static manifold.science.MetricScaleUnit.r;

public final class ForceUnit extends AbstractProductUnit<MassUnit, AccelerationUnit, Force, ForceUnit>
{
  private static final UnitCache<ForceUnit> CACHE = new UnitCache<>();

  public static final ForceUnit N = get( Kilogram, AccelerationUnit.BASE, 1r, "Newton", "N" );

  public static final ForceUnit BASE = N;

  public static ForceUnit get( MassUnit massUnit, AccelerationUnit accUnit ) {
    return get( massUnit, accUnit, null, null, null );
  }
  public static ForceUnit get( MassUnit massUnit, AccelerationUnit accUnit, Rational factor, String name, String symbol ) {
    ForceUnit unit = new ForceUnit( massUnit, accUnit, factor, name, symbol );
    return CACHE.get( unit );
  }
    
  private ForceUnit( MassUnit massUnit, AccelerationUnit accUnit, Rational factor, String name, String symbol ) {
    super( massUnit, accUnit, factor, name, symbol );
  }

  @Override
  public Force makeDimension( Number amount )
  {
    return new Force( Rational.get( amount ), this );
  }

  public MassUnit getMassUnit() {
    return getLeftUnit();
  }
  public AccelerationUnit getAccUnit() {
    return getRightUnit();
  }
        
  public PowerUnit times( VelocityUnit v ) {
    return PowerUnit.get( this * v.getLengthUnit(), v.getTimeUnit() );
  }

  public EnergyUnit times( LengthUnit len ) {
    return EnergyUnit.get( this, len );
  }

  public MomentumUnit times( TimeUnit t ) {
    return MomentumUnit.get( getMassUnit(), VelocityUnit.get( getAccUnit().getVelocityUnit().getLengthUnit(), t ) );
  }
  
  public MassUnit div( AccelerationUnit acc ) {
    return getMassUnit();
  }
}
