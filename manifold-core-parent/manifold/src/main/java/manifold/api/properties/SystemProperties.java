package manifold.api.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import manifold.util.JsonUtil;
import manifold.util.cache.FqnCache;
import manifold.util.concurrent.LocklessLazyVar;

/**
 * Makes available the standard system properties, which should be available on all JVMs
 */
public class SystemProperties
{
  private static final String FQN = "gw.lang.SystemProperties";

  public static Map<String, LocklessLazyVar<Model>> make()
  {
    Map<String, LocklessLazyVar<Model>> systemProps = new HashMap<>( 2 );
    systemProps.put( FQN,
                     LocklessLazyVar.make(
                       () ->
                       {
                         FqnCache<String> cache = new FqnCache<>( FQN, true, JsonUtil::makeIdentifier );
                         _keys.forEach( key -> cache.add( key, System.getProperty( key ) ) );
                         return new Model( FQN, cache );
                       } ) );
    return systemProps;
  }

  private static final Set<String> _keys = Collections.unmodifiableSet( new TreeSet<>( Arrays.asList(
    "java.version",
    "java.vendor",
    "java.vendor.url",
    "java.home",
    "java.vm.specification.version",
    "java.vm.specification.vendor",
    "java.vm.specification.name",
    "java.vm.version",
    "java.vm.vendor",
    "java.vm.name",
    "java.specification.version",
    "java.specification.vendor",
    "java.specification.name",
    "java.class.version",
    "java.class.path",
    "java.library.path",
    "java.io.tmpdir",
    "java.compiler",
    "java.ext.dirs",
    "os.name",
    "os.arch",
    "os.version",
    "file.separator",
    "path.separator",
    "line.separator",
    "user.name",
    "user.home",
    "user.dir"
  ) ) );
}
