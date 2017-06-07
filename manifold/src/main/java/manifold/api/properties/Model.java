package manifold.api.properties;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import manifold.api.fs.IFile;
import manifold.api.json.Json;
import manifold.api.sourceprod.ResourceFileSourceProducer;
import manifold.util.cache.FqnCache;

/**
 */
class Model implements ResourceFileSourceProducer.IModel
{
  private String _fqn;
  private IFile _file;
  private FqnCache<String> _cache;

  public Model( String fqn, IFile file )
  {
    _fqn = fqn;
    _file = file;

    buildCache( fqn, file );
  }

  public Model( String fqn, FqnCache<String> cache )
  {
    _fqn = fqn;
    _file = null;
    _cache = cache;
  }

  @Override
  public String getFqn()
  {
    return _fqn;
  }

  @Override
  public List<IFile> getFiles()
  {
    return Collections.singletonList( _file );
  }

  public FqnCache<String> getCache()
  {
    return _cache;
  }

  private void buildCache( String fqn, IFile file )
  {
    try( InputStream propertiesStream = file.openInputStream() )
    {
      Properties properties = new Properties();
      properties.load( propertiesStream );

      FqnCache<String> cache = new FqnCache<>( fqn, true, Json::makeIdentifier );

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
