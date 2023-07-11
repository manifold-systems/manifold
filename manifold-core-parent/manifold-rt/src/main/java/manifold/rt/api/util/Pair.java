/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.rt.api.util;

import java.util.Objects;

/**
 * A simple class to type-safely model a pair of values.
 * @param <F> type of first value
 * @param <S> type of second value
 */
public class Pair<F, S>
{
  /** {@code and} is a "binding" constant that enables clean syntax for creating pairs: <br>
   * <pre><code>Pair&lt;String,Integer&gt; pair = "Moe" and 88;</code></pre>
   * <br>
   * Use with {@link java.util.Map}{@code #mapOf} extension method via {@code manifold-collections} dependency.<br>
   * <pre><code>Map&lt;String,Integer&gt; map = Map.mapOf("Moe" and 77, "Larry" and 88, "Curly" and 99);</code></pre>
   */
  public static final And and = And.instance();

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

    if( !Objects.equals( _first, pair._first ) )
    {
      return false;
    }
    return Objects.equals( _second, pair._second );
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

  /**
   * Enables the: {@code first and second} syntax for {@code Pair}, which is particularly useful with the
   * {@link java.util.Map}{@code #mapOf} extension method.
   * <br>
   * <pre><code>Map&lt;String,Integer&gt; map = Map.mapOf("Moe" and 77, "Larry" and 88, "Curly" and 99);</code></pre>
   * </code></pre>
   */
  public static class And
  {
    private static final And INSTANCE = new And();
    public static And instance()
    {
      return INSTANCE;
    }

    private And()
    {
    }

    public <F> First<F> postfixBind( F first )
    {
      return new First<>( first );
    }

    public static class First<F>
    {
      private final F _first;

      private First( F left )
      {
        _first = left;
      }

      public F getFirst()
      {
        return _first;
      }

      @SuppressWarnings( "unused" )
      public <S> Pair<F, S> prefixBind( S second )
      {
        return new Pair<>( _first, second );
      }
    }
  }
}