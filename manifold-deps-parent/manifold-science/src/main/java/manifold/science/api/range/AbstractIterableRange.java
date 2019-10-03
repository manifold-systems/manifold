

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

package manifold.science.api.range;

import java.util.Iterator;

public abstract class AbstractIterableRange<E extends Comparable<E>, S, U, ME extends AbstractIterableRange<E, S, U, ME>> extends AbstractRange<E, ME> implements IterableRange<E, S, U, ME>
{
  private S _step;
  private U _unit;

  public AbstractIterableRange( E left, E right, S step )
  {
    this( left, right, step, null, true, true, false );
  }

  public AbstractIterableRange( E left, E right, S step, U unit, boolean bLeftClosed, boolean bRightClosed, boolean bReverse )
  {
    super( left, right, bReverse ? bRightClosed : bLeftClosed, bReverse ? bLeftClosed : bRightClosed, bReverse );

    _step = step;
    _unit = unit;
  }

  @Override
  public Iterator<E> iterator()
  {
    if( isReverse() )
    {
      return iterateFromRight();
    }
    else
    {
      return iterateFromLeft();
    }
  }
  
  @Override
  public S getStep()
  {
    return _step;
  }
  @Override
  public ME step( S s )
  {
    _step = s;
    //noinspection unchecked
    return (ME)this;
  }

  @Override
  public U getUnit()
  {
    return _unit;
  }
  @Override
  public ME unit( U u )
  {
    _unit = u;
    //noinspection unchecked
    return (ME)this;
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( !(o instanceof AbstractIterableRange) )
    {
      return false;
    }
    if( !super.equals( o ) )
    {
      return false;
    }

    AbstractIterableRange that = (AbstractIterableRange)o;

    if( _step != null ? !_step.equals( that._step ) : that._step != null )
    {
      return false;
    }
    return !(_unit != null ? !_unit.equals( that._unit ) : that._unit != null);
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + (_step != null ? _step.hashCode() : 0);
    result = 31 * result + (_unit != null ? _unit.hashCode() : 0);
    return result;
  }

  @Override
  public String toString()
  {
    return super.toString() + " step " + getStep();
  }
}