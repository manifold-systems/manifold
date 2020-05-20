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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import manifold.api.host.IModule;
import manifold.rt.api.util.ManStringUtil;

/**
 * Utility class to scan for certain manifest headers through the classpath.
 */
public final class Extensions
{
  public static final String CONTAINS_SOURCES = "Contains-Sources";

  public static List<String> getExtensions( IDirectory dir, String headerName )
  {
    List<String> extensions = new ArrayList<String>();
    getExtensions( extensions, dir, headerName );
    return extensions;
  }

  public static boolean containsManifest( IDirectory dir )
  {
    IFile manifestFile = dir.file( "META-INF/MANIFEST.MF" );
    return manifestFile != null && manifestFile.exists();
  }

  public static void getExtensions( Collection<String> result, IDirectory dir, String headerName )
  {
    IFile manifestFile = dir.file( "META-INF/MANIFEST.MF" );
    if( manifestFile == null || !manifestFile.exists() )
    {
      return;
    }
    InputStream in = null;
    try
    {
      in = manifestFile.openInputStream();
      Manifest manifest = new Manifest( in );
      scanManifest( result, manifest, headerName );
    }
    catch( Exception e )
    {
      // FIXME: For some reason, WebSphere changes JARs in WEB-INF/lib, breaking signatures. So ignore errors.
      ResourcePath path = dir.getPath();
      String str = path != null ? path.getFileSystemPathString() : dir.toString();
      System.err.println( "Cannot read manifest from jar " + str + ", ignoring" );
    }
    finally
    {
      if( in != null )
      {
        try
        {
          in.close();
        }
        catch( IOException e )
        {
          // Ignore
        }
      }
    }
  }

  private static void scanManifest( Collection<String> result, Manifest manifest, String headerName )
  {
    Attributes mainAttributes = manifest.getMainAttributes();
    String valueList = mainAttributes.getValue( headerName );
    if( valueList != null )
    {
      for( String val : valueList.split( "," ) )
      {
        result.add( ManStringUtil.strip( val ) );
      }
    }
  }

  public static List<IDirectory> getJarsWithSources( IModule module )
  {
    List<IDirectory> jars = new ArrayList<IDirectory>();
    for( IDirectory root : module.getJavaClassPath() )
    {
      List<String> extensions = new ArrayList<String>();
      Extensions.getExtensions( extensions, root, CONTAINS_SOURCES );
      if( !extensions.isEmpty() )
      {
        jars.add( root );
      }
    }
    return jars;
  }

  private Extensions()
  {
    // No instances.
  }

}
