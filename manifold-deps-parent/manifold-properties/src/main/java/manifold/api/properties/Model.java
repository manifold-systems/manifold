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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcRawExpression;
import manifold.api.host.IManifoldHost;
import manifold.api.type.AbstractSingleFileModel;
import manifold.rt.api.util.ManIdentifierUtil;
import manifold.api.util.cache.FqnCache;

/**
 */
class Model extends AbstractSingleFileModel
{
  private FqnCache<SrcRawExpression> _cache;

  public Model( IManifoldHost host, String fqn, Set<IFile> files )
  {
    super( host, fqn, files );
    buildCache( fqn, getFile() );
  }

  public Model( IManifoldHost host, String fqn, FqnCache<SrcRawExpression> cache )
  {
    super( host, fqn, Collections.emptySet() );
    _cache = cache;
  }

  public FqnCache<SrcRawExpression> getCache()
  {
    return _cache;
  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    buildCache( getFqn(), file );
  }

  private void buildCache( String fqn, IFile file )
  {
    try( InputStream propertiesStream = file.openInputStream() )
    {
      Properties properties = new Properties();
      properties.load( propertiesStream );

      FqnCache<SrcRawExpression> cache = new FqnCache<>( fqn, true, ManIdentifierUtil::makeIdentifier );

      for( String key : properties.stringPropertyNames() )
      {
        cache.add( key, createExpression ( key, properties.getProperty( key ) ) );
      }
      _cache = cache;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  protected SrcRawExpression createExpression( String key, String value ){
    return  new SrcRawExpression( String.class, value);
  }
}
