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

import graphql.language.Definition;
import graphql.language.Node;
import graphql.language.OperationDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.TypeDefinition;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import manifold.api.host.IModule;
import manifold.api.type.JavaTypeManifold;

public class GqlManifold extends JavaTypeManifold<GqlModel>
{
  public static final List<String> EXTS = Arrays.asList( "graphql", "graphqls", "gql" );

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

    Definition typeDef = null;
    for( StringTokenizer tokenizer = new StringTokenizer( relativeInner, "." ); tokenizer.hasMoreTokens(); )
    {
      String innerName = tokenizer.nextToken();
      typeDef = typeDef == null ? type.getChild( innerName ) : getChildDefinition( typeDef, innerName );
      if( typeDef == null )
      {
        // special case so Builder classes can have extension methods applied
        return innerName.equals( "Builder" ) && !tokenizer.hasMoreTokens();
      }
    }
    return typeDef != null;
  }

  private Definition getChildDefinition( Definition def, String name )
  {
    for( Node child: def.getNamedChildren().getChildren( name ) )
    {
      if( child instanceof TypeDefinition || child instanceof OperationDefinition )
      {
        return (Definition)child;
      }
    }
    return null;
  }

  TypeDefinition findTypeDefinition( String simpleName )
  {
    return getAllTypeNames().stream()
      .map( fqn -> {
        GqlModel model = getModel( fqn );
        return model == null ? null : model.getTypeDefinition( simpleName );
      } )
      .filter( Objects::nonNull )
      .findFirst().orElse( null );
  }

  ScalarTypeDefinition findScalarTypeDefinition( String simpleName )
  {
    return getAllTypeNames().stream()
      .map( fqn -> {
        GqlModel model = getModel( fqn );
        return model == null ? null : model.getScalarTypeDefinition( simpleName );
      } )
      .filter( Objects::nonNull )
      .findFirst().orElse( null );
  }

  SchemaDefinition findSchemaDefinition()
  {
    return getAllTypeNames().stream()
      .map( fqn -> {
        GqlModel model = getModel( fqn );
        return model == null ? null : model.getSchemaDefinition();
      } )
      .filter( Objects::nonNull )
      .findFirst().orElse( null );
  }

  @Override
  protected String contribute( JavaFileManager.Location location, String topLevelFqn, boolean genStubs, String existing, GqlModel model, DiagnosticListener<JavaFileObject> errorHandler )
  {
    StringBuilder sb = new StringBuilder();
    model.report( errorHandler );
    model.getType().render( sb, location, getModule(), errorHandler );
    return sb.toString();
  }
}
