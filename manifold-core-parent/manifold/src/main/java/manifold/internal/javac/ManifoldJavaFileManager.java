package manifold.internal.javac;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.file.RelativePath;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.util.Name;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import manifold.api.fs.cache.PathCache;
import manifold.api.host.IManifoldHost;
import manifold.api.host.IModule;
import manifold.api.host.ITypeSystemListener;
import manifold.api.host.RefreshRequest;
import manifold.api.type.ITypeManifold;
import manifold.api.type.TypeName;
import manifold.internal.host.SimpleModule;
import manifold.util.JreUtil;
import manifold.util.ManClassUtil;
import manifold.util.ReflectUtil;
import manifold.util.cache.FqnCache;
import manifold.util.cache.FqnCacheNode;


import static manifold.api.type.ContributorKind.Primary;

/**
 */
class ManifoldJavaFileManager extends JavacFileManagerBridge<JavaFileManager> implements ITypeSystemListener
{
  private final IManifoldHost _host;
  private final boolean _fromJavaC;
  private FqnCache<InMemoryClassJavaFileObject> _classFiles;
  private FqnCache<JavaFileObject> _generatedFiles;
  private Log _issueLogger;
  private Context _ctx;
  private int _runtimeMode;

  ManifoldJavaFileManager( IManifoldHost host, JavaFileManager fileManager, Context ctx, boolean fromJavaC )
  {
    super( fileManager, ctx == null ? ctx = new Context() : ctx );
    _host = host;
    _ctx = ctx;
    _fromJavaC = fromJavaC;
    _classFiles = new FqnCache<>();
    _generatedFiles = new FqnCache<>();
    _issueLogger = Log.instance( ctx );
    if( ctx.get(JavaFileManager.class) == null )
    {
      ctx.put( JavaFileManager.class, fileManager );
    }
    _host.addTypeSystemListenerAsWeakRef( null, this );
  }

  public IManifoldHost getHost()
  {
    return _host;
  }

  /**
   * @since 9
   */
  public String inferModuleName( Location location )
  {
    if( location instanceof ManPatchLocation )
    {
      String name = ((ManPatchLocation)location).inferModuleName( _ctx );
      if( name != null )
      {
        return name;
      }
    }
    return super.inferModuleName( location );
  }

  @Override
  public boolean hasLocation( Location location )
  {
    return !JavacPlugin.IS_JAVA_8 && location == ReflectUtil.field( StandardLocation.class, "PATCH_MODULE_PATH" ).getStatic()
           || super.hasLocation( location );
  }

  /**
   * @since 9
   */
  @Override
  public Location getLocationForModule( Location location, String moduleName ) throws IOException
  {
    if( location == ReflectUtil.field( StandardLocation.class, "PATCH_MODULE_PATH" ).getStatic() )
    {
      return new ManPatchModuleLocation( moduleName );
    }
    return super.getLocationForModule( location, moduleName );
  }

  /**
   * @since 9
   */
  @Override
  public Location getLocationForModule( Location location, JavaFileObject fo ) throws IOException
  {
    if( fo instanceof GeneratedJavaStubFileObject &&
        location == ReflectUtil.field( StandardLocation.class, "PATCH_MODULE_PATH" ).getStatic() )
    {
      //System.err.println( "PATCH: " + fo.getName() );
      return new ManPatchLocation( (GeneratedJavaStubFileObject)fo );
    }
    return super.getLocationForModule( location, fo );
  }

  @Override
  public JavaFileObject getJavaFileForOutput( Location location, String className, JavaFileObject.Kind kind, FileObject sibling ) throws IOException
  {
    if( !okToWriteClassFile( kind, sibling ) )
    {
      InMemoryClassJavaFileObject file = new InMemoryClassJavaFileObject( className, kind );
      if( !(sibling instanceof GeneratedJavaStubFileObject) || ((GeneratedJavaStubFileObject)sibling).isPrimary() )
      {
        // only retain primary class files e.g., don't keep stubbed class files from extension classes

        _classFiles.add( className, file );
        className = className.replace( '$', '.' );
        _classFiles.add( className, file );
      }
      return file;
    }
    return super.getJavaFileForOutput( location, className, kind, sibling );
  }

  private boolean okToWriteClassFile( JavaFileObject.Kind kind, FileObject fo )
  {
    // it's ok to write a type manifold class to disk if we're running javac and the class is not an extended java class

    return !isRuntimeMode() && _fromJavaC &&
           !isIntellijPluginTemporaryFile( kind, fo ) &&
           (kind != JavaFileObject.Kind.CLASS ||
            !(fo instanceof GeneratedJavaStubFileObject) ||
            (JavacPlugin.instance().isStaticCompile() && ((GeneratedJavaStubFileObject)fo).isPrimary()));
  }

