package manifold.api.fs.cache;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import manifold.api.fs.IDirectory;
import manifold.api.host.IModule;
import manifold.util.concurrent.Cache;
import manifold.util.concurrent.LocklessLazyVar;

/**
 */
public class ModulePathCache
{
  private static final ModulePathCache INSTANCE = new ModulePathCache();

  private final Cache<IModule, LocklessLazyVar<PathCache>> _cacheByModule =
    new Cache<>( "Path Cache", 1000, module ->
      LocklessLazyVar.make( () -> makePathCache( module ) ) );

  public static ModulePathCache instance()
  {
    return INSTANCE;
  }

  private ModulePathCache()
  {
  }

  public PathCache get( IModule module )
  {
    return _cacheByModule.get( module ).get();
  }

  private PathCache makePathCache( IModule module )
  {
    return new PathCache( module,
      () -> makeModuleSourcePath( module ),
      () -> _cacheByModule.get( module ).clear() );
  }

  private List<IDirectory> makeModuleSourcePath( IModule module )
  {
    return module.getSourcePath().stream()
      .filter( dir -> Arrays.stream( module.getExcludedPath() )
        .noneMatch( excludeDir -> excludeDir.equals( dir ) ) )
      .collect( Collectors.toList() );
  }
}
