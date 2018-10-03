package manifold.internal.host;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFileSystem;
import manifold.util.SourcePathUtil;

/**
 * A Manifold host exclusive to the javac plugin.
 */
public class JavacManifoldHost extends SingleModuleManifoldHost
{
  public void initializeAndCompileNonJavaFiles( Supplier<Set<String>> sourcePath, Supplier<List<String>> classpath, Supplier<List<String>> outputPath )
  {
    List<String> cp = classpath.get().stream().filter( e -> !SourcePathUtil.excludeFromSourcePath( e ) ).collect( Collectors.toList() );
    Set<String> sp = sourcePath.get().stream().filter( e -> !SourcePathUtil.excludeFromSourcePath( e ) ).collect( Collectors.toSet() );
    List<String> op = outputPath.get();

    int i = 0;
    for( String p : op )
    {
      if( !cp.contains( p ) )
      {
        // ensure output path is in the classpath
        cp.add( i++, p );
      }
    }

    List<String> all = new ArrayList<>();
    for( String p : sp )
    {
      if( !all.contains( p ) )
      {
        all.add( p );
      }
    }
    for( String p : cp )
    {
      if( !all.contains( p ) )
      {
        all.add( p );
      }
    }
    initPaths( cp, all, op );
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
