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

package manifold.sql.query.type;

import manifold.api.fs.IFile;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class SqlManifold extends JavaTypeManifold<SqlModel>
{
  public static final List<String> EXTS = Collections.singletonList( "sql" );

  private SqlScopeFinder _scopeFinder;

  @Override
  public void init( IModule module )
  {
    _scopeFinder = new SqlScopeFinder( module );
    init( module, (fqn, files) -> new SqlModel( this, fqn, files ) );
  }

  public SqlScopeFinder getScopeFinder()
  {
    return _scopeFinder;
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return EXTS.contains( fileExtension );
  }

  @Override
  public String getTypeNameForFile( String fqn, IFile file )
  {
    // handle sql file names that encode the DbConfig name as a secondary extension: MyQuery.MyDbConfig.sql
    // type name is MyQuery

    String fileBaseName = file.getBaseName();
    int contentExt = fileBaseName.lastIndexOf( '.' );
    if( contentExt < 0 )
    {
      // No secondary extension in name, keep fqn as-is
      return fqn;
    }
    int indexContentExt = fqn.lastIndexOf( '_' );
    if( fqn.substring( indexContentExt + 1 ).equals( fileBaseName.substring( contentExt + 1 ) ) )
    {
      return fqn.substring( 0, indexContentExt );
    }
    return fqn;
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    return false;
  }

  public <R> R findByModel( Function<SqlModel, R> byModel )
  {
    return getAllTypeNames().stream()
      .map( fqn -> {
        SqlModel model = getModel( fqn );
        return model == null ? null : byModel.apply( model );
      } )
      .filter( Objects::nonNull )
      .findFirst().orElse( null );
  }

  public <R> Stream<R> findAllByModel( Function<SqlModel, R> byModel )
  {
    return getAllTypeNames().stream()
      .map( fqn -> {
        SqlModel model = getModel( fqn );
        return model == null ? null : byModel.apply( model );
      } )
      .filter( Objects::nonNull );
  }

  /**
   * Override so that getModel() can be called within this package (see SqlScope)
   */
  @Override
  protected SqlModel getModel( String fqn )
  {
    return super.getModel( fqn );
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, boolean genStubs, String existing, SqlModel model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    StringBuilder sb = new StringBuilder();
    if( !model.getScope().hasConfigErrors() )
    {
      model.getType().render( sb, location, getModule(), errorHandler );
    }
    model.report( errorHandler );
    return sb.toString();
  }
}
