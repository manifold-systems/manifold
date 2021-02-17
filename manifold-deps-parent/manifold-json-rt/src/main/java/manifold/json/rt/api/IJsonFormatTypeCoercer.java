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

package manifold.json.rt.api;

import manifold.ext.rt.CoercionProviders;
import manifold.ext.rt.api.ICoercionProvider;
import manifold.json.rt.DefaultCoercer;
import manifold.rt.api.util.Pair;
import manifold.rt.api.util.ServiceUtil;
import manifold.util.concurrent.LocklessLazyVar;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implement this interface as a <a href="https://docs.oracle.com/javase/tutorial/ext/basics/spi.html#register-service-providers">service provider</a>.
 * Provide a mapping from a JSON Schema {@code "format"} to a Java type. For instance, <p>
 * the {@code "date-time"} format maps to {@link java.time.LocalDateTime}
 * <p>
 * See {@link DefaultCoercer} to see how {@code "date-time"} and other formats are mapped to a set of Java types.
 * <p>
 * Register one or more of your {@code IJsonFormatTypeCoercer} implementations in a file named:<b>
 * <pre>META-INF/services/manifold.ext.rt.api.ICoercionProvider</pre>
 * This file contains the fully qualified names of your implementations, one per line e.g.,
 * <pre>
 * com.example.FooFormatTypeCoercer
 * com.example.BarFormatTypeCoercer
 * </pre>
 */
public interface IJsonFormatTypeCoercer extends ICoercionProvider
{
  LocklessLazyVar<Set<ICoercionProvider>> _coercionProviders =
    LocklessLazyVar.make( () -> {
      Set<ICoercionProvider> registered = new HashSet<>();
      //!! for IJ plugin: need the classloader from here
      ServiceUtil.loadRegisteredServices( registered, ICoercionProvider.class, IJsonFormatTypeCoercer.class.getClassLoader() );
      return registered;
    } );

  LocklessLazyVar<List<IJsonFormatTypeCoercer>> _instances =
    LocklessLazyVar.make( () ->
      _coercionProviders.get().stream()
        .filter( e -> e instanceof IJsonFormatTypeCoercer )
        .map( e -> (IJsonFormatTypeCoercer)e )
        .collect( Collectors.toList() ) );

  static List<IJsonFormatTypeCoercer> get()
  {
    return _instances.get();
  }

  /**
   * @return Pairs of one or more format names to Java type such as {@code "date-time"->LocalDateTime.class} to be
   * referenced in Json Schema {@code "format"} types.
   */
  Map<String, Class<?>> getFormats();
}
