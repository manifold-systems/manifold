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
import manifold.api.host.IModuleComponent;
import manifold.api.type.JavaTypeManifold;
import manifold.util.cache.FqnCache;
import manifold.util.cache.FqnCacheNode;
import manifold.util.concurrent.LocklessLazyVar;

public class PropertiesTypeManifold extends JavaTypeManifold<Model>
{
  private static final Set<String> FILE_EXTENSIONS = Collections.singleton( "properties" );

  public void init( IModuleComponent typeLoader )
  {
    init( typeLoader, Model::new, "editor.plugin.typeloader.properties.PropertiesTypeFactory" );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return FILE_EXTENSIONS.contains( fileExtension.toLowerCase() );
  }

  @Override
  protected Map<String, LocklessLazyVar<Model>> getPeripheralTypes()
  {
    return SystemProperties.make();
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
