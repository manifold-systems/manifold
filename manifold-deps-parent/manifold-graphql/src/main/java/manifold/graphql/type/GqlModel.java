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

import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.InvalidSyntaxError;
import graphql.language.Definition;
import graphql.language.Document;
import graphql.language.FragmentDefinition;
import graphql.language.OperationDefinition;
import graphql.language.SDLDefinition;
import graphql.language.ScalarTypeDefinition;
import graphql.language.SchemaDefinition;
import graphql.language.SourceLocation;
import graphql.language.TypeDefinition;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.type.AbstractSingleFileModel;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.api.util.JavacDiagnostic;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.misc.ParseCancellationException;


import static java.nio.charset.StandardCharsets.UTF_8;

public class GqlModel extends AbstractSingleFileModel
{
  private final GqlManifold _gqlManifold;
  private GqlParentType _type;
  private SchemaDefinition _schemaDefinition;
  private TypeDefinitionRegistry _typeRegistry;
  private Map<String, OperationDefinition> _operations;
  private Map<String, FragmentDefinition> _fragments;
  private GqlIssueContainer _issues;

  @SuppressWarnings("WeakerAccess")
  public GqlModel( GqlManifold gqlManifold, String fqn, Set<IFile> files )
  {
    super( gqlManifold.getModule().getHost(), fqn, files );
    _gqlManifold = gqlManifold;
    _issues = null;
    _fragments = Collections.emptyMap();
    _operations = Collections.emptyMap();
    init();
  }

  private void init()
  {
    parse();
    _type = new GqlParentType( getFqn(), _schemaDefinition, _typeRegistry,
      _operations, _fragments, getFile(), _gqlManifold );
  }

  private void parse()
  {
    try( InputStream stream = getFile().openInputStream() )
    {
      parse( new InputStreamReader( stream, UTF_8 )  );
    }
    catch( InvalidSyntaxException ise )
    {
      _issues = new GqlIssueContainer( Collections.singletonList( toGraphQLError( ise ) ), getFile() );
      _typeRegistry = new TypeDefinitionRegistry();
    }
    catch( ParseCancellationException pce )
    {
      handleParseException( pce );
      _typeRegistry = new TypeDefinitionRegistry();
    }
    catch( IOException ioe )
    {
      throw new RuntimeException( ioe );
    }
  }

  private void parse( Reader schemaInput ) throws ParseCancellationException
  {
    Parser parser = new Parser();
    Document document = parser.parseDocument( schemaInput );
    buildRegistry( document );
  }

  private void buildRegistry( Document document )
  {
    TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();
    List<Definition> definitions = document.getDefinitions();
    Map<String, OperationDefinition> operations = new LinkedHashMap<>();
    Map<String, FragmentDefinition> fragments = new LinkedHashMap<>();
    List<GraphQLError> errors = new ArrayList<>();
    for( Definition definition: definitions )
    {
      if( definition instanceof SchemaDefinition )
      {
        _schemaDefinition = (SchemaDefinition)definition;
      }
      else if( definition instanceof SDLDefinition )
      {
        // types, interfaces, unions, inputs, scalars, extensions
        typeRegistry.add( (SDLDefinition)definition ).ifPresent( errors::add );
        if( definition instanceof ScalarTypeDefinition )
        {
          // register scalar type
          typeRegistry.scalars().put( ((ScalarTypeDefinition)definition).getName(), (ScalarTypeDefinition)definition );
        }
      }
      else if( definition instanceof OperationDefinition )
      {
        // queries, mutations, subscriptions
        operations.put( ((OperationDefinition)definition).getName(), (OperationDefinition)definition );
      }
      else if( definition instanceof FragmentDefinition )
      {
        // fragments
        fragments.put( ((FragmentDefinition)definition).getName(), (FragmentDefinition)definition );
      }
    }
    _issues = new GqlIssueContainer( errors, getFile() );
    _typeRegistry = typeRegistry;
    _operations = operations;
    _fragments = fragments;
  }

  private void handleParseException( ParseCancellationException e ) throws RuntimeException
  {
    _issues = new GqlIssueContainer( Collections.singletonList( toInvalidSyntaxError( e ) ), getFile() );
  }

  private GraphQLError toGraphQLError( InvalidSyntaxException ise )
  {
    return new GraphQLError()
    {
      @Override
      public String getMessage()
      {
        return ise.getMessage();
      }

      @Override
      public List<SourceLocation> getLocations()
      {
        return Collections.singletonList( ise.getLocation() );
      }

      @Override
      public ErrorClassification getErrorType()
      {
        return ErrorType.InvalidSyntax;
      }
    };
  }

  private static InvalidSyntaxError toInvalidSyntaxError( Exception parseException )
  {
    String msg = parseException.getMessage();
    SourceLocation sourceLocation = null;
    if( parseException.getCause() instanceof RecognitionException )
    {
      RecognitionException recognitionException = (RecognitionException)parseException.getCause();
      msg = recognitionException.getMessage();
      sourceLocation = new SourceLocation( recognitionException.getOffendingToken().getLine(), recognitionException.getOffendingToken().getCharPositionInLine() );
    }
    return new InvalidSyntaxError( sourceLocation, msg );
  }

  GqlParentType getType()
  {
    return _type;
  }

  @Override
  public void updateFile( IFile file )
  {
    super.updateFile( file );
    init();
  }

  TypeDefinition getTypeDefinition( String simpleName )
  {
    return _typeRegistry.getType( simpleName ).orElse( null );
  }

  ScalarTypeDefinition getScalarTypeDefinition( String simpleName )
  {
    return _typeRegistry.scalars().get( simpleName );
  }

  SchemaDefinition getSchemaDefinition()
  {
    return _schemaDefinition;
  }

  void report( DiagnosticListener<JavaFileObject> errorHandler )
  {
    if( errorHandler == null )
    {
      return;
    }

    List<IIssue> issues = getIssues();
    if( issues.isEmpty() )
    {
      return;
    }

    JavaFileObject file = new SourceJavaFileObject( getFile().toURI() );
    for( IIssue issue: issues )
    {
      Diagnostic.Kind kind = issue.getKind() == IIssue.Kind.Error ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
      errorHandler.report( new JavacDiagnostic( file, kind, issue.getStartOffset(), issue.getLine(), issue.getColumn(), issue.getMessage() ) );
    }
  }

  private List<IIssue> getIssues()
  {
    List<IIssue> allIssues = new ArrayList<>();
    if( _issues != null )
    {
      allIssues.addAll( _issues.getIssues() );
    }
    return allIssues;
  }
}