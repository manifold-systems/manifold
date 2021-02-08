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

package manifold.ext.props.rt.api;

import java.lang.reflect.Modifier;

/**
 * Options used with {@link prop}, {@link get}, {@link set}.
 */
public enum PropOption
{
  /**
   * For use with {@link prop}, {@link get}, {@link set}. If specified, the generated get/set methods will be abstract.
   * Note only properties declared in interfaces and abstract classes can be {@code abstract}.
   */
  Abstract( Modifier.ABSTRACT ),

  /**
   * For use with {@link prop}, {@link get}, {@link set}. If specified, the generated get/set methods will be final.
   * Note only properties declared in classes, as opposed to interfaces, can be {@code final}.
   */
  Final( Modifier.FINAL ),

  /**
   * For use with {@link get}, {@link set} to override the property's declared accessibility.
   * If specified, the generated get and/or set methods will be {@code private}.
   */
  Private( Modifier.PRIVATE ),
  /**
   * For use with {@link get}, {@link set} to override the property's declared accessibility.
   * If specified, the generated get and/or set methods will be <i>package-private</i>.
   */
  Package( 0 ),
  /**
   * For use with {@link get}, {@link set} to override the property's declared accessibility.
   * If specified, the generated get and/or set methods will be {@code protected}.
   */
  Protected( Modifier.PROTECTED ),
  /**
   * For use with {@link get}, {@link set} to override the property's declared accessibility.
   * If specified, the generated get and/or set methods will be {@code public}.
   */
  Public( Modifier.PUBLIC );

  public static final PropOption[] EMPTY_ARRAY = new PropOption[0];

  private int _modifier;

  PropOption( int modifier )
  {
    _modifier = modifier;
  }

  /**
   * @return The Java {@link Modifier} value corresponding with the option.
   */
  public int getModifier()
  {
    return _modifier;
  }

  public int or( PropOption option )
  {
    return or( option.getModifier() );
  }
  public int or( int modifier )
  {
    return getModifier() | modifier;
  }
}
