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

package manifold.internal.javac;

import java.io.IOException;
import java.util.Set;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

public class WrappedMJFM extends ForwardingJavaFileManager
{
  private final JavaFileManager _plainFm;

  /**
   * Creates a new instance of ForwardingJavaFileManager.
   *
   * @param fileManager delegate to this file manager
   */
  protected WrappedMJFM( JavaFileManager fileManager, ManifoldJavaFileManager mfm )
  {
    super( mfm );
    _plainFm = fileManager;
  }

  @Override
  public Iterable<JavaFileObject> list( Location location, String packageName, Set set, boolean recurse ) throws IOException
  {
    Iterable list = _plainFm.list( location, packageName, set, recurse );
    if( !list.iterator().hasNext() )
    {
      list = super.list( location, packageName, set, recurse );
    }
    return list;
  }
}
