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

package manifold.internal.host;

import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;
import manifold.api.service.BaseService;

/**
 */
public abstract class AbstractManifoldHost extends BaseService implements IManifoldHost
{
  //## todo: move this to RuntimeManifoldHost after factoring ExtensionManifold#isInnerToJavaClass()
  public ClassLoader getActualClassLoader()
  {
//    if( JavacPlugin.instance() == null )
//    {
//      // runtime
//      return Thread.currentThread().getContextClassLoader();
//    }
//    // compile-time
    return RuntimeManifoldHost.class.getClassLoader();
  }

  @Override
  public ClassLoader getClassLoaderForFile( IFile file )
  {
    // Only module-based IDE hosts handle this
    return null;
  }

  public boolean isPathIgnored( String path )
  {
    return false;
  }
}
