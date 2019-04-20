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

package manifold.graphql.type;

import graphql.language.ScalarTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.TypeDefinition;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;

public class GqlManifold extends JavaTypeManifold<GqlModel>
{
  public static final List<String> EXTS = Arrays.asList( "graphql", "gql" );

  @Override
  public void init( IModule module )
  {
    init( module, (fqn, files) -> new GqlModel( this, fqn, files ) );
  }

  @Override
  public boolean handlesFileExtension( String fileExtension )
  {
    return EXTS.contains( fileExtension );
  }

  @Override
  public boolean isInnerType( String topLevel, String relativeInner )
  {
    GqlModel model = getModel( topLevel );
    GqlParentType type = model == null ? null : model.getType();
    if( type == null )
    {
      return false;
    }

    return type.hasChild( relativeInner );
  }

  TypeDefinition findTypeDefinition( String simpleName )
  {
    return getAllTypeNames().stream()
      .map( fqn -> getModel( fqn ).getTypeDefinition( simpleName ) )
      .filter( Objects::nonNull )
      .findFirst().orElse( null );
  }

  ScalarTypeDefinition findScalarTypeDefinition( String simpleName )
  {
    return getAllTypeNames().stream()
      .map( fqn -> getModel( fqn ).getScalarTypeDefinition( simpleName ) )
      .filter( Objects::nonNull )
      .findFirst().orElse( null );
  }

  SchemaDefinition findSchemaDefinition()
  {
    return getAllTypeNames().stream()
      .map( fqn -> getModel( fqn ).getSchemaDefinition() )
      .filter( Objects::nonNull )
      .findFirst().orElse( null );
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, String existing, GqlModel model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    StringBuilder sb = new StringBuilder();
    model.report( errorHandler );
    model.getType().render( sb );
    return sb.toString();
  }
}
