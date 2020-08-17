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

package manifold.internal.javac;

import javax.tools.JavaFileManager;

public class ManPatchModuleLocation implements JavaFileManager.Location
{
  private final String _moduleName;
  private final JavaFileManager.Location _locationForModule;

  /**
   * @param moduleName The name of the (potentially) patched module.
   * @param locationForModule The location of the patched module (if the module is patched via --patch-module on the
   *                          command line), otherwise null. Note this location is to be used with
   *                          ManifoldJavaFileManager#list() to preserve patched java classes.
   */
  ManPatchModuleLocation( String moduleName, JavaFileManager.Location locationForModule )
  {
    _moduleName = moduleName;
    _locationForModule = locationForModule;
  }

  @Override
  public String getName()
  {
    return _moduleName;
  }

  public JavaFileManager.Location getLocationForModule()
  {
    return _locationForModule == null ? this : _locationForModule;
  }

  @Override
  public boolean isOutputLocation()
  {
    return false;
  }
}
