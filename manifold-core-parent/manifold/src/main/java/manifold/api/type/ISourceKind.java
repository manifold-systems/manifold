/*
 * Copyright (c) 2018 - Manifold Systems LLC
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

/**
 * Indicates the source language a {@link ITypeManifold} uses in projected types.
 * <p/>
 * The {@link #Java}, {@link #JavaScript}, and {@link #None} constants are all
 * handled directly when using Manifold with Java. Support for other JVM
 * languages must be provided via third parties implementing {@link manifold.api.host.IManifoldHost}.
 */
public interface ISourceKind
{
  /** Java source */
  ISourceKind Java = new ISourceKind() {};

  /** JavaScript source */
  ISourceKind JavaScript = new ISourceKind() {};

  /** The {@ITypeManifold} does not contribute source */
  ISourceKind None = new ISourceKind() {};
}
