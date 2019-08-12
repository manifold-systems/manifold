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

package manifold.internal.host;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFileSystem;
import manifold.api.host.IManifoldHost;
import manifold.internal.javac.JavacPlugin;
import manifold.api.util.SourcePathUtil;

/**
 * {@link JavacManifoldHost} is exclusive to an instance of {@link JavacPlugin}. There can be multiple JavacTasks,
 * therefore multiple JavacPlugins, therefore multiple JavacManifoldHosts per process. Moreover, instances of
 * {@link JavacManifoldHost} can coexist with the {@link RuntimeManifoldHost} instance.  Likewise, instances of
 * {@code IjManifoldHost} can coexist with instanceof {@link JavacManifoldHost} and the {@link RuntimeManifoldHost}.
 * More generally, any two host instances must not share state that contributes to the {@link IManifoldHost} semantic
 * contract.
 */
public class JavacManifoldHost extends SingleModuleManifoldHost
{
  public void initialize( Set<String> sourcePath, List<String> classpath, List<String> outputPath )
  {
    List<String> cp = classpath.stream().filter( e -> !SourcePathUtil.excludeFromSourcePath( e ) ).collect( Collectors.toList() );
    Set<String> sp = sourcePath.stream().filter( e -> !SourcePathUtil.excludeFromSourcePath( e ) ).collect( Collectors.toSet() );

    int i = 0;
    for( String p: outputPath )
    {
      if( !cp.contains( p ) )
      {
        // ensure output path is in the classpath
        cp.add( i++, p );
      }
    }

    List<String> all = new ArrayList<>();
    for( String p: sp )
    {
      if( !all.contains( p ) )
      {
        all.add( p );
      }
    }
    for( String p: cp )
    {
      if( !all.contains( p ) )
      {
        all.add( p );
      }
    }
    initPaths( cp, all, outputPath );
  }

  private void initPaths( List<String> classpath, List<String> sourcePath, List<String> outputPath )
  {
    IFileSystem fs = getFileSystem();
    List<IDirectory> cp = classpath.stream().map( path -> fs.getIDirectory( new File( path ) ) ).collect( Collectors.toList() );
    List<IDirectory> sp = sourcePath.stream().map( path -> fs.getIDirectory( new File( path ) ) ).collect( Collectors.toList() );
    List<IDirectory> op = outputPath.stream().map( path -> fs.getIDirectory( new File( path ) ) ).collect( Collectors.toList() );
    createSingleModule( cp, sp, op );
  }

}
