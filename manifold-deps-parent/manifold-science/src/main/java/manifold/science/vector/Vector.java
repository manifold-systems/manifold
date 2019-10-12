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

package manifold.science.vector;

import manifold.science.Angle;
import manifold.science.AngleUnit;
import manifold.science.api.AbstractMeasure;
import manifold.science.api.Dimension;
import manifold.science.api.Unit;
import manifold.science.util.Rational;


import static manifold.science.AngleUnit.Degree;
import static manifold.science.AngleUnit.Radian;
import static manifold.science.util.CoercionConstants.r;
import static manifold.science.util.DimensionlessConstants.pi;

//!! Highly experimental!
public abstract class Vector<M extends AbstractMeasure<U, M>,
  U extends Unit<M, U>,
  V extends Vector<M, U, V>> implements Dimension<V>
{
  private final M _magnitude;
  private final Angle _angle;

  protected Vector( M magnitude, Angle angle )
  {
    _magnitude = magnitude;
    _angle = angle;
  }

  public abstract V make( M magnitude, Angle angle );

  public M getMagnitude()
  {
    return _magnitude;
  }

  public Angle getAngle()
  {
    return _angle;
  }

  public M getX()
  {
    // todo: add trig functions for Rational
    return _magnitude.fromBaseNumber( _magnitude.toBaseNumber() * Math.cos( _angle.to( Radian ).getValue().doubleValue() ) );
  }

  public M getY()
  {
    // todo: add trig functions for Rational
    return _magnitude.fromBaseNumber( _magnitude.toBaseNumber() * Math.sin( _angle.to( Radian ).getValue().doubleValue() ) );
  }

  public V unaryMinus()
  {
    return make( _magnitude, _angle + 180 Degree );
  }

  public V plus( V v )
  {
    //todo: for better accuracy use magnitude and angle components instead of x, y coords

    Rational x = getX().toBaseNumber() + v.getX().toBaseNumber();
    Rational y = getY().toBaseNumber() + v.getY().toBaseNumber();
    // todo: add trig functions for Rational
    Rational angle = x == 0 r ? x : Math.atan( (y / x).doubleValue() ) r;
    Rational mag = (x * x + y * y).sqrt();
    if( x < 0 r )
    {
      if( y < 0 r )
      {
        angle = angle - pi;
      }
      else
      {
        angle = angle + pi;
      }
    }
    return make( _magnitude.fromBaseNumber( mag ), new Angle( angle, Radian, _angle.getDisplayUnit() ) );
  }

  public V minus( V v )
  {
    return plus( -v );
  }

  public Rational times( V v )
  {
    Rational x = getX().toBaseNumber() * v.getX().toBaseNumber();
    Rational y = getY().toBaseNumber() * v.getY().toBaseNumber();
    return x + y;
  }

  public Rational div( V v )
  {
    throw new UnsupportedOperationException();
  }

  public V copy( U lu, AngleUnit au )
  {
    return make( _magnitude.make( _magnitude.toBaseNumber(), _magnitude.getBaseUnit(), lu ),
      new Angle( _angle.toBaseNumber(), _angle.getBaseUnit(), au ) );
  }

  public V fromNumber( Rational p0 )
  {
    return make( _magnitude.make( p0, _magnitude.getDisplayUnit() ), _angle );
  }

  public V fromBaseNumber( Rational p0 )
  {
    return make( _magnitude.make( p0, _magnitude.getBaseUnit(), _magnitude.getDisplayUnit() ), _angle );
  }

  public Rational toNumber()
  {
    return _magnitude.toNumber();
  }

  public Rational toBaseNumber()
  {
    return _magnitude.toBaseNumber();
  }

  public V to( U lu, AngleUnit au )
  {
    return copy( lu, au );
  }

  public String toString()
  {
    return _magnitude + " " + _angle;
  }

  public int hashCode()
  {
    return 31 * _magnitude.hashCode() + _angle.hashCode();
  }

  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null )
    {
      return false;
    }
    if( o.getClass() != getClass() )
    {
      return false;
    }
    //noinspection unchecked
    Vector that = (Vector<M, U, V>)o;
    return _magnitude == that._magnitude && _angle == that._angle;
  }

  public int compareTo( V o )
  {
    return _magnitude.compareTo( o.getMagnitude() );
  }

  @Override
  public EqualityMode equalityMode()
  {
    return EqualityMode.Equals;
  }
}
