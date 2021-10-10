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

package manifold.internal.javac;

import javax.tools.JavaFileManager;
import java.nio.file.Path;

public interface PreJava17JavacFileManagerMethod
{
  // This method exists so that a bridge method will be generated for the Iterable return type for Java 9 - 16.
  // Since we compile with Java 8 and this method does not exist in Java 8, we have to trick the compiler into
  // creating the bridge method in this way.
  Iterable<? extends Path> getLocationAsPaths( JavaFileManager.Location location );
}
