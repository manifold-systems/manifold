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

package manifold.api.type;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.host.AbstractTypeSystemListener;
import manifold.api.host.IModule;
import manifold.api.host.RefreshKind;
import manifold.api.host.RefreshRequest;
import manifold.api.service.BaseService;
import manifold.util.ManClassUtil;
import manifold.util.StreamUtil;
import manifold.util.cache.FqnCache;
import manifold.util.concurrent.ConcurrentHashSet;
import manifold.util.concurrent.LocklessLazyVar;

/**
 * A base class for a type manifold that is based on a resource file type, typically discernible by the file extension.
 *
 * @param <M> The model you derive backing contributions of source code.
 */
public abstract class ResourceFileTypeManifold<M extends IModel> extends BaseService implements ITypeManifold
{
  private IModule _module;
  private LocklessLazyVar<FqnCache<LocklessLazyVar<M>>> _fqnToModel;
  private BiFunction<String, Set<IFile>, M> _modelMapper;
  @SuppressWarnings("all")
  private CacheClearer _cacheClearer;

  @Override
  public void init( IModule module )
  {
    _module = module;
  }

  /**
   * @param module  The module passed into the {@code ITypeManifold) implementation constructor
   * @param modelMapper A function to provide a model given a qualified name and resource file
   */
  protected void init( IModule module, BiFunction<String, Set<IFile>, M> modelMapper )
  {
    _module = module;
    _modelMapper = modelMapper;
    _fqnToModel = LocklessLazyVar.make( this::buildFqnToModelCache );
    getModule().getHost().addTypeSystemListenerAsWeakRef( getModule(), _cacheClearer = createCacheClearer() );
  }

  protected CacheClearer createCacheClearer()
  {
    return new CacheClearer();
  }

  private FqnCache<LocklessLazyVar<M>> buildFqnToModelCache()
  {
    FqnCache<LocklessLazyVar<M>> fqnToModel = new FqnCache<>();
    Map<String, Set<IFile>> primaryFqnToFiles = buildPrimaryFqnToFilesMap();

    for( Map.Entry<String, Set<IFile>> entry : primaryFqnToFiles.entrySet() )
    {
      String primaryFqn = entry.getKey();
      Set<IFile> files = entry.getValue();
      LocklessLazyVar<M> model = null;
      String primaryFqnNoMinus;

      // Map primary type to model
      if( primaryFqn.charAt( 0 ) != '-' )
      {
        model = LocklessLazyVar.make( () -> _modelMapper.apply( primaryFqn, files ) );
        fqnToModel.add( primaryFqn, model );
        primaryFqnNoMinus = primaryFqn;
      }
      else
      {
        primaryFqnNoMinus = primaryFqn.substring( 1 );
      }

      // Map additional types to same model
      for( IFile file : files )
      {
        for( String addFqn : getAdditionalTypes( primaryFqnNoMinus, file ) )
        {
          if( model == null )
          {
            model = LocklessLazyVar.make( () -> _modelMapper.apply( primaryFqnNoMinus, files ) );
          }
          fqnToModel.add( addFqn, model ); // use same model as base fqn
        }
      }
    }

    // Add peripheral (global) types having separate model scheme
    Map<String, LocklessLazyVar<M>> peripheralTypes = getPeripheralTypes();
    if( peripheralTypes != null )
    {
      for( Map.Entry<String, LocklessLazyVar<M>> entry: peripheralTypes.entrySet() )
      {
        if( fqnToModel.get( entry.getKey() ) == null )
        {
          fqnToModel.add( entry.getKey(), entry.getValue() );
        }
      }
    }
    
    return fqnToModel;
  }

  private Map<String, Set<IFile>> buildPrimaryFqnToFilesMap()
  {
    Map<String, Set<IFile>> primaryFqnToFiles = new HashMap<>();
    Map<String, FqnCache<IFile>> extensionCaches = getModule().getPathCache().getExtensionCaches();
    for( Map.Entry<String, FqnCache<IFile>> entry : extensionCaches.entrySet() )
    {
      String ext = entry.getKey();
      if( !handlesFileExtension( ext ) )
      {
        continue;
      }

      FqnCache<IFile> fileCache = entry.getValue();
      for( String fqn : fileCache.getFqns() )
      {
        IFile file = fileCache.get( fqn );
        if( file != null && handlesFile( file ) )
        {
          String primaryFqn = getTypeNameForFile( fqn, file );
          if( primaryFqn != null )
          {
            String pfqn = primaryFqn.isEmpty() ? '-' + fqn : primaryFqn;
            Set<IFile> files = primaryFqnToFiles.get( pfqn );
            if( files == null )
            {
              files = new ConcurrentHashSet<>();
            }
            if( !isDuplicate( file, files ) )
            {
              files.add( file );
              primaryFqnToFiles.put( pfqn, files );
            }
          }
        }
      }
    }
    return primaryFqnToFiles;
  }

  protected boolean isDuplicate( IFile file, Set<IFile> files )
  {
    Set<String> fqnForFile = getModule().getPathCache().getFqnForFile( file );
    for( IFile f : files )
    {
      Set<String> fqn = getModule().getPathCache().getFqnForFile( f );
      if( fqnForFile.equals( fqn ) )
      {
        return true;
      }
    }
    return false;
  }

  /**
   * A map of name-to-model peripheral to the main map of name-to-model,
   * possibly including types that are not file-based. Optional.
   */
  protected Map<String, LocklessLazyVar<M>> getPeripheralTypes()
  {
    return Collections.emptyMap();
  }

  /**
   * Provide the type name that corresponds with the resource file, if different from {@code defaultFqn}.
   *
   * @param defaultFqn The default name derived from the resource file name.
   * @param file The resource file corresponding with the type name.
   * @return A valid type name corresponding with the type or {@code null} if there is no primary type for {@code file}
   * but there may be additional types via {@link #getAdditionalTypes(String, IFile)}
   */
  public String getTypeNameForFile( String defaultFqn, IFile file )
  {
    return defaultFqn;
  }

  /**
   * Additional types derived from {@code file}.
   * These can be supporting classes, interfaces, extension classes, what have you.
   * In the case of extension classes, this type manifold must implement IExtensionProvider.
   *
   * @param fqnForFile The primary type this type manifold assigned to {@code file}
   * @param file The resource file from which types may be derived
   */
  protected Set<String> getAdditionalTypes( String fqnForFile, IFile file )
  {
    return Collections.emptySet();
  }

  /**
   * @param topLevelFqn   Qualified name of top-level type
   * @param relativeInner Top-level relative name of inner class
   *
   * @return true if relativeInner is an inner class of topLevel
   */
  public abstract boolean isInnerType( String topLevelFqn, String relativeInner );

