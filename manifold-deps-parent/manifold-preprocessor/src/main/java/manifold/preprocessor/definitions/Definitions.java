/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.preprocessor.definitions;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import manifold.api.fs.IDirectory;
import manifold.api.fs.IFile;
import manifold.api.fs.IResource;
import manifold.util.concurrent.LocklessLazyVar;

/**
 * Models definitions as a hierarchy of maps.  Searches for a definition in leaf-first order where the leaf definitions
 * are controlled by the {@code #define} and {@code #undef} directives in the compiling source file.  Parent definitions
 * correspond with the following:
 * <ul>
 *   <li> {@code build.properties} files in the source file's directory ancestry </li>
 *   <li> Custom compiler arguments provided by the {@code -Akey[=value]} javac command line option </li>
 *   <li> Compiler and JVM environment settings such as Java source version, JPMS mode, operating system, etc.
 * </ul>
 * Note the effects of {@code #define} and {@code #undef} are limited to the file scope. This means {@code #define}
 * definitions are not available to other files.  Similarly, parent definitions masked with {@code #undef} are
 * not affected in other files.
 */
public class Definitions
{
  public static final String BUILD_PROPERTIES = "build.properties";

  private final IFile _definitionsSource;
  private final LocklessLazyVar<Definitions> _parent;
  private final Map<String, String> _localDefs;
  private final Map<String, String> _localUnDefs;

  public Definitions( IFile definitionsSource )
  {
    this( definitionsSource, null );
  }

  protected Definitions( IFile definitionsSource, Map<String, String> definitions )
  {
    _definitionsSource = definitionsSource;
    _parent = LocklessLazyVar.make( () -> loadParentDefinitions() );
    _localDefs = definitions == null ? new HashMap<>() : definitions;
    _localUnDefs = new HashMap<>();
  }

  protected Definitions loadParentDefinitions()
  {
    Definitions parentDefinitions = null;

    if( _definitionsSource != null )
    {
      IFile source = _definitionsSource;
      if( source.exists() )
      {
        parentDefinitions = findBuildProperties( source );
      }
    }

    if( parentDefinitions == null )
    {
      parentDefinitions = new Definitions( null, loadEnvironmentDefinitions() )
      {
        @Override
        protected Definitions loadParentDefinitions()
        {
          return null;
        }
      };
    }

    return parentDefinitions;
  }

  protected Map<String, String> loadEnvironmentDefinitions()
  {
    return EnvironmentDefinitions.instance().getEnv();
  }

  private Definitions findBuildProperties( IResource source )
  {
    if( source == null )
    {
      return null;
    }

    if( source instanceof IFile && source.exists() )
    {
      if( source.getName().equalsIgnoreCase( BUILD_PROPERTIES ) )
      {
        // go up two since we already have the build.properties for its own parent
        source = source.getParent();
      }
      source = source.getParent();
      if( source == null )
      {
        return null;
      }
    }

    if( ((IDirectory)source).hasChildFile( BUILD_PROPERTIES ) )
    {
      return makeBuildDefinitions( ((IDirectory)source).file( BUILD_PROPERTIES ) );
    }
    return findBuildProperties( source.getParent() );
  }

  private Definitions makeBuildDefinitions( IFile source )
  {
    Properties properties = new Properties();
    try( InputStream input = source.openInputStream() )
    {
      properties.load( input );
      //noinspection unchecked
      return new Definitions( source, (Map)properties );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public void clear()
  {
    _localDefs.clear();
    _localUnDefs.clear();
  }

  /**
   * @return True if there a definition having name {@code def}, regardless of its value.
   */
  public boolean isDefined( String def )
  {
    if( _localUnDefs.containsKey( def ) )
    {
      return false;
    }

    if( _localDefs.containsKey( def ) )
    {
      return true;
    }

    Definitions parent = _parent.get();
    return parent != null && parent.isDefined( def );
  }

  public String getValue( String def )
  {
    if( _localUnDefs.containsKey( def ) )
    {
      return null;
    }

    if( _localDefs.containsKey( def ) )
    {
      return _localDefs.get( def );
    }

    Definitions parent = _parent.get();
    return parent == null ? null : parent.getValue( def );
  }

  /**
   * Define {@code def} in the source file's local definition space.
   */
  public String define( String def )
  {
    return define( def, "" );
  }

  /**
   * Define {@code def} in the source file's local definition space with {@code value}.
   */
  public String define( String def, String value )
  {
    _localUnDefs.remove( def );
    return _localDefs.put( def, value );
  }

  /**
   * Remove {@code def} from the File's local definition space. Note if {@code def} is defined in a parent scope
   * e.g., a properties file, it remains defined.  In other words {@code #undef} applies exclusively to the source
   * file scope.
   */
  public String undef( String def )
  {
    _localUnDefs.put( def, "" );
    return _localDefs.remove( def );
  }


  @Override
  public boolean equals( Object o )
  {
    if( this == o )
    {
      return true;
    }
    if( o == null || getClass() != o.getClass() )
    {
      return false;
    }
    Definitions that = (Definitions)o;
    return _localDefs.equals( that._localDefs ) &&
           _localUnDefs.equals( that._localUnDefs );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( _localDefs, _localUnDefs );
  }
}
