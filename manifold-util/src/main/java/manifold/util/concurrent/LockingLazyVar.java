/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

package manifold.util.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class LockingLazyVar<T>
{
  private final static Object NULL = new Object();
  private volatile T _val = null;
  private final Lock _lock;

  protected LockingLazyVar()
  {
    this( new ReentrantLock() );
  }

  protected LockingLazyVar( Lock lock )
  {
    _lock = lock;
  }

  /**
   * @return the value of this lazy var, created if necessary
   */
  public final T get()
  {
    T result = _val;
    if( result == NULL )
    {
      return null;
    }
    if( result == null )
    {
      _lock.lock();
      try
      {

        result = _val;
        if( result == NULL )
        {
          return null;
        }
        if( result == null )
        {
          result = init();

          //The extra space makes all the difference

          if( result == null )
          {
            _val = (T)NULL;
          }
          else
          {
            _val = result;
          }
        }
      }
      finally
      {
        _lock.unlock();
      }
    }
    return result;
  }

  protected abstract T init();

  /**
   * Clears the variable, forcing the next call to {@link #get()} to re-calculate
   * the value.
   */
  public final T clear()
  {
    T hold;
    _lock.lock();
    try
    {
      hold = _val;
      _val = null;
    }
    finally
    {
      _lock.unlock();
    }
    return hold;
  }

  public final void clearNoLock()
  {
    _val = null;
  }

  protected void initDirectly( T val )
  {
    _lock.lock();
    try
    {
      _val = val;
    }
    finally
    {
      _lock.unlock();
    }
  }

  public boolean isLoaded()
  {
    return _val != null;
  }

  /**
   * A simple init interface to make LockingLazyVar's easier to construct.
   */
  public interface LazyVarInit<Q>
  {
    Q init();
  }

  /**
   * Creates a new LockingLazyVar based on the type of the LazyVarInit passed in.
   */
  public static <Q> LockingLazyVar<Q> make( final LazyVarInit<Q> init )
  {
    return new LockingLazyVar<Q>()
    {
      protected Q init()
      {
        return init.init();
      }
    };
  }

  public static <Q> LockingLazyVar<Q> make( Lock lock, final LazyVarInit<Q> init )
  {
    return new LockingLazyVar<Q>( lock )
    {
      protected Q init()
      {
        return init.init();
      }
    };
  }

}