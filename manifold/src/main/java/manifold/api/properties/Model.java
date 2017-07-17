package manifold.api.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import manifold.api.fs.IFile;
import manifold.api.sourceprod.AbstractSingleFileModel;
import manifold.util.JsonUtil;
import manifold.util.cache.FqnCache;

/**
 */
class Model extends AbstractSingleFileModel
{
  private FqnCache<String> _cache;

  public Model( String fqn, Set<IFile> files )
  {
    super( fqn, files );
    buildCache( fqn, getFile() );
  }

  public Model( String fqn, FqnCache<String> cache )
  {
    super( fqn, Collections.emptySet() );
    _cache = cache;
  }

  public FqnCache<String> getCache()
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

      FqnCache<String> cache = new FqnCache<>( fqn, true, JsonUtil::makeIdentifier );

      for( String key : properties.stringPropertyNames() )
      {
        cache.add( key, properties.getProperty( key ) );
      }
      _cache = cache;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }
}
