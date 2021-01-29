/*
 * Copyright (c) 2021 - Manifold Systems LLC
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

package manifold.util;

import java.lang.reflect.Method;
import java.util.List;

public final class MethodScore implements Comparable<MethodScore>
{
  private int _score;
  private boolean _errant;
  private boolean _valid;
  private Method _method;
  private Class _receiverType;
  private List<Class> _paramTypes;

  public MethodScore( Class receiverType )
  {
    _receiverType = receiverType;
  }

  /**
   * @return true if this score represents a matching method score rather than
   * just a placeholder indicating that no method matched
   */
  public boolean isValid()
  {
    return _valid;
  }
  public void setValid( boolean valid )
  {
    _valid = valid;
  }

  public int getScore()
  {
    return _score;
  }
  public void setScore( int score )
  {
    _score = score;
  }
  public void incScore( int amount )
  {
    _score += amount;
  }

  /**
   * @return true iff the method is call-compatible with the arguments.
   */
  public boolean isErrant()
  {
    return _errant;
  }
  public void setErrant( boolean errant )
  {
    _errant = errant;
  }


  public Method getMethod()
  {
    return _method;
  }
  public void setMethod( Method method )
  {
    _method = method;
  }

  public Class getReceiverType()
  {
    return _receiverType;
  }

  public int compareTo( MethodScore o )
  {
    // if the scores are the same, compare their signatures for great stability justice
    if( _score == o._score )
    {
      return o._method.toString().compareTo( _method.toString() );
    }
    else
    {
      return _score > o._score ? 1 : -1;
    }
  }

  public List<Class> getParameterTypes()
  {
    return _paramTypes;
  }
  public void setParameterTypes( List<Class> paramTypes )
  {
    _paramTypes = paramTypes;
  }

  public boolean matchesArgSize()
  {
    return _method.getParameterTypes().length == _paramTypes.size();
  }
}