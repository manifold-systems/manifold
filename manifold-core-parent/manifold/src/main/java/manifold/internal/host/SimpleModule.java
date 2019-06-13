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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.cache.PathCache;
import manifold.api.host.Dependency;
import manifold.api.host.IManifoldHost;
import manifold.api.host.IModule;
import manifold.api.type.ContributorKind;
import manifold.api.type.ITypeManifold;
import manifold.api.type.TypeName;
import manifold.internal.javac.GeneratedJavaStubFileObject;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.internal.javac.SourceSupplier;
import manifold.util.JavacDiagnostic;
import manifold.util.concurrent.LocklessLazyVar;


import static manifold.api.type.ContributorKind.Partial;
import static manifold.api.type.ContributorKind.Primary;

/**
 */
@SuppressWarnings("WeakerAccess")
public abstract class SimpleModule implements IModule
{
  private IManifoldHost _host;
  private List<IDirectory> _classpath;
  private List<IDirectory> _sourcePath;
  private List<IDirectory> _outputPath;
  private SortedSet<ITypeManifold> _typeManifolds;
  private LocklessLazyVar<PathCache> _pathCache;

  public SimpleModule( IManifoldHost host, List<IDirectory> classpath, List<IDirectory> sourcePath, List<IDirectory> outputPath )
  {
    _host = host;

    verifyPaths( classpath, "classpath" );
    _classpath = classpath;

    verifyPaths( sourcePath, "source path" );
    _sourcePath = sourcePath;

    verifyPaths( outputPath, "output path" );
    _outputPath = outputPath;

    _pathCache = LocklessLazyVar.make( this::makePathCache );
  }

  private void verifyPaths( List<IDirectory> paths, String pathType )
  {
    if( paths == null )
    {
      return;
    }

    for( Iterator<IDirectory> iter = paths.iterator(); iter.hasNext(); )
    {
      IDirectory dir = iter.next();
      if( dir == null )
      {
        System.err.println( "Null path found in " + pathType );
      }
      else if( !dir.exists() )
      {
        // Warn of the nonexistent path
        System.err.println( "Warning: " + dir + " does not exist in " + pathType );
        // Remove it, otherwise in Java 9+ checks are made to verify the paths exist e.g., module locations
        iter.remove();
      }
    }
  }

  @Override
  public IManifoldHost getHost()
  {
    return _host;
  }

  @Override
  public List<IDirectory> getSourcePath()
  {
    return _sourcePath;
  }

  @Override
  public List<IDirectory> getJavaClassPath()
  {
    return _classpath;
  }

  @SuppressWarnings("unused")
  protected void setJavaClassPath( List<IDirectory> cp )
  {
    _classpath = cp;
  }

  @Override
  public List<IDirectory> getOutputPath()
  {
    return _outputPath;
  }

  @Override
  public IDirectory[] getExcludedPath()
  {
    return new IDirectory[0];
  }

  @Override
  public List<IDirectory> getCollectiveSourcePath()
  {
    return getSourcePath();
  }

  @Override
  public List<IDirectory> getCollectiveJavaClassPath()
  {
    return getJavaClassPath();
  }

  @Override
  public List<Dependency> getDependencies()
  {
    return Collections.emptyList();
  }

  public PathCache getPathCache()
  {
    return _pathCache.get();
  }

  public Set<ITypeManifold> getTypeManifolds()
  {
    return _typeManifolds;
  }

  public JavaFileObject produceFile( String fqn, JavaFileManager.Location location, DiagnosticListener<JavaFileObject> errorHandler )
  {
    //noinspection unchecked
    Set<ITypeManifold> sps = findTypeManifoldsFor( fqn );
    return sps.isEmpty() ? null : new GeneratedJavaStubFileObject( fqn, new SourceSupplier( fqn, sps, () -> compoundProduce( location, sps, fqn, errorHandler ) ) );
  }

  private String compoundProduce( JavaFileManager.Location location, Set<ITypeManifold> sps, String fqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    ITypeManifold found = null;
    String result = "";
    for( ITypeManifold sp: sps )
    {
      if( sp.getContributorKind() == Primary ||
          sp.getContributorKind() == Partial )
      {
        if( found != null && (found.getContributorKind() == Primary || sp.getContributorKind() == Primary) )
        {
          //## todo: use location to select more specifically (in Java 9+ with the location's module)
          List<IFile> files = sp.findFilesForType( fqn );
          JavaFileObject file = new SourceJavaFileObject( files.get( 0 ).toURI() );
          errorHandler.report( new JavacDiagnostic( file, Diagnostic.Kind.ERROR, 0, 1, 1,
            "The type, " + fqn + ", has conflicting type manifolds:\n" +
            "'" + found.getClass().getName() + "' and '" + sp.getClass().getName() + "'.\n" +
            "Either two or more resource files have the same base name or the project depends on two or more type manifolds that target the same resource type.\n" +
            "If the former, consider renaming one or more of the resource files.\n" +
            "If the latter, you must remove one or more of the type manifold libraries." ) );
        }
        else
        {
          found = sp;
          result = sp.contribute( location, fqn, result, errorHandler );
        }
      }
    }
    for( ITypeManifold sp: sps )
    {
      if( sp.getContributorKind() == ContributorKind.Supplemental )
      {
        result = sp.contribute( location, fqn, result, errorHandler );
      }
    }

    return result;
  }

  public void initializeTypeManifolds()
  {
    if( _typeManifolds != null )
    {
      return;
    }

    synchronized( this )
    {
      if( _typeManifolds != null )
      {
        return;
      }

      _typeManifolds = loadTypeManifolds();
      _typeManifolds.forEach( tm -> tm.init( this ) );
    }
  }

  public Set<TypeName> getChildrenOfNamespace( String packageName )
  {
    Set<TypeName> children = new HashSet<>();
    for( ITypeManifold sp: getTypeManifolds() )
    {
      Collection<TypeName> typeNames = sp.getTypeNames( packageName );
      children.addAll( typeNames );
    }
    return children;
  }

  private PathCache makePathCache()
  {
    return new PathCache( this, this::makeModuleSourcePath, () -> {} );
  }

  private List<IDirectory> makeModuleSourcePath()
  {
    return getSourcePath().stream()
      .filter( dir -> Arrays.stream( getExcludedPath() )
        .noneMatch( excludeDir -> excludeDir.equals( dir ) ) )
      .collect( Collectors.toList() );
  }
}
