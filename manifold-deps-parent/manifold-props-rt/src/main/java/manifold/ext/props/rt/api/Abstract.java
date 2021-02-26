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

/**
 * <h2>For internal use only.</h2>
 * <p/>
 * Use {@code abstract} modifier directly on properties instead of this to indicate corresponding accessor <i>methods</i>
 * are abstract. Manifold rewrites the property fields's modifiers during compilation and replace {@code abstract} with
 * {@code @Abstract}.
 */
public @interface Abstract
{
}
