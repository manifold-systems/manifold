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

package manifold.api.fs;

import java.io.File;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import manifold.api.host.IManifoldHost;
import manifold.api.service.IService;

public interface IFileSystem extends IService
{
  IManifoldHost getHost();

  IDirectory getIDirectory( File dir );

  IFile getIFile( File file );

  void setCachingMode( CachingMode cachingMode );

  void clearAllCaches();

  IDirectory getIDirectory( URL url );

  IFile getIFile( URL url );

  Lock getLock();

//  IFile getFakeFile( URL url, IModule module );

  enum CachingMode
  {
    NO_CACHING,
    CHECK_TIMESTAMPS,
    FUZZY_TIMESTAMPS,
    FULL_CACHING
  }
}
