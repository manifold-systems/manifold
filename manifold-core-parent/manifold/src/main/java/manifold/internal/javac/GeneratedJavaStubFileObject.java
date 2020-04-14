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

package manifold.internal.javac;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import javax.tools.SimpleJavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.type.ISelfCompiledFile;
import manifold.util.concurrent.LocklessLazyVar;

import static manifold.api.type.ITypeManifold.ARG_DUMP_SOURCE;

/**
 */
public class GeneratedJavaStubFileObject extends SimpleJavaFileObject implements ISelfCompiledFile
{
  private String _name;
  private long _timestamp;
  private SourceSupplier _sourceSupplier;
  private LocklessLazyVar<String> _src = LocklessLazyVar.make( () -> _sourceSupplier.getSource() );
  private LocklessLazyVar<Boolean> _isFragment = LocklessLazyVar.make(
    () -> getResourceFiles().stream().anyMatch( f -> f instanceof IFileFragment ) );

  public GeneratedJavaStubFileObject( String name, SourceSupplier sourceSupplier )
  {
    super( URI.create( "genstub:///" + name.replace( '.', '/' ) + Kind.SOURCE.extension ), Kind.SOURCE );
    _name = name.replace( '.', '/' ) + Kind.SOURCE.extension;
    _timestamp = System.currentTimeMillis();
    _sourceSupplier = sourceSupplier;
  }

  @Override
  public URI toUri()
  {
    return URI.create( "genstub:///" + getName() );
  }

  @Override
  public String getName()
  {
    return _name;
  }

  public boolean isPrimary()
  {
    return _sourceSupplier.isPrimary();
  }

  @Override
  public boolean isSelfCompile( String fqn )
  {
    return _sourceSupplier.isSelfCompile( fqn );
  }

  @Override
  public byte[] compile( String fqn )
  {
    return _sourceSupplier.compile( fqn );
  }

  @Override
  public InputStream openInputStream()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public OutputStream openOutputStream()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public CharSequence getCharContent( boolean ignoreEncodingErrors )
  {
    String source = _src.get();
    maybeDumpSource( source );
    return source;
  }

  private void maybeDumpSource( String source )
  {
    if( shouldDumpSource() )
    {
      System.out.println( "\n================\n" );
      System.out.println( getName() );
      System.out.println( "\n================\n" );
      int[] line = new int[]{1};
      String code = Arrays.stream(source.split("\n")).map(s -> line[0]++ + ":  " + s + "\n").reduce(String::concat).orElse("");
      System.out.println(code);
    }
  }

  private static Boolean _dumpSource;
  private boolean shouldDumpSource()
  {
    return _dumpSource == null
           ? _dumpSource = !System.getProperty( ARG_DUMP_SOURCE, "" ).isEmpty()
           : _dumpSource;
  }

  @Override
  public Writer openWriter()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public long getLastModified()
  {
    return _timestamp;
  }

  @Override
  public boolean delete()
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals( Object o )
  {
    if( !(o instanceof GeneratedJavaStubFileObject) )
    {
      return false;
    }
    return _name.equals( ((GeneratedJavaStubFileObject)o).getName() );
  }

  @Override
  public int hashCode()
  {
    return _name.hashCode();
  }

  @Override
  public Kind getKind()
  {
    return Kind.SOURCE;
  }

  @Override
  public boolean isNameCompatible( String simpleName, Kind kind )
  {
    return !simpleName.equals( "module-info" ) && !simpleName.equals( "package-info" );
  }

  public boolean isFileFragment()
  {
    return _isFragment.get();
  }
  public IFileFragment getFileFragment()
  {
    return (IFileFragment)getResourceFiles().stream()
      .filter( f -> f instanceof IFileFragment )
      .findFirst().orElse( null );
  }

  /**
   * Resource files from which the type is created.
   */
  public Set<IFile> getResourceFiles()
  {
    return _sourceSupplier.getResourceFiles();
  }
}
