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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import manifold.api.gen.SrcRawExpression;
import manifold.api.host.IManifoldHost;
import manifold.rt.api.util.ManIdentifierUtil;
import manifold.api.util.cache.FqnCache;
import manifold.util.concurrent.LocklessLazyVar;

/**
 * Makes available the standard system properties, which should be available on all JVMs
 */
public class SystemProperties
{
  private static final String FQN = "gw.lang.SystemProperties";

  public static Map<String, LocklessLazyVar<Model>> make( IManifoldHost host )
  {
    Map<String, LocklessLazyVar<Model>> systemProps = new HashMap<>( 2 );
    systemProps.put( FQN,
                     LocklessLazyVar.make(
                       () ->
                       {
                         FqnCache<SrcRawExpression> cache = new FqnCache<>( FQN, true, ManIdentifierUtil::makeIdentifier );
                         _keys.forEach( key -> cache.add( key,
                             new SrcRawExpression( String.class , System.getProperty( key ) ) ) );
                         return new Model( host, FQN, cache );
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
