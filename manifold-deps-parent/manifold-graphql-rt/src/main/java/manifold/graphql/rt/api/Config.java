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

package manifold.graphql.rt.api;

public class Config
{
  private static final Config INSTANCE = new Config();
  public static Config instance()
  {
    return INSTANCE;
  }

  private boolean _removeNullConstraintValues;

  /**
   * If true, recursively removes all {@code null} values from the <i>variables</i> component
   * of the GraphQL request payload, otherwise {@code null} are retained. Note the treatment
   * of {@code null} variable values is not well established by the GraphQL specification. It
   * states that:
   * <p/>
   * <i>null <b>may be</b> interpreted differently</i>
   * <p/>
   * while also stating:<p/>
   * <i>
   *   The same two methods of representing the lack of a value are possible via variables by
   *   either providing the a variable value as null and not providing a variable value at all.
   * </i>
   * <p/>
   * See https://spec.graphql.org/October2016/#sec-Null-Value.
   * <p/>
   * Note, the default setting is {@code false}, which does <i>not</i> remove {@code null} variable/input values.
   */
  public boolean isRemoveNullConstraintValues()
  {
    return _removeNullConstraintValues;
  }
  public void setRemoveNullConstraintValues( boolean value )
  {
    _removeNullConstraintValues = value;
  }

  private Config() {}
}
