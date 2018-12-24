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

/**
 * Implements the lazy initialization pattern.
 * No locking of any kind is used.
 */
public abstract class LocklessLazyVar<T>
{
  protected final static Object NULL = new Object();
  private volatile T _val = null;

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
      result = init();
      if( result == null )
      {
        _val = (T)NULL;
      }
      else
      {
        _val = result;
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
    T hold = _val;
    _val = null;
    return hold;
  }

  protected void initDirectly( T val )
  {
    _val = val;
  }

  public boolean isLoaded()
  {
    return _val != null;
  }

  public interface LazyVarInit<Q>
  {
    Q init();
  }

  /**
   * Creates a new LockingLazyVar based on the type of the LazyVarInit passed in.
   */
  public static <Q> LocklessLazyVar<Q> make( final LazyVarInit<Q> closure )
  {
    return new LocklessLazyVar<Q>()
    {
      protected Q init()
      {
        return closure.init();
      }
    };
  }

}
