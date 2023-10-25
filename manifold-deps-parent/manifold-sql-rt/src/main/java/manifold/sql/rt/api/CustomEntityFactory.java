/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.sql.rt.api;

public interface CustomEntityFactory
{
  /**
   * Construct a custom entity class that implements {@code entityInterface} replacing the default class.
   * <p/>
   * The interface is entirely self-implementing minus the {@code getBindings()} method, which must be implemented by
   * all custom entity classes. Thus, a custom class must provide at least the following functionality:
   * <pre><code>
   *     import org.example.schema.sampledb.Actor;
   *     import manifold.sql.rt.api.TxBindings;
   *
   *     public class CustomActor implements Actor {
   *       private final TxBindings bindings;
   *       private CustomActor(TxBindings bindings) {
   *         this.bindings = bindings;
   *       }
   *       public TxBindings getBindings() {
   *         return bindings;
   *       }
   *     } </code></pre>
   * The custom class may override any of the {@code entityInterface} methods and provide any number of its own features.
   * <B>However</B>, the class should not add any state other than the TxBindings that is provided.
   *
   * @param txBindings The TxBindings instance the returned class instance receives in its constructor. This instance
   *                   must be maintained by this class and returned from the {@code getBindings()} method.
   * @param entityInterface The interface the class of the returned entity instance must implement.
   * @return An instance of the {@code entityInterface} or null the entity is not customized.
   * @param <T> The entity interface to implement.
   */
  <T extends TableRow> T newInstance( TxBindings txBindings, Class<T> entityInterface );
}
