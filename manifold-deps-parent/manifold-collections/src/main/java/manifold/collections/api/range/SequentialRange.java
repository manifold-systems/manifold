

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

public class SequentialRange<E extends Sequential<E, S, U>, S, U> extends AbstractIterableRange<E, S, U, SequentialRange<E, S, U>>
{
  public SequentialRange( E left, E right, S step, U unit, boolean leftClosed, boolean rightClosed, boolean reverse )
  {
    super( left, right, step, unit, reverse ? rightClosed : leftClosed, reverse ? leftClosed : rightClosed, reverse );
  }

  @Override
  public Iterator<E> iterateFromLeft()
  {
    return new SequentialIterator();
  }

  @Override
  public Iterator<E> iterateFromRight()
  {
    return new ReverseSequentialIterator();
  }

  @Override
  public E getFromLeft( int iStepIndex )
  {
    if( iStepIndex < 0 )
    {
      throw new IllegalArgumentException( "Step index must be >= 0: " + iStepIndex );
    }

    if( !isLeftClosed() )
    {
      iStepIndex++;
    }
    E value = getLeftEndpoint().nextNthInSequence( getStep(), getUnit(), iStepIndex );
    int iComp = value.compareTo( getRightEndpoint() );
    if( isRightClosed() ? iComp <= 0 : iComp < 0 )
    {
      return value;
    }

    return null;
  }

  @Override
  public E getFromRight( int iStepIndex )
  {
    if( iStepIndex < 0 )
    {
      throw new IllegalArgumentException( "Step index must be >= 0: " + iStepIndex );
    }

    if( !isRightClosed() )
    {
      iStepIndex++;
    }
    E value = getRightEndpoint().previousNthInSequence( getStep(), getUnit(), iStepIndex );
    int iComp = value.compareTo( getLeftEndpoint() );
    if( isLeftClosed() ? iComp >= 0 : iComp > 0 )
    {
      return value;
    }

    return null;
  }

  private class SequentialIterator implements Iterator<E>
  {
    private E _csr;

    public SequentialIterator()
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
      if( _csr == null )
      {
        return false;
      }
      int iComp = _csr.compareTo( getRightEndpoint() );
      return iComp < 0 || (isRightClosed() && iComp == 0);
    }

    @Override
    public E next()
    {
      int iComp = _csr.compareTo( getRightEndpoint() );
      if( iComp > 0 ||
          (!isRightClosed() && iComp == 0) )
      {
        throw new NoSuchElementException();
      }
      E ret = _csr;
      _csr = _csr.nextInSequence( getStep(), getUnit() );
      return ret;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  private class ReverseSequentialIterator implements Iterator<E>
  {
    private E _csr;

    public ReverseSequentialIterator()
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
      if( _csr == null )
      {
        return false;
      }

      int iComp = _csr.compareTo( getLeftEndpoint() );
      return iComp > 0 || (isLeftClosed() && iComp == 0);
    }

    @Override
    public E next()
    {
      int iComp = _csr.compareTo( getLeftEndpoint() );
      if( iComp < 0 ||
          (!isLeftClosed() && iComp == 0) )
      {
        throw new NoSuchElementException();
      }
      E ret = _csr;
      _csr = _csr.previousInSequence( getStep(), getUnit() );
      return ret;
    }

    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }
}