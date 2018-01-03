package manifold.internal.javac;

import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
import manifold.api.type.TypeName;
import manifold.internal.host.ManifoldHost;
import manifold.util.ManClassUtil;
import manifold.util.cache.FqnCache;
import manifold.util.cache.FqnCacheNode;

/**
 */
class ManifoldJavaFileManager extends JavacFileManagerBridge<JavaFileManager> implements ITypeLoaderListener
{
  private final boolean _fromJavaC;
  private FqnCache<InMemoryClassJavaFileObject> _classFiles;
  private FqnCache<JavaFileObject> _generatedFiles;
  private JavaFileManager _javacMgr;
  private Log _issueLogger;

  ManifoldJavaFileManager( JavaFileManager fileManager, Context ctx, boolean fromJavaC )
  {
    super( fileManager, ctx == null ? ctx = new Context() : ctx );
    _fromJavaC = fromJavaC;
    _javacMgr = fileManager;
    _classFiles = new FqnCache<>();
    _generatedFiles = new FqnCache<>();
    _issueLogger = Log.instance( ctx );
    ManifoldHost.addTypeLoaderListenerAsWeakRef( null, this );
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

  public JavaFileObject getSourceFileForInput( Location location, String fqn, JavaFileObject.Kind kind, DiagnosticListener<JavaFileObject> errorHandler ) throws IOException
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
    if( kinds.contains( JavaFileObject.Kind.SOURCE ) && (location == StandardLocation.SOURCE_PATH || location == StandardLocation.CLASS_PATH) )
    {
      Set<TypeName> children = ManifoldHost.getChildrenOfNamespace( packageName );
      if( children == null || children.isEmpty() )
      {
        return list;
      }

      ArrayList<JavaFileObject> newList = new ArrayList<>();
      list.forEach( newList::add );
      Set<String> names = makeNames( list );

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
            Iterable<JavaFileObject> sublist = list( location, tn.name, kinds, recurse );
            sublist.forEach( newList::add );
          }
        }
        else
        {
          IssueReporter<JavaFileObject> issueReporter = new IssueReporter<>( _issueLogger );
          JavaFileObject file = findGeneratedFile( tn.name.replace( '$', '.' ), tn.getModule(), issueReporter );
          if( file != null )
          {
            newList.add( file );
          }
        }
      }
      list = newList;
    }
    return list;
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
