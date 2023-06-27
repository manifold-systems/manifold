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
import manifold.api.type.AbstractSingleFileModel;
import manifold.json.rt.Json;
import manifold.rt.api.Bindings;
import manifold.rt.api.util.ManStringUtil;
import manifold.rt.api.util.StreamUtil;
import manifold.sql.rt.api.DbConfig;
import manifold.sql.rt.connection.DbConfigImpl;
import manifold.sql.schema.api.Schema;
import manifold.sql.schema.api.SchemaProvider;
import manifold.sql.schema.api.SchemaTable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;

public class SchemaModel extends AbstractSingleFileModel
{
  private final SchemaManifold _schemaManifold;
  private Schema _schema;
  private DbConfigImpl _dbConfig;
  private SchemaParentType _type;

  @SuppressWarnings( "WeakerAccess" )
  public SchemaModel( SchemaManifold schemaManifold, String fqn, Set<IFile> files )
  {
    super( schemaManifold.getModule().getHost(), fqn, files );
    _schemaManifold = schemaManifold;
    init();
  }

  private void init()
  {
    _schema = loadSchema();
    _type = new SchemaParentType( this );
  }

  private Schema loadSchema()
  {
    try( Reader reader = new InputStreamReader( getFile().openInputStream() ) )
    {
      Bindings bindings = (Bindings)Json.fromJson( StreamUtil.getContent( reader ) );
      bindings.put( "name", getFile().getBaseName() );
      bindings.put( "path", getFile().getPath().getFileSystemPathString() );
      _dbConfig = new DbConfigImpl( bindings );
      return SchemaProvider.PROVIDERS.get().stream()
        .map( sp -> sp.getSchema( _dbConfig ) )
        .filter( schema -> schema != null )
        .findFirst().orElse( null );
    }
    catch( Exception e )
    {
      throw new RuntimeException( e );
    }
  }

  DbConfig getDbConfig()
  {
    return _dbConfig;
  }

  Schema getSchema()
  {
    return _schema;
  }

  SchemaParentType getType()
  {
    return _type;
  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    init();
  }

  SchemaTable getTable( String simpleName )
  {
    return _schema.getTable( _schema.getOriginalName( simpleName ) );
  }
}