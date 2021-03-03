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
import java.util.Arrays;

/**
 * Options used with {@link get}, {@link set}.
 */
public enum PropOption
{
  /**
   * For use with {@link get}, {@link set} to override the non-use of {@code abstract} modifier on the {@code @var}
   * property, or individually with {@link get} and {@link set}. If specified, the generated get/set method will be
   * abstract. Note only properties declared in interfaces and abstract classes can be {@code abstract}. If the
   * corresponding get/set method is user-defined, it must be declared {@code abstract}.
   * <pre><code>
   *    // only the setter method is abstract
   *   {@literal @}var @set(Abstract) String name;
   * </code></pre>
   */
  Abstract( Modifier.ABSTRACT ),

  /**
   * For use with {@link get}, {@link set} to override the non-use of {@code final} modifier on the {@code @var}
   * property, or individually with {@link get} and {@link set}. If specified, the generated get/set method will be
   * final. Note properties declared in interfaces cannot be {@code final}. If the corresponding get/set method is
   * user-defined, it must be declared {@code final}.
   * <pre><code>
   *    // only the setter is final
   *   {@literal @}var @set(Final) String name;
   * </code></pre>
   */
  Final( Modifier.FINAL ),

  /**
   * For use with {@link get}, {@link set} to override the property's declared accessibility.
   * If specified, the generated get and/or set methods will be {@code private}.
   * If the corresponding get/set method is user-defined, it must be declared {@code private}.
   */
  Private( Modifier.PRIVATE ),
  /**
   * For use with {@link get}, {@link set} to override the property's declared accessibility.
   * If specified, the generated get and/or set methods will be <i>package-private</i>.
   * If the corresponding get/set method is user-defined, it must be declared {@code package-private}.
   */
  Package( 0 ),
  /**
   * For use with {@link get}, {@link set} to override the property's declared accessibility.
   * If specified, the generated get and/or set methods will be {@code protected}.
   * If the corresponding get/set method is user-defined, it must be declared {@code protected}.
   */
  Protected( Modifier.PROTECTED ),
  /**
   * For use with {@link get}, {@link set} to override the property's declared accessibility.
   * If specified, the generated get and/or set methods will be {@code public}.
   * If the corresponding get/set method is user-defined, it must be declared {@code public}.
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

  public static PropOption fromModifier( int modifier )
  {
    return Arrays.stream( PropOption.values() )
      .filter( e -> e.getModifier() == modifier )
      .findFirst()
      .orElseThrow( () -> new IllegalArgumentException( "Bad modifier: " + modifier ) );
  }
}
