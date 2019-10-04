

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

package manifold.collections.api.range;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class DoubleRange extends NumberRange<Double, DoubleRange>
{
  public DoubleRange( Double left, Double right )
  {
    this( left, right, 1 );
  }

  public DoubleRange( Double left, Double right, double step )
  {
    this( left, right, step, true, true, false );
  }

  public DoubleRange( Double left, Double right, double step, boolean leftClosed, boolean rightClosed, boolean reverse )
  {
    super( left, right, step, leftClosed, rightClosed, reverse );

    if( step <= 0 )
    {
      throw new IllegalArgumentException( "The step must be greater than 0: " + step );
    }
  }

  @Override
  public Iterator<Double> iterateFromLeft()
  {
    return new ForwardIterator();
  }

  @Override
  public Iterator<Double> iterateFromRight()
  {
    return new ReverseIterator();
  }

  @Override
  public Double getFromLeft( int iStepIndex )
  {
    if( iStepIndex < 0 )
    {
      throw new IllegalArgumentException( "Step index must be >= 0: " + iStepIndex );
    }

    if( !isLeftClosed() )
    {
      iStepIndex++;
    }
    double value = getLeftEndpoint() + getStep() * iStepIndex;
    if( isRightClosed() ? value <= getRightEndpoint() : value < getRightEndpoint() )
    {
      return value;
    }

    return null;
  }

  @Override
  public Double getFromRight( int iStepIndex )
  {
    if( iStepIndex < 0 )
    {
      throw new IllegalArgumentException( "Step index must be >= 0: " + iStepIndex );
    }

    if( !isRightClosed() )
    {
      iStepIndex++;
    }
    double value = getRightEndpoint() - getStep() * iStepIndex;
    if( isLeftClosed() ? value >= getLeftEndpoint() : value > getLeftEndpoint() )
    {
      return value;
    }

    return null;
  }

  public class ForwardIterator implements Iterator<Double>
  {
    private double _csr;

    ForwardIterator()
    {
      _csr = getLeftEndpoint();
      if( !isLeftClosed() && hasNext() )
      {
        next();
      }
    }

    @Override
    public boolean hasNext()
    {
      return _csr < getRightEndpoint() || (isRightClosed() && _csr == getRightEndpoint());
    }

    @Override
    public Double next()
    {
      if( _csr > getRightEndpoint() ||
          (!isRightClosed() && _csr == getRightEndpoint()) )
      {
        throw new NoSuchElementException();
      }
      double ret = _csr;
      _csr = _csr + getStep();
      return ret;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  private class ReverseIterator implements Iterator<Double>
  {
    private double _csr;

    ReverseIterator()
    {
      _csr = getRightEndpoint();
      if( !isRightClosed() && hasNext() )
      {
        next();
      }
    }

    @Override
    public boolean hasNext()
    {
      return _csr > getLeftEndpoint() || (isLeftClosed() && _csr == getLeftEndpoint());
    }

    @Override
    public Double next()
    {
      if( _csr < getLeftEndpoint() ||
          (!isLeftClosed() && _csr == getLeftEndpoint()) )
      {
        throw new NoSuchElementException();
      }
      double ret = _csr;
      _csr = _csr - getStep();
      return ret;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}
