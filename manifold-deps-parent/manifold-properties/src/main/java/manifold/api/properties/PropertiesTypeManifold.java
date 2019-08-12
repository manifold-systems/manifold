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

package manifold.api.properties;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;
import manifold.api.util.cache.FqnCache;
import manifold.api.util.cache.FqnCacheNode;
import manifold.util.concurrent.LocklessLazyVar;

public class PropertiesTypeManifold extends JavaTypeManifold<Model>
{
  private static final Set<String> FILE_EXTENSIONS = Collections.singleton( "properties" );

  public void init( IModule module )
  {
    init( module, (fqn,files) -> new Model( getModule().getHost(), fqn, files ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  protected Map<String, LocklessLazyVar<Model>> getPeripheralTypes()
  {
    return SystemProperties.make( getModule().getHost() );
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    Model model = getModel( topLevel );
    FqnCache<String> cache = model == null ? null : model.getCache();
    if( cache == null )
    {
      return false;
    }
    FqnCacheNode<String> node = cache.getNode( relativeInner );
    return node != null && !node.isLeaf();
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    List<IFile> files = findFilesForType( topLevelFqn );
    SrcClass srcClass = new PropertiesCodeGen( model.getCache(), files.isEmpty() ? null : files.get( 0 ), topLevelFqn ).make();
    StringBuilder sb = srcClass.render( new StringBuilder(), 0 );
    return sb.toString();
  }
}
