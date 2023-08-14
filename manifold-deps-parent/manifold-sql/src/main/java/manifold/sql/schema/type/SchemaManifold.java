/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.sql.schema.type;

import manifold.api.fs.IFile;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;
import manifold.api.util.cache.FqnCache;
import manifold.internal.javac.JavacPlugin;
import manifold.json.rt.Json;
import manifold.rt.api.Bindings;
import manifold.rt.api.util.ManClassUtil;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.impl.DbConfigImpl;
import manifold.sql.schema.api.Schema;
import manifold.sql.schema.api.SchemaTable;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import java.util.function.Function;

import static manifold.sql.rt.api.DbLocationProvider.Mode.CompileTime;
import static manifold.sql.rt.api.DbLocationProvider.Mode.DesignTime;

public class SchemaManifold extends JavaTypeManifold<SchemaModel>
{
  public static final String DBCONFIG_EXT = "dbconfig";
  public static final List<String> EXTS = Collections.singletonList( DBCONFIG_EXT );

  @Override
  public void init( IModule module )
  {
    init( module, (fqn, files) -> new SchemaModel( this, fqn, files ) );
  }

  @Override
  public String getTypeNameForFile( String defaultFqn, IFile file )
  {
    //## todo: cache this name mapping? it's called A LOT

    try( Reader reader = new InputStreamReader( file.openInputStream() ) )
    {
      Function<String, FqnCache<IFile>> resByExt = ext ->
        getModule().getPathCache().getExtensionCache( ext );
      Bindings bindings = (Bindings)Json.fromJson( StreamUtil.getContent( reader ) );
      DbConfigImpl dbConfig = new DbConfigImpl( resByExt, bindings, JavacPlugin.instance() != null ? CompileTime : DesignTime );
      String schemaPackage = dbConfig.getSchemaPackage();
      if( schemaPackage == null )
      {
        throw new RuntimeException( "Missing 'schemaPackage' from DbConfig file: " + file );
      }
      String simpleName = ManClassUtil.getShortClassName( defaultFqn );
      return super.getTypeNameForFile( schemaPackage + '.' + simpleName, file );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return EXTS.contains( fileExtension );
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    SchemaModel model = getModel( topLevel );
    Schema type = model == null ? null : model.getSchema();
    if( type == null )
    {
      return false;
    }

    SchemaTable table = model.getTable( relativeInner );
    return table != null;
  }

  /**
   * Override so that getModel() can be called within this package (see SchemaScope)
   */
  @Override
  public SchemaModel getModel( String fqn )
  {
    return super.getModel( fqn );
  }

  public Schema getSchema( IFile file )
  {
    // get schema from the corresponding SchemaModel

    Set<String> fqns = getModule().getPathCache().getFqnForFile( file );
    if( fqns.isEmpty() )
    {
      return null;
    }
    SchemaModel model = fqns.stream()
      .map( fqn -> getModel( getTypeNameForFile( fqn, file ) ) )
      .filter( m -> m != null )
      .findFirst().orElse( null );
    return model == null ? null : model.getSchema();
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, boolean genStubs, String existing, SchemaModel model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    StringBuilder sb = new StringBuilder();
    model.getType().render( sb, location, getModule(), errorHandler );
    model.report( errorHandler );
    return sb.toString();
  }
}
