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
import manifold.api.host.IModule;
import manifold.api.host.ITypeLoaderListener;
import manifold.api.host.RefreshRequest;
import manifold.api.type.ITypeManifold;
import manifold.api.type.TypeName;
import manifold.internal.host.ManifoldHost;
import manifold.util.JreUtil;
import manifold.util.ManClassUtil;
import manifold.util.ReflectUtil;
import manifold.util.cache.FqnCache;
import manifold.util.cache.FqnCacheNode;


import static manifold.api.type.ITypeManifold.ProducerKind.Primary;

/**
 */
class ManifoldJavaFileManager extends JavacFileManagerBridge<JavaFileManager> implements ITypeLoaderListener
{
  private final boolean _fromJavaC;
  private FqnCache<InMemoryClassJavaFileObject> _classFiles;
  private FqnCache<JavaFileObject> _generatedFiles;
  private Log _issueLogger;
  private Context _ctx;

  ManifoldJavaFileManager( JavaFileManager fileManager, Context ctx, boolean fromJavaC )
  {
    super( fileManager, ctx == null ? ctx = new Context() : ctx );
    _ctx = ctx;
    _fromJavaC = fromJavaC;
    _classFiles = new FqnCache<>();
    _generatedFiles = new FqnCache<>();
    _issueLogger = Log.instance( ctx );
    if( ctx.get(JavaFileManager.class) == null )
    {
      ctx.put( JavaFileManager.class, fileManager );
    }
    ManifoldHost.addTypeLoaderListenerAsWeakRef( null, this );
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
    if( location == ReflectUtil.field( StandardLocation.class, "PATCH_MODULE_PATH" ).getStatic() )
    {
      //System.err.println( "PATCH: " + fo.getName() );
      return new ManPatchLocation( fo );
    }
    return super.getLocationForModule( location, fo );
  }

  @Override
  public JavaFileObject getJavaFileForOutput( Location location, String className, JavaFileObject.Kind kind, FileObject sibling ) throws IOException
  {
    if( !_fromJavaC || kind == JavaFileObject.Kind.CLASS && sibling instanceof GeneratedJavaStubFileObject )
    {
      InMemoryClassJavaFileObject file = new InMemoryClassJavaFileObject( className, kind );
      _classFiles.add( className, file );
      className = className.replace( '$', '.' );
      _classFiles.add( className, file );
      return file;
    }
    return super.getJavaFileForOutput( location, className, kind, sibling );
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

    return findGeneratedFile( fqn.replace( '$', '.' ), ManifoldHost.getCurrentModule(), errorHandler );
  }

  public Iterable<JavaFileObject> list( Location location, String packageName, Set<JavaFileObject.Kind> kinds, boolean recurse ) throws IOException
  {
    Iterable<JavaFileObject> list = super.list( location, packageName, kinds, recurse );
    if( kinds.contains( JavaFileObject.Kind.SOURCE ) && (location == StandardLocation.SOURCE_PATH || location == StandardLocation.CLASS_PATH || location instanceof ManPatchModuleLocation) )
    {
      Set<TypeName> children = ManifoldHost.getChildrenOfNamespace( packageName );
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
          JavaFileObject file = findGeneratedFile( fqn, tn.getModule(), issueReporter );
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
      Set<ITypeManifold> typeManifoldsFor = ManifoldHost.getCurrentModule().findTypeManifoldsFor( fqn );
      return typeManifoldsFor.stream().anyMatch( tm -> tm.getProducerKind() == Primary );
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
    PathCache pathCache = ManifoldHost.getCurrentModule().getPathCache();
    return pathCache.getExtensionCache( "class" ).get( fqn ) != null;
  }

  private JavaFileObject findGeneratedFile( String fqn, IModule module, DiagnosticListener<JavaFileObject> errorHandler )
  {
    FqnCacheNode<JavaFileObject> node = _generatedFiles.getNode( fqn );
    if( node != null )
    {
      return node.getUserData();
    }

    JavaFileObject file = ManifoldHost.produceFile( fqn, module, errorHandler );
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
    if( location instanceof ManPatchModuleLocation )
    {
      if( fileObj.getClass().getSimpleName().equals( "DirectoryFileObject" ) )
      {
          RelativePath relativePath = (RelativePath)ReflectUtil.field( fileObj, "relativePath" ).get();
          return removeExtension( relativePath.getPath() ).replace( File.separatorChar, '.' ).replace( '/', '.' );
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
}
