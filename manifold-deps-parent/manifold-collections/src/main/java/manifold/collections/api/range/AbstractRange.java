

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

public abstract class AbstractRange<E extends Comparable<E>, ME extends AbstractRange<E, ME>> implements Range<E, ME>
{
  final private E _left;
  final private E _right;
  final private boolean _leftClosed;
  final private boolean _rightClosed;
  final private boolean _reversed;

  public AbstractRange( E left, E right )
  {
    this( left, right, true, true, false );
  }

  public AbstractRange( E left, E right, boolean leftClosed, boolean rightClosed, boolean reverse )
  {
    checkArgs( left, right );
    _reversed = reverse;
    if( reverse )
    {
      _left = right;
      _right = left;

      _leftClosed = rightClosed;
      _rightClosed = leftClosed;
    }
    else
    {
      _left = left;
      _right = right;

      _leftClosed = leftClosed;
      _rightClosed = rightClosed;
    }

    if( _left.compareTo( _right ) > 0 )
    {
      throw new IllegalArgumentException(
        "The logical left endpoint is greater than the logical right endpoint: [" + left + ", " + right + "]" );
    }
  }

  private void checkArgs( E left, E right )
  {
    if( left == null )
    {
      throw new IllegalArgumentException( "Non-null value expected for left endpoint." );
    }
    if( right == null )
    {
      throw new IllegalArgumentException( "Non-null value expected for right endpoint." );
    }
  }

  @Override
  public E getLeftEndpoint()
  {
    return _left;
  }

  @Override
  public E getRightEndpoint()
  {
    return _right;
  }

  @Override
  public boolean isLeftClosed()
  {
    return _leftClosed;
  }

  @Override
  public boolean isRightClosed()
  {
    return _rightClosed;
  }

  @Override
  public boolean contains( E e )
  {
    return (isLeftClosed()
            ? getLeftEndpoint().compareTo( e ) <= 0
            : getLeftEndpoint().compareTo( e ) < 0) &&
           (isRightClosed()
            ? getRightEndpoint().compareTo( e ) >= 0
            : getRightEndpoint().compareTo( e ) > 0);
  }

  @Override
  public boolean contains( ME range )
  {
    return (isLeftClosed()
            ? getLeftEndpoint().compareTo( range.getLeftEndpoint() ) <= 0
            : range.isLeftClosed()
              ? getLeftEndpoint().compareTo( range.getLeftEndpoint() ) < 0
              : getLeftEndpoint().compareTo( range.getLeftEndpoint() ) <= 0) &&
           (isRightClosed()
            ? getRightEndpoint().compareTo( range.getRightEndpoint() ) >= 0
            : range.isRightClosed()
              ? getRightEndpoint().compareTo( range.getRightEndpoint() ) > 0
              : getRightEndpoint().compareTo( range.getRightEndpoint() ) >= 0);
  }

  @Override
  public boolean isReversed()
  {
    return _reversed;
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( !(o instanceof AbstractRange) )
    {
      return false;
    }

    AbstractRange that = (AbstractRange)o;

    if( isLeftClosed() != that.isLeftClosed() )
    {
      return false;
    }
    if( isReversed() != that.isReversed() )
    {
      return false;
    }
    if( isRightClosed() != that.isRightClosed() )
    {
      return false;
    }
    if( !getLeftEndpoint().equals( that.getLeftEndpoint() ) )
    {
      return false;
    }
    return getRightEndpoint().equals( that.getRightEndpoint() );

  }

  @Override
  public int hashCode()
  {
    int result = getLeftEndpoint().hashCode();
    result = 31 * result + getRightEndpoint().hashCode();
    result = 31 * result + (isLeftClosed() ? 1 : 0);
    result = 31 * result + (isRightClosed() ? 1 : 0);
    result = 31 * result + (isReversed() ? 1 : 0);
    return result;
  }

  @Override
  public String toString()
  {
    if( isReversed() )
    {
      return getRightEndpoint() + (isRightClosed() ? "" : "|") + ".." + (isLeftClosed() ? "" : "|") + getLeftEndpoint();
    }
    return getLeftEndpoint() + (isLeftClosed() ? "" : "|") + ".." + (isRightClosed() ? "" : "|") + getRightEndpoint();
  }
}