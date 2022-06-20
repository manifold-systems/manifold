/*
 * Copyright (c) 2022 - Manifold Systems LLC
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

package manifold.tuple.rt.api;

import manifold.ext.rt.api.Self;
import manifold.util.ManExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

/**
 * Root interface for tuples.
 * <p/>
 * See {@link manifold.tuple.rt.internal.GeneratedTuple}
 */
public interface Tuple extends Iterable<TupleItem>
{
  /**
   * The tuple's labels, natural order.
   */
  List<String> orderedLabels();

  /**
   * The tuple's values, natural order.
   */
  List<?> orderedValues();

  /**
   * The tuple's name/value pairs, natural order.
   */
  @Override
  Iterator<TupleItem> iterator();

  /**
   * @return A shallow copy of this tuple. The return type is the receiver's type.
   */
  default @Self Tuple copy()
  {
    try
    {
      return (Tuple)getClass().getConstructors()[0].newInstance( orderedValues().toArray() );
    }
    catch( Exception e )
    {
      throw ManExceptionUtil.unchecked( e );
    }
  }
}
