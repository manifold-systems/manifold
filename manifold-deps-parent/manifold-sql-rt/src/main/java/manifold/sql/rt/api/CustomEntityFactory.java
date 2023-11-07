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
   * The interface is self-implementing; default methods delegate to bindings. As a consequence, a custom entity class
   * can focus on custom behavior and not be concerned about maintaining existing behavior.
   * <p/>
   * A typical custom entity class.
   * <pre><code>
   *     import org.example.schema.sampledb.Actor;
   *     import manifold.sql.rt.api.TxBindings;
   *
   *     public class CustomActor implements Actor extends BaseEntity {
   *       private CustomActor(TxBindings bindings) {
   *         super(bindings);
   *       }
   *
   *       // your code here...
   *
   *     } </code></pre>
   *
   * A custom class may override any of the {@code entityInterface} methods and provide any number of its own methods.
   *
   * @param txBindings The TxBindings instance the returned class instance receives in its constructor. This instance
   *                   must be maintained by this class and returned from the {@code getBindings()} method.
   * @param entityInterface The interface the class of the returned entity instance must implement.
   * @return An instance of the {@code entityInterface} or null the entity is not customized.
   * @param <T> The entity interface to implement.
   */
  <T extends Entity> T newInstance( TxBindings txBindings, Class<T> entityInterface );
}
