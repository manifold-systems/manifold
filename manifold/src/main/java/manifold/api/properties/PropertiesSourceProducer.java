/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.api.properties;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.api.sourceprod.JavaSourceProducer;
import manifold.api.host.ITypeLoader;
import manifold.util.cache.FqnCache;
import manifold.util.cache.FqnCacheNode;
import manifold.util.concurrent.LocklessLazyVar;

public class PropertiesSourceProducer extends JavaSourceProducer<Model>
{
  private static final Set<String> FILE_EXTENSION = Collections.singleton( "properties" );

  public void init( ITypeLoader typeLoader )
  {
    init( typeLoader, FILE_EXTENSION, Model::new,
          "editor.plugin.typeloader.properties.PropertiesTypeFactory" );
  }

  @Override
  protected Map<String, LocklessLazyVar<Model>> getPeripheralTypes()
  {
    return SystemProperties.make();
  }

  @Override
  protected boolean isInnerType( String topLevel, String relativeInner )
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
  protected String produce( String topLevelFqn, String existing, Model model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    List<IFile> files = findFilesForType( topLevelFqn );
    SrcClass srcClass = new PropertiesCodeGen( model.getCache(), files.isEmpty() ? null : files.get( 0 ), topLevelFqn ).make();
    StringBuilder sb = srcClass.render( new StringBuilder(), 0 );
    return sb.toString();
  }
}
