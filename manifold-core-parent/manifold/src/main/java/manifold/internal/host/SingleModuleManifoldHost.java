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

import java.util.List;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFileFragment;
import manifold.api.fs.IFileSystem;
import manifold.api.fs.def.FileSystemImpl;
import manifold.api.host.IModule;
import manifold.api.host.ITypeSystemListener;
import manifold.internal.javac.JavaParser;
import manifold.util.BytecodeOptions;
import manifold.util.concurrent.LocklessLazyVar;

/**
 */
public abstract class SingleModuleManifoldHost extends AbstractManifoldHost
{
  private DefaultSingleModule _module;
  private TypeRefreshListener _typeRefreshHandler = new TypeRefreshListener( this );
  private ThreadLocal<JavaParser> _javaParser = new ThreadLocal<>();
  private LocklessLazyVar<IFileSystem> _fileSystem = LocklessLazyVar.make(
    () ->
    {
      //noinspection ConstantConditions
      if( BytecodeOptions.JDWP_ENABLED.get() )
      {
        return new FileSystemImpl( this, IFileSystem.CachingMode.NO_CACHING );
      }
      return new FileSystemImpl( this, IFileSystem.CachingMode.FULL_CACHING );
    }
  );

  public IFileSystem getFileSystem()
  {
    return _fileSystem.get();
  }

  @Override
  public JavaParser getJavaParser()
  {
    if( _javaParser.get() == null )
    {
      _javaParser.set( new JavaParser( this ) );
    }
    return _javaParser.get();
  }

  public IModule getSingleModule()
  {
    return _module;
  }

  void createSingleModule( List<IDirectory> classpath, List<IDirectory> sourcePath, List<IDirectory> outputPath )
  {
    if( _module != null )
    {
      throw new IllegalStateException();
    }

    // Must assign _module BEFORE initializeTypeManifolds() to prevent double bootstrapping
    _module = new DefaultSingleModule( this, classpath, sourcePath, outputPath );
    _module.initializeTypeManifolds();
  }

  public void addTypeSystemListenerAsWeakRef( Object ctx, ITypeSystemListener listener )
  {
    // Full type system monitoring is only necessary for an environment like an IDE where types change.
    // Compilation and runtime environments should only care about *creation* events e.g., from a type embedded via @Type.
    _typeRefreshHandler.addTypeSystemListenerAsWeakRef( listener );
  }

  @Override
  public void createdType( IFileFragment file, String[] types )
  {
    _typeRefreshHandler.created( file, types );
  }
}