  // ManChangedResourceBuilder from IJ plugin
  private boolean isIntellijPluginTemporaryFile( JavaFileObject.Kind kind, FileObject fo )
  {
    String name = fo == null ? null : fo.getName();
    return name != null && name.contains( "_Manifold_Temp_Main_" );
  }

  public InMemoryClassJavaFileObject findCompiledFile( String fqn )
  {
    return _classFiles.get( fqn );
  }

  public JavaFileObject getSourceFileForInput( Location location, String fqn, JavaFileObject.Kind kind, DiagnosticListener<JavaFileObject> errorHandler )
  {
    try
    {
      JavaFileObject file = super.getJavaFileForInput( location, fqn, kind );
      if( file != null )
      {
        return file;
      }
    }
    catch( IOException ignore )
    {
    }

    return findGeneratedFile( fqn.replace( '$', '.' ), location, getHost().getSingleModule(), errorHandler );
  }

  public Iterable<JavaFileObject> list( Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse ) throws IOException
  {
    Iterable<JavaFileObject> list = super.list( location, packageName, kinds, recurse );
    if( kinds.contains( JavaFileObject.Kind.SOURCE ) && (location == StandardLocation.SOURCE_PATH || location == StandardLocation.CLASS_PATH || location instanceof ManPatchModuleLocation) )
    {
      Set<TypeName> children =((SimpleModule)getHost().getSingleModule()).getChildrenOfNamespace( packageName );
      if( children == null || children.isEmpty() )
      {
        return list;
      }

      ArrayList<JavaFileObject> newList = new ArrayList<>();
      list.forEach( newList::add );
      Set<String> names = makeNames( list );

      Iterable<JavaFileObject> patchableFiles = null;
      if( location instanceof ManPatchModuleLocation )
      {
        Set<JavaFileObject.Kind> classesAndSource = new HashSet<>( Arrays.asList( JavaFileObject.Kind.CLASS, JavaFileObject.Kind.SOURCE ) );
        Location modulePatchLocation = makeModuleLocation( (ManPatchModuleLocation)location );
        if( modulePatchLocation == null )
        {
          return list;
        }
        // Get a list of class files from the patch module location; these are patch candidates
        patchableFiles = super.list( modulePatchLocation, packageName, classesAndSource, recurse );
      }

      for( TypeName tn : children )
      {
        if( names.contains( ManClassUtil.getShortClassName( tn.name ) ) )
        {
          continue;
        }

        if( isClassFile( tn ) )
        {
          continue;
        }

        if( tn.kind == TypeName.Kind.NAMESPACE )
        {
          if( recurse )
          {
            //noinspection ConstantConditions
            Iterable<JavaFileObject> sublist = list( location, tn.name, kinds, recurse );
            sublist.forEach( newList::add );
          }
        }
        else
        {
          IssueReporter<JavaFileObject> issueReporter = new IssueReporter<>( _issueLogger );
          String fqn = tn.name.replace( '$', '.' );
          JavaFileObject file = findGeneratedFile( fqn, location, tn.getModule(), issueReporter );
          if( file != null && isCorrectModule( location, patchableFiles, file, fqn ) )
          {
            newList.add( file );
          }
        }
      }
      list = newList;
    }
    return list;
  }

  private boolean isCorrectModule( Location location, Iterable<JavaFileObject> patchableFiles, JavaFileObject file, String fqn )
  {
    if( !(location instanceof ManPatchModuleLocation) )
    {
      // not a ManPatchModuleLocation means not an extended class

      if( !JreUtil.isJava9Modular_compiler( _ctx ) )
      {
        return true;
      }

      // true if type is not exclusively an extended type
      Set<ITypeManifold> typeManifoldsFor = getHost().getSingleModule().findTypeManifoldsFor( fqn );
      return typeManifoldsFor.stream().anyMatch( tm -> tm.getContributorKind() == Primary );
    }

    if( patchableFiles == null )
    {
      return true;
    }

    String cname = inferBinaryName( location, file );

    for( JavaFileObject f: patchableFiles )
    {
      String name = inferBinaryName( location, f );

      if( cname.equals( name) )
      {
        return true;
      }
    }

    return false;
  }

  private Location makeModuleLocation( ManPatchModuleLocation location )
  {
    // Module module = Modules.instance( _ctx ).getObservableModule( Names.instance( _ctx ).fromString( location.getName() ) );
    // return module.classLocation;

    Symbol moduleElement = (Symbol)ReflectUtil.method( Symtab.instance( _ctx ), "getModule", Name.class )
      .invoke( Names.instance( _ctx ).fromString( location.getName() ) );
    if( moduleElement == null )
    {
      return null;
    }
    return (Location)ReflectUtil.field( moduleElement, "classLocation" ).get();
  }

