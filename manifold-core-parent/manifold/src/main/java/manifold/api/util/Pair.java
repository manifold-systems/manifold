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

package manifold.api.util;

public class Pair<F, S>
{
  final F _first;
  final S _second;

  public Pair( F first, S second )
  {
    _first = first;
    _second = second;
  }

  public F getFirst()
  {
    return _first;
  }

  public S getSecond()
  {
    return _second;
  }

  public static <T, V> Pair<T, V> make( T f, V s )
  {
    return new Pair<>( f, s );
  }

  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( !(o instanceof Pair) )
    {
      return false;
    }

    Pair pair = (Pair)o;

    if( _first != null ? !_first.equals( pair._first ) : pair._first != null )
    {
      return false;
    }
    return _second != null ? _second.equals( pair._second ) : pair._second == null;
  }

  public int hashCode()
  {
    int result;
    result = (_first != null ? _first.hashCode() : 0);
    result = 31 * result + (_second != null ? _second.hashCode() : 0);
    return result;
  }

  @Override
  public String toString()
  {
    return "(" + _first + ", " + _second + ")";
  }

}