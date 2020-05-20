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

import java.util.AbstractList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegExpMatch extends AbstractList<String> implements List<String>
{
  private Matcher _matcher;

  public RegExpMatch( Matcher matcher )
  {
    _matcher = matcher;
  }

  public String get( int index )
  {
    return _matcher.group( index + 1 );
  }

  public int size()
  {
    return _matcher.groupCount();
  }

  public Matcher getMatcher()
  {
    return _matcher;
  }

  public Pattern getPattern()
  {
    return _matcher.pattern();
  }

  /**
   * @deprecated RegExpMatch now implements List&lt;String>, so it is
   * no longer necessary to call getGroups()
   */
  public List<String> getGroups()
  {
    return this;
  }

  @Override
  public String toString()
  {
    return "[RegExpMatch: " + _matcher + "]";
  }
}
