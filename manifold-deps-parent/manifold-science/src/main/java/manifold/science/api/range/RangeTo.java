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

public class RangeTo
{
  public static class To
  {
    private static final To INSTANCE = new To();

    public static To instance()
    {
      return INSTANCE;
    }

    boolean _leftClosed;
    boolean _rightClosed;

    public To()
    {
      _leftClosed = true;
      _rightClosed = true;
    }

    public <E extends Sequenceable<E, S, U>, S, U> From<E, S, U> postfixBind( E sequenceable )
    {
      return new From<>( sequenceable );
    }

    public class From<E extends Sequenceable<E, S, U>, S, U>
    {
      private E _start;

      From( E sequenceable )
      {
        _start = sequenceable;
      }

      public SequenceableRange<E, S, U> prefixBind( E end )
      {
        return new SequenceableRange<>( _start, end, null, null, _leftClosed, _rightClosed, _start.compareTo( end ) > 0 );
      }
    }
  }
  public static class _To extends To
  {
    private static final _To INSTANCE = new _To();

    public static _To instance()
    {
      return INSTANCE;
    }

    _To()
    {
      _leftClosed = false;
    }
  }
  public static class To_ extends To
  {
    private static final To_ INSTANCE = new To_();

    public static To_ instance()
    {
      return INSTANCE;
    }

    To_()
    {
      _rightClosed = false;
    }
  }
  public static class _To_ extends To
  {
    private static final _To_ INSTANCE = new _To_();

    public static _To_ instance()
    {
      return INSTANCE;
    }

    _To_()
    {
      _leftClosed = false;
      _rightClosed = false;
    }
  }
}
