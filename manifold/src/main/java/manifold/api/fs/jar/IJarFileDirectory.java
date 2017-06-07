/*
 * Copyright 2014 Guidewire Software, Inc.
 */

package manifold.api.fs.jar;

import manifold.api.fs.IDirectory;

public interface IJarFileDirectory extends IDirectory
{
  JarEntryDirectoryImpl getOrCreateDirectory( String relativeName );
  JarEntryFileImpl getOrCreateFile( String relativeName );
}
