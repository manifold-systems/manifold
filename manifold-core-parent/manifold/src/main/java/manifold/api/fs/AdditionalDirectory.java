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

package manifold.api.fs;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class AdditionalDirectory extends DelegateDirectory
{
  public AdditionalDirectory( IFileSystem fs, IDirectory delegate )
  {
    super( fs, delegate );
  }

  @Override
  public List<? extends IDirectory> listDirs()
  {
    List<IDirectory> result = new ArrayList<IDirectory>();
    for( IDirectory dir : getDelegate().listDirs() )
    {
      result.add( new AdditionalDirectory( getFileSystem(), dir ) );
    }
    return result;
  }

  @Override
  public boolean isAdditional()
  {
    return true;
  }
}
