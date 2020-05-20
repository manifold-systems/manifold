/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.yaml;

import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.host.IManifoldHost;
import manifold.api.json.JsonModel;
import manifold.api.type.ResourceFileTypeManifold;
import manifold.api.yaml.rt.Yaml;

/**
 */
class YamlModel extends JsonModel
{
  YamlModel( IManifoldHost host, String fqn, Set<IFile> files )
  {
    super( host, fqn, files );
  }

  @Override
  protected Object load()
  {
    return Yaml.fromYaml( ResourceFileTypeManifold.getContent( getFile() ), false, true );
  }
}