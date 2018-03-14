package manifold.ext.producer.sample;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.host.IModuleComponent;
import manifold.api.host.RefreshKind;
import manifold.api.host.RefreshRequest;
import manifold.api.type.JavaTypeManifold;
import manifold.api.type.ResourceFileTypeManifold;
import manifold.ext.IExtensionClassProducer;
import manifold.util.StreamUtil;
import manifold.util.cache.FqnCache;
import manifold.util.concurrent.LocklessLazyVar;


import static manifold.ext.producer.sample.Model.makeExtensionClassName;

/**
 * A sample implementation to exercise the IExtensionClassProducer interface.
 * <p/>
 * Handles the contrived ".favs" file extension having the following format:
 * <pre>
 *   (<qualified-type-name> | <favorite-name> | <favorite-value> [new line])*
 * </pre>
 * For example:
 * <pre>
 *   java.lang.String|Food|Cheeseburger
 *   java.lang.String|Car|Alfieri
 *   java.util.Map|Food|Pizza
 * </pre>
 * As such this class adds methods favoriteFood() and favoriteCar() to String, and favoriteFood() to Map.
 * The methods return a String value corresponding with Cheeseburger, Alfieri, and Pizza.
 */
public class ExtensionProducerSampleTypeManifold extends JavaTypeManifold<Model> implements IExtensionClassProducer
{
  private static final String FILE_EXT = "favs";

  private final LocklessLazyVar<Map<String,LocklessLazyVar<Model>>> _extensionToModel = LocklessLazyVar.make( this::buildCache );
  private final LocklessLazyVar<Map<String, Set<IFile>>> _extensionToFiles = LocklessLazyVar.make( HashMap::new );


  @Override
  public void init( IModuleComponent typeLoader )
  {
    super.init( typeLoader, Model::new );
  }

  /**
   * Overriden because the file's fqn isn't really a type for this type manifold,
   * only the extension classes derived from the files are types this manifold produces.
   */
  @Override
  public String findTopLevelFqn( String fqn )
  {
    //noinspection ConstantConditions
    if( _extensionToModel.get().containsKey( fqn ) )
    {
      return fqn;
    }
    return null;
  }

  private Map<String, LocklessLazyVar<Model>> buildCache()
  {
    Map<String,LocklessLazyVar<Model>> extensionToModel = new HashMap<>();
    FqnCache<IFile> extensionCache = getModule().getPathCache().getExtensionCache( FILE_EXT );
    Set<String> favsFiles = extensionCache.getFqns();
    for( String favsFileFqn: favsFiles )
    {
      IFile file = extensionCache.get( favsFileFqn );
      addFile( extensionToModel, file );
    }
    return extensionToModel;
  }

  private void addFile( Map<String, LocklessLazyVar<Model>> extensionToModel, IFile file )
  {
    Set<String> extendedTypes = readExtendedTypes( file );
    for( String extended: extendedTypes )
    {
      String extension = makeExtensionClassName( extended );
      Set<IFile> files = _extensionToFiles.get().computeIfAbsent( extension, e -> new HashSet<>() );
      files.add( file );
      extensionToModel.putIfAbsent( extension, LocklessLazyVar.make( () -> new Model( extension, files ) ) );
    }
  }

  private void removeFile( IFile file )
  {
    for( String extension: getTypesForFile( file ) )
    {
      _extensionToModel.get().remove( extension );
      _extensionToFiles.get().get( extension ).remove( file );
    }
  }

  @Override
  protected ResourceFileTypeManifold<Model>.CacheClearer createCacheClearer()
  {
    return new MyCacheClearer();
  }

  @Override
  public RefreshKind refreshedFile( IFile file, String[] types, RefreshKind kind )
  {
    _extensionToModel.clear();
    return kind;
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return fileExtension.equalsIgnoreCase( FILE_EXT );
  }

  @Override
  protected Map<String, LocklessLazyVar<Model>> getPeripheralTypes()
  {
    return _extensionToModel.get();
  }

  @Override
  public boolean isInnerType( String topLevelFqn, String relativeInner )
  {
    return false;
  }

  @Override
  public boolean isExtendedType( String fqn )
  {
    String extension = makeExtensionClassName( fqn );
    //noinspection ConstantConditions
    return _extensionToModel.get().containsKey( extension );
  }

  @Override
  public Set<String> getExtensionClasses( String extendedType )
  {
    return isExtendedType( extendedType )
           ? getExtensions( extendedType )
           : Collections.emptySet();
  }

  private Set<String> getExtensions( String extendedType )
  {
    String extension = makeExtensionClassName( extendedType );
    //noinspection ConstantConditions
    if( _extensionToModel.get().containsKey( extension ) )
    {
      return Collections.singleton( extension );
    }
    return Collections.emptySet();
  }

  @Override
  public Set<String> getExtendedTypes()
  {
    //noinspection ConstantConditions
    return _extensionToModel.get()
      .keySet().stream()
      .map( Model::deriveExtendedClassFrom )
      .collect( Collectors.toSet() );
  }

  @Override
  public String[] getTypesForFile( IFile file )
  {
    if( !handlesFile( file ) )
    {
      return new String[0];
    }

    Set<String> types = new HashSet<>();
    for( Map.Entry<String, LocklessLazyVar<Model>> entry:
      Objects.requireNonNull( _extensionToModel.get() ).entrySet() )
    {
      String extension = entry.getKey();
      Model model = Objects.requireNonNull( entry.getValue().get() );
      if( model.getFiles().contains( file ) )
      {
        types.add( extension );
      }
    }
    return types.toArray( new String[types.size()] );
  }

  @Override
  public Set<String> getExtendedTypesForFile( IFile file )
  {
    if( !handlesFile( file ) )
    {
      return Collections.emptySet();
    }

    Set<String> types = new HashSet<>();
    for( Map.Entry<String, LocklessLazyVar<Model>> entry:
      Objects.requireNonNull( _extensionToModel.get() ).entrySet() )
    {
      String extension = entry.getKey();
      Model model = Objects.requireNonNull( entry.getValue().get() );
      if( model.getFiles().contains( file ) )
      {
        types.add( Model.deriveExtendedClassFrom( extension ) );
      }
    }
    return types;
  }

  private Set<String> readExtendedTypes( IFile file )
  {
    Objects.requireNonNull( file );
    String content;
    try
    {
      content = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }


    Set<String> extendedTypes = new HashSet<>();
    for( StringTokenizer tokenizer = new StringTokenizer( content, "\r\n" ); tokenizer.hasMoreTokens(); )
    {
      String line = tokenizer.nextToken();
      StringTokenizer lineTokenizer = new StringTokenizer( line, "|" );
      String extended = lineTokenizer.nextToken();
      extendedTypes.add( extended );
    }
    return extendedTypes;
  }

  @Override
  protected String contribute( String topLevelFqn, String existing, Model model,
                               DiagnosticListener<JavaFileObject> errorHandler )
  {
    return model.makeSource( topLevelFqn );
  }

  private class MyCacheClearer extends CacheClearer
  {
    @Override
    public void preRefresh( RefreshRequest request )
    {
      switch( request.kind )
      {
        case CREATION:
          addFile( _extensionToModel.get(), request.file );
          break;
        case MODIFICATION:
          removeFile( request.file );
          addFile( _extensionToModel.get(), request.file );
          break;
      }
    }

    @Override
    public void postRefresh( RefreshRequest request )
    {
      switch( request.kind )
      {
        case DELETION:
          removeFile( request.file );
          break;
      }
    }
  }
}