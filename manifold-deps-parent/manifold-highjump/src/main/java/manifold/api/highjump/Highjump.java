/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.highjump;

import manifold.api.type.ITypeManifold;
import manifold.internal.host.RuntimeManifoldHost;

public class Highjump
{
  private static final Highjump INSTANCE = new Highjump();
  private final HighjumpTypeManifold _highjump;

  public static Highjump instance()
  {
    return INSTANCE;
  }

  private Highjump()
  {
    for( ITypeManifold tm: RuntimeManifoldHost.get().getSingleModule().getTypeManifolds() )
    {
      if( tm instanceof HighjumpTypeManifold )
      {
        _highjump = (HighjumpTypeManifold)tm;
        return;
      }
    }
    throw new IllegalStateException( "Type manifold for '" + HighjumpTypeManifold.class.getTypeName() + "' not found." );
  }

  public Object evaluate( String expr )
  {
    return evaluate( Options.builder( expr ).build() );
  }
  public Object evaluate( Options options )
  {
    Object result = _highjump.evaluate( options );
    return result;
  }
}
