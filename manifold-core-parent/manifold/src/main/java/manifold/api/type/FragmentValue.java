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

package manifold.api.type;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use this annotation in generated code to handle a file fragment embedded in a String literal:
 * <pre>
 *   // Sample code using embedded SQL
 *   var qb = "[>.sql<] SELECT name, age FROM contact WHERE age > #age</]>";
 *   var query = qb.builder(30).build();
 * </pre>
 * <pre>
 * // Generated from Manifold
 * &#064;FragmentValue(methodName = "runMe", type = Fragment_123456.Query.class)
 * public class Fragment_123456 {
 *   public static class Builder {...}
 *   public static Builder builder(int age) {...}
 *
 *   public static class Result {...}
 *
 *   ...
 *
 *   public Builder builder(int age) {
 *     return new Builder( age );
 *   }
 *
 *   public static Query runMe() {
 *     return new Fragment_123456();
 *   }
 * }
 * </pre>
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.SOURCE)
public @interface FragmentValue
{
  String methodName();
  String type();
}
