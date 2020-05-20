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

package manifold.js;

import java.net.MalformedURLException;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;
import manifold.api.host.IModule;
import manifold.api.type.AbstractSingleFileModel;

public class JavascriptModel extends AbstractSingleFileModel
{
  private final IModule _module;
  private String _url;

  JavascriptModel( IModule module, String fqn, Set<IFile> files )
  {
    super( module.getHost(), fqn, files );
    _module = module;
    assignUrl();
  }

  private void assignUrl()
  {
    try
    {
      _url = getFile().toURI().toURL().toString();
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }
  }

  public IModule getModule()
  {
    return _module;
  }

  public String getUrl()
  {
    return _url;
  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    assignUrl();
  }
}

