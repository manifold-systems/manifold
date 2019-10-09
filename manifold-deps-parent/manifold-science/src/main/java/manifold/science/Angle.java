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

import manifold.science.api.AbstractMeasure;
import manifold.science.util.Rational;

final public class Angle extends AbstractMeasure<AngleUnit, Angle>
{
  public Angle( Rational value, AngleUnit unit, AngleUnit displayUnit )
  {
    super( value, unit, displayUnit );
  }

  public Angle( Rational value, AngleUnit unit )
  {
    this( value, unit, unit );
  }

  @Override
  public AngleUnit getBaseUnit()
  {
    return AngleUnit.BASE;
  }

  @Override
  public Angle make( Rational value, AngleUnit unit, AngleUnit displayUnit )
  {
    return new Angle( value, unit, displayUnit );
  }

  @Override
  public Angle make( Rational value, AngleUnit unit )
  {
    return new Angle( value, unit );
  }

  //@BinderSeparators( :accepted = {":"} )
  public LengthVector postfixBind( Length len )
  {
    return new LengthVector( len, this );
  }

  //@BinderSeparators( :accepted = {":"} )
  public TimeVector postfixBind( Time t )
  {
    return new TimeVector( t, this );
  }

  //@BinderSeparators( :accepted = {":"} )
  public VelocityVector postfixBind( Velocity v )
  {
    return new VelocityVector( v, this );
  }

  public Frequency div( Time time )
  {
    return new Frequency( toBaseNumber() / time.toBaseNumber(), FrequencyUnit.BASE, FrequencyUnit.get( getDisplayUnit(), time.getDisplayUnit() ) );
  }

  public Time div( Frequency freq )
  {
    return new Time( toBaseNumber() / freq.toBaseNumber(), TimeUnit.BASE, freq.getDisplayUnit().getTimeUnit() );
  }
}