  private boolean isClassFile( TypeName tn )
  {
    String fqn = tn.name;
    int iDollar = fqn.indexOf( '$' );
    if( iDollar > 0 )
    {
      fqn = fqn.substring( 0, iDollar );
    }
    PathCache pathCache = getHost().getSingleModule().getPathCache();
    return pathCache.getExtensionCache( "class" ).get( fqn ) != null;
  }

  private JavaFileObject findGeneratedFile( String fqn, Location location, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    FqnCacheNode<JavaFileObject> node = _generatedFiles.getNode( fqn );
    if( node != null )
    {
      return node.getUserData();
    }

    JavaFileObject file = module.produceFile( fqn, location, errorHandler );
    // note we cache even if file is null, fqn cache is also a miss cache
    _generatedFiles.add( fqn, file );

    return file;
  }

  private Set<String> makeNames( Iterable<JavaFileObject> list )
  {
    HashSet<String> set = new HashSet<>();
    for( JavaFileObject file : list )
    {
      String name = file.getName();
      if( name.endsWith( ".java" ) )
      {
        set.add( name.substring( name.lastIndexOf( File.separatorChar ) + 1, name.lastIndexOf( '.' ) ) );
      }
    }
    return set;
  }

  public void remove( String fqn )
  {
    _classFiles.remove( fqn );
  }

  @Override
  public void refreshedTypes( RefreshRequest request )
  {
    switch( request.kind )
    {
      case CREATION:
      case MODIFICATION:
      case DELETION:
        // Remove all affected types for any refresh kind.
        // Note we remove types for CREATION request because we could have cached misses to the type name.
        _classFiles.remove( request.types );
        _generatedFiles.remove( request.types );
        break;
    }
  }

  @Override
  public void refreshed()
  {
    _classFiles = new FqnCache<>();
  }

  public Collection<InMemoryClassJavaFileObject> getCompiledFiles()
  {
    HashSet<InMemoryClassJavaFileObject> files = new HashSet<>();
    _classFiles.visitDepthFirst(
      o ->
      {
        if( o != null )
        {
          files.add( o );
        }
        return true;
      } );
    return files;
  }

  @Override
  public String inferBinaryName( Location location, JavaFileObject fileObj )
  {
    if( fileObj instanceof GeneratedJavaStubFileObject )
    {
      return removeExtension( fileObj.getName() ).replace( File.separatorChar, '.' ).replace( '/', '.' );
    }

    if( fileObj instanceof SourceJavaFileObject )
    {
      return ((SourceJavaFileObject)fileObj).inferBinaryName( location );
    }

    if( location instanceof ManPatchModuleLocation )
    {
      if( fileObj.getClass().getSimpleName().equals( "DirectoryFileObject" ) )
      {
          RelativePath relativePath = (RelativePath)ReflectUtil.field( fileObj, "relativePath" ).get();
          return removeExtension( relativePath.getPath() ).replace( File.separatorChar, '.' ).replace( '/', '.' );
      }
      else if( fileObj.getClass().getSimpleName().equals( "SigJavaFileObject" ) )
      {
        // Since Java 10 javac uses .sig files...
        FileObject fileObject = (FileObject)ReflectUtil.field( fileObj, "fileObject" ).get();
        return fileObject instanceof JavaFileObject ? inferBinaryName( location, (JavaFileObject)fileObject ) : null;
      }
      else if( fileObj.getClass().getSimpleName().equals( "JarFileObject" ) )
      {
        String relativePath = ReflectUtil.method( fileObj, "getPath" ).invoke().toString();
        if( relativePath.startsWith( "/" ) )
        {
          relativePath = relativePath.substring( 1 );
        }
        return removeExtension( relativePath ).replace( File.separatorChar, '.' ).replace( '/', '.' );
      }
    }
    return super.inferBinaryName( location, fileObj );
  }

  @Override
  public boolean isSameFile( FileObject file1, FileObject file2 )
  {
    if( file1 instanceof GeneratedJavaStubFileObject || file2 instanceof GeneratedJavaStubFileObject )
    {
      return file1.equals( file2 );
    }
    return super.isSameFile( file1, file2 );
  }

  private static String removeExtension( String fileName )
  {
    int iDot = fileName.lastIndexOf( "." );
    return iDot == -1 ? fileName : fileName.substring( 0, iDot );
  }

  public int pushRuntimeMode()
  {
    return _runtimeMode++;
  }
  public void popRuntimeMode( int check )
  {
    if( --_runtimeMode != check )
    {
      throw new IllegalStateException( "runtime mode unbalanced" );
    }
  }
  public boolean isRuntimeMode()
  {
    return _runtimeMode > 0;
  }
}
