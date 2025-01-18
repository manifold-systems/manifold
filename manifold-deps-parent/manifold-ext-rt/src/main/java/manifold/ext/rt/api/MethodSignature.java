/*
 * Copyright (c) 2025 - Manifold Systems LLC
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

package manifold.ext.rt.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used to define method signatures, consisting of the method name, and it parameter types
 */
@Retention(RetentionPolicy.CLASS)
public @interface MethodSignature
{
    /** The name of the method */
    String name = "name";
    String name();

    /** the parameter types of the method */
    String paramTypes = "paramTypes";
    Class[] paramTypes();
}
