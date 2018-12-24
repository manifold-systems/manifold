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

package manifold.api.fs.physical.fast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import manifold.api.fs.ResourcePath;
import manifold.api.fs.physical.IFileMetadata;
import manifold.api.fs.physical.IPhysicalFileSystem;

public class FastPhysicalFileSystem implements IPhysicalFileSystem
{
  @Override
  public List<? extends IFileMetadata> listFiles( ResourcePath directoryPath )
  {
    File file = toJavaFile( directoryPath );
    File[] files = file.listFiles();
    List<FastFileMetadata> fileInfos = new ArrayList<>();
    if( files != null )
    {
      for( File f : files )
      {
        fileInfos.add( new FastFileMetadata( f ) );
      }
    }

    return fileInfos;
  }

  @Override
  public IFileMetadata getFileMetadata( ResourcePath filePath )
  {
    return new FastFileMetadata( toJavaFile( filePath ) );
  }

  @Override
  public boolean exists( ResourcePath filePath )
  {
    return toJavaFile( filePath ).exists();
  }

  @Override
  public boolean delete( ResourcePath filePath )
  {
    return toJavaFile( filePath ).delete();
  }

  @Override
  public boolean mkdir( ResourcePath dirPath )
  {
    return toJavaFile( dirPath ).mkdir();
  }

  @Override
  public void clearDirectoryCaches( ResourcePath dirPath )
  {
    // Do nothing
  }

  @Override
  public void clearAllCaches()
  {
    // Do nothing
  }

  private File toJavaFile( ResourcePath directoryPath )
  {
    return new File( directoryPath.getFileSystemPathString() );
  }
}
