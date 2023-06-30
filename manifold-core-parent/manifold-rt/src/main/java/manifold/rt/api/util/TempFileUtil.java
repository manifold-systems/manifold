/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.rt.api.util;

import java.io.File;

public class TempFileUtil
{
  public static File makeTempFile( String path )
  {
    return makeTempFile( path, true );
  }
  public static File makeTempFile( String path, boolean deleteOnExit )
  {
    String tempDir = System.getProperty( "java.io.tmpdir" );
    File tempFile = new File( tempDir, path );
    //noinspection ResultOfMethodCallIgnored
    tempFile.getParentFile().mkdirs();
    if( deleteOnExit )
    {
      tempFile.deleteOnExit();
    }
    return tempFile;
  }
}
