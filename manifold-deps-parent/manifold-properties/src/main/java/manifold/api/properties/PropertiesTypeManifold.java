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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcExpression;
import manifold.api.gen.SrcRawExpression;
import manifold.api.host.IModule;
import manifold.api.properties.ResourceBundleFiles.Type;
import manifold.api.type.JavaTypeManifold;
import manifold.api.util.cache.FqnCache;
import manifold.api.util.cache.FqnCacheNode;
import manifold.util.concurrent.LocklessLazyVar;

public class PropertiesTypeManifold extends JavaTypeManifold<Model>
{
  private static final Set<String> FILE_EXTENSIONS = Collections.singleton( "properties" );

  private final ResourceBundleFiles resourceBundleFiles = new ResourceBundleFiles();

  public void init( IModule module )
  {
    init( module, this::createModel);
  }

  private Model createModel(String _fqn, Set<IFile> files){
    return new Model(getModule().getHost(), _fqn, files ) {
      protected SrcExpression createExpression( String key, String value ){
        return resourceBundleFiles.getType(_fqn) == Type.DEFAULT
            ? new SrcRawExpression( "_resourceBundle.getString(\"" + key + "\")")
            : super.createExpression(key, value);
      }
    };
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  public boolean handlesFile( IFile file )
  {
    if(handlesFileExtension( file.getExtension() ) ) {
      getFqnForFile(file).ifPresent(fqn -> resourceBundleFiles.addFile(file, fqn));
      return true;
    }
    return false;
  }

  @Override
  protected Map<String, LocklessLazyVar<Model>> getPeripheralTypes()
  {
    resourceBundleFiles.removeSingleFileResourceBundles();
    return SystemProperties.make( getModule().getHost()  );
  }

  private Optional<String> getFqnForFile( IFile file ) {
    return Optional.ofNullable(getModule().getPathCache().getFqnForFile( file )).map(fqns -> fqns.iterator().next());
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    Model model = getModel( topLevel );
    FqnCache<SrcExpression> cache = model == null ? null : model.getCache();
    if( cache == null )
    {
      return false;
    }
    FqnCacheNode<SrcExpression> node = cache.getNode( relativeInner );
    return node != null && !node.isLeaf();
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, boolean genStubs,
      String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    List<IFile> files = findFilesForType( topLevelFqn );
    Type type = resourceBundleFiles.getType(topLevelFqn);
    switch(type){
      case DEFAULT:
        return new ResourceBundelCodeGen( model.getCache(), files.isEmpty() ? null : files.get(0), topLevelFqn)
            .make(getModule(), location, errorHandler)
            .render( new StringBuilder(), 0 ).toString();
      case SPECIFIC:
        return "";
      case NONE:
        return new PropertiesCodeGen( model.getCache(), files.isEmpty() ? null : files.get(0), topLevelFqn)
            .make(getModule(), location, errorHandler)
            .render( new StringBuilder(), 0 ).toString();
      default:
        throw new IllegalStateException();
    }
  }
}