  /**
   * Contribute source code for the specified type and model.
   *
   *
   * @param location (Experimental) The location of the use-site in the Java compiler.  Provides javac module context.  Optional and only relevant at compile-time when executed within a Javac compiler.
   * @param topLevelFqn The qualified name of the top-level type to contribute.
   * @param existing    The source produced from other manifolds so far; if not empty, this manifold must not be a {@link ContributorKind#Primary} contributor.
   * @param model       The model your manifold uses to generate the source.
   * @return The combined source code for the specified top-level type.
   */
  protected abstract String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, M model, DiagnosticListener<JavaFileObject> errorHandler );

  protected M getModel( String topLevel )
  {
    LocklessLazyVar<M> lazyModel = _fqnToModel.get().get( topLevel );
    return lazyModel == null ? null : lazyModel.get();
  }

  @Override
  public boolean handlesFile( IFile file )
  {
    return handlesFileExtension( file.getExtension() );
  }

  @Override
  public String[] getTypesForFile( IFile file )
  {
    if( !handlesFile( file ) )
    {
      return new String[0];
    }

    Set<String> fqns = getModule().getPathCache().getFqnForFile( file );
    Set<String> aliasedFqns = new HashSet<>();
    if( fqns != null )
    {
      for( String fqn : fqns )
      {
        fqn = getTypeNameForFile( fqn, file );
        if( fqn != null )
        {
          aliasedFqns.add( fqn );
        }
      }
    }
    return aliasedFqns.toArray( new String[aliasedFqns.size()] );
  }

  public IModule getModule()
  {
    return _module;
  }

  @Override
  public RefreshKind refreshedFile( IFile file, String[] types, RefreshKind kind )
  {
    _fqnToModel.clear();
    return kind;
  }

  @Override
  public boolean isType( String fqn )
  {
    FqnCache<LocklessLazyVar<M>> fqnCache = _fqnToModel.get();
    if( fqnCache.isEmpty() )
    {
      return false;
    }

    fqn = fqn.replace( '$', '.' );
    
    String topLevel = findTopLevelFqn( fqn );
    if( topLevel == null )
    {
      return false;
    }

    if( topLevel.equals( fqn ) )
    {
      return true;
    }

    return isInnerType( topLevel, fqn.substring( topLevel.length() + 1 ) );
  }

  @Override
  public boolean isPackage( String pkg )
  {
    return !getTypeNames( pkg ).isEmpty();
  }

  /**
   * This method avoids initializing all the files
   */
  @SuppressWarnings("WeakerAccess")
  public String findTopLevelFqn( String fqn )
  {
    FqnCache<LocklessLazyVar<M>> fqnCache = _fqnToModel.get();
    if( fqnCache.isEmpty() )
    {
      return null;
    }

    while( true )
    {
      LocklessLazyVar<M> lazyModel = fqnCache.get( fqn );
      if( lazyModel != null )
      {
        return fqn;
      }
      int iDot = fqn.lastIndexOf( '.' );
      if( iDot <= 0 )
      {
        return null;
      }
      fqn = fqn.substring( 0, iDot );
    }
  }

  @Override
  public boolean isTopLevelType( String fqn )
  {
    FqnCache<LocklessLazyVar<M>> fqnCache = _fqnToModel.get();
    if( fqnCache.isEmpty() )
    {
      return false;
    }

    return fqnCache.get( fqn ) != null;
  }

  @Override
  public String getPackage( String fqn )
  {
    String topLevel = findTopLevelFqn( fqn );
    return ManClassUtil.getPackage( topLevel );
  }

  @Override
  public String contribute( JavaFileManager.Location location, String fqn, String existing, DiagnosticListener<JavaFileObject> errorHandler )
  {
    String topLevel = findTopLevelFqn( fqn );
    LocklessLazyVar<M> lazyModel = _fqnToModel.get().get( topLevel );

    M model = lazyModel.get();
    String source = contribute( location, topLevel, existing, model, errorHandler );

    if( !model.isProcessing() )
    {
      // Now remove the model since we don't need it anymore
      lazyModel.clear();
    }

    return source;
  }

  @Override
  public Collection<String> getAllTypeNames()
  {
    FqnCache<LocklessLazyVar<M>> fqnCache = _fqnToModel.get();
    if( fqnCache.isEmpty() )
    {
      return Collections.emptySet();
    }

    return fqnCache.getFqns();
  }

  @Override
  public Collection<TypeName> getTypeNames( String namespace )
  {
    return getAllTypeNames().stream()
      .filter( fqn -> ManClassUtil.getPackage( fqn ).equals( namespace ) )
      .map( fqn -> new TypeName( fqn, _module, TypeName.Kind.TYPE, TypeName.Visibility.PUBLIC ) )
      .collect( Collectors.toSet() );
  }

  @Override
  public List<IFile> findFilesForType( String fqn )
  {
    String topLevel = findTopLevelFqn( fqn );
    if( topLevel == null )
    {
      return Collections.emptyList();
    }

    M model = getModel( topLevel );
    return model != null ? new ArrayList<>( model.getFiles() ) : Collections.emptyList();
  }

  @Override
  public void clear()
  {
    _fqnToModel.clear();
  }

  public static String getContent( IFile file )
  {
    if( file != null )
    {
      try( InputStream inputStream = file.openInputStream() )
      {
        return StreamUtil.getContent( new InputStreamReader( inputStream ) ); //.replace( "\r\n", "\n" );
      }
      catch( Exception e )
      {
        throw new RuntimeException( e );
      }
    }
    return null;
  }

  protected class CacheClearer extends AbstractTypeSystemListener
  {
    @Override
    public void refreshed()
    {
      clear();
    }

    public void preRefresh( RefreshRequest request )
    {
    }

    public void postRefresh( RefreshRequest request )
    {
    }

    @Override
    public void refreshedTypes( RefreshRequest request )
    {
      IModule refreshModule = request.module;
      if( refreshModule != null && refreshModule != getModule() )
      {
        return;
      }

      if( request.file == null || !handlesFile( request.file ) )
      {
        return;
      }

      Set<ITypeManifold> tms = getModule().findTypeManifoldsFor( request.file,
        tm -> tm == ResourceFileTypeManifold.this );
      if( tms.isEmpty() )
      {
        return;
      }

      preRefresh( request );

      switch( request.kind )
      {
        case MODIFICATION:
          for( String type : getTypesForFile( request.file ) )
          {
            modifiedType( Collections.singleton( request.file ), type );
          }
          break;

        case CREATION:
        {
          for( String type : getTypesForFile( request.file ) )
          {
            createdType( Collections.singleton( request.file ), type );
          }
          break;
        }

        case DELETION:
        {
          for( String type : getTypesForFile( request.file ) )
          {
            deletedType( Collections.singleton( request.file ), type );
          }
          break;
        }
      }

      postRefresh( request );
    }

    public void deletedType( Set<IFile> files, String type )
    {
      M lazyModel = getModel( type );
      if( lazyModel != null )
      {
        for( IFile file: files )
        {
          lazyModel.removeFile( file );
        }
        if( lazyModel.getFiles().size() == 0 )
        {
          _fqnToModel.get().remove( type );
        }
      }
    }

    public void createdType( Set<IFile> files, String type )
    {
      M lazyModel = getModel( type );
      if( lazyModel != null )
      {
        for( IFile file: files )
        {
          lazyModel.addFile( file );
        }
      }
      else
      {
        _fqnToModel.get().add( type, LocklessLazyVar.make( () -> _modelMapper.apply( type, files ) ) );
      }
    }

    public void modifiedType( Set<IFile> files, String type )
    {
      M lazyModel = getModel( type );
      if( lazyModel != null )
      {
        for( IFile file: files )
        {
          lazyModel.updateFile( file );
        }
      }
      else
      {
        _fqnToModel.get().add( type, LocklessLazyVar.make( () -> _modelMapper.apply( type, files ) ) );
      }
    }
  }
}
