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

import manifold.api.fs.IFile;
import manifold.api.util.cache.FqnCache;

import java.util.function.Function;

/**
 * Note, implementers must chain/forward to the default provider to fall back on existing behavior.
 */
public interface DbLocationProvider
{
  String PROVIDED = "#";
  Object UNHANDLED = new Object() {};

  enum Mode {CompileTime, DesignTime, Runtime, Unknown}

  Object getLocation( Function<String, FqnCache<IFile>> resByExt, Mode mode, String tag, String... args );
}
