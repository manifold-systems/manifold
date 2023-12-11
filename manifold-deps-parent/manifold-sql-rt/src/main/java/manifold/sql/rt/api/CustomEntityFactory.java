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
   * Constructs classes implementing the entity API interfaces.
   * <p/>
   * Entity interfaces are self-implementing; default methods delegate to bindings. As such, a custom entity class
   * can focus on custom behavior and not be concerned about delegating existing behavior.
   * <p/>
   * A typical custom entity class.
   * <pre><code>
   *     import org.example.schema.mydatabase.Actor;
   *     import manifold.sql.rt.api.*;
   *
   *     public class MyActor extends BaseEntity implements Actor {
   *       private MyActor(TxBindings bindings) {
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
