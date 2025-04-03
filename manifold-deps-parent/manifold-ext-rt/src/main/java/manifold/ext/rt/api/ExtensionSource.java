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

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Use this annotation to indicate a class is a class which methods can be used
 * as Manifold Extension methods. Methods don't need to be annotated with
 * {@link Extension}, nor need the method parameters be annotated with {@link This}
 * or {@link ThisClass}.
 */
@Retention(RetentionPolicy.CLASS)
@Repeatable(value = ExtensionSources.class)
public @interface ExtensionSource
{
    /**
     * The source class, which contains methods that can be used as Manifold Extension methods
     */
    String source = "source";
    Class source();

    /**
     * If overrideExistingMethods is true, existing methods can be overridden by extension methods
     */
    String overrideExistingMethods = "overrideExistingMethods";
    boolean overrideExistingMethods() default false;

    /**
     * When {@link #methods} are configured, the types defines if the methods are included or excluded
     * to be added as extension methods
     */
    String type = "type";
    ExtensionMethodType type() default ExtensionMethodType.EXCLUDE;

    /**
     * Definitions of methods to be added or excluded, depending on the {@link #type}
     */
    String methods = "methods";
    MethodSignature[] methods() default { };

}
