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
import graphql.language.*;
import graphql.parser.InvalidSyntaxException;
import graphql.parser.Parser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.*;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

import graphql.parser.ParserEnvironment;
import graphql.parser.ParserOptions;
import graphql.schema.idl.TypeDefinitionRegistry;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
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
  private GqlScope _scope;
  private GqlParentType _type;
  private TypeDefinitionRegistry _typeRegistry;
  private Map<String, OperationDefinition> _operations;
  private Map<String, FragmentDefinition> _fragments;
  private GqlIssueContainer _issues;

  @SuppressWarnings("WeakerAccess")
  public GqlModel( GqlManifold gqlManifold, String fqn, Set<IFile> files )
  {
    super( gqlManifold.getModule().getHost(), fqn, files );
    _gqlManifold = gqlManifold;
    init();
  }

  private void init()
  {
    _issues = null;
    _fragments = Collections.emptyMap();
    _operations = Collections.emptyMap();
    _scope = assignScope();
    parse();
    _type = new GqlParentType( this );
  }

  private GqlScope assignScope()
  {
    GqlScope scope = _gqlManifold.getScopeFinder().findScope( getFile() );
    if( scope == null )
    {
      scope = GqlScope.makeErrantScope( _gqlManifold, getFqn(), getFile() );
    }
    return scope;
  }

  GqlScope getScope()
  {
    return _scope;
  }

  public TypeDefinitionRegistry getTypeRegistry()
  {
    return _typeRegistry;
  }

  public Map<String, OperationDefinition> getOperations()
  {
    return _operations;
  }

  public Map<String, FragmentDefinition> getFragments()
  {
    return _fragments;
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
    Document document = Parser.parse( ParserEnvironment.newParserEnvironment()
      .parserOptions( ParserOptions.newParserOptions()
        .maxTokens( Integer.MAX_VALUE )
        .maxWhitespaceTokens( Integer.MAX_VALUE )
        .captureSourceLocation( true )
        .build() )
      .document( schemaInput )
      .build() );
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
        getScope().setSchemaDefinition( (SchemaDefinition)definition );
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
      validateDefinition( definition, errors );
    }
    _issues = new GqlIssueContainer( errors, getFile() );
    _typeRegistry = typeRegistry;
    _operations = operations;
    _fragments = fragments;
  }

  //
  // Checks for errors not handled by the GraphQL parser e.g., duplicate fields
  //
  private void validateDefinition( Definition definition, List<GraphQLError> errors )
  {
    Set<String> names = new HashSet<>();
    for( NamedNode child: getDefinitions( definition ) )
    {
      String name = child.getName();
      if( names.contains( name ) )
      {
        GraphQLError error = new InvalidSyntaxError( child.getSourceLocation(),
          "Duplicate definition: " + child.getName() );
        errors.add( error );
      }
      else
      {
        names.add( name );
      }
    }
  }

  private Iterable<? extends NamedNode> getDefinitions( Definition def )
  {
    if( def instanceof OperationDefinition )
    {
      return ((OperationDefinition)def).getVariableDefinitions();
    }
    if( def instanceof InputObjectTypeDefinition )
    {
      return ((InputObjectTypeDefinition)def).getInputValueDefinitions();
    }
    if( def instanceof ObjectTypeDefinition )
    {
      return ((ObjectTypeDefinition)def).getFieldDefinitions();
    }
    if( def instanceof EnumTypeDefinition )
    {
      return ((EnumTypeDefinition)def).getEnumValueDefinitions();
    }
    if( def instanceof InterfaceTypeDefinition )
    {
      return ((InterfaceTypeDefinition)def).getFieldDefinitions();
    }
    return Collections.emptyList();
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

  void addIssue( GraphQLError issue )
  {
    _issues.addIssues( Collections.singletonList( issue ) );
  }

  void report( DiagnosticListener<JavaFileObject> errorHandler )
  {
    if( errorHandler == null )
    {
      return;
    }

    List<IIssue> scopeIssues = getScope().getIssues();
    if( !scopeIssues.isEmpty() )
    {
      IFile scopeConfigFile = getScope().getConfigFile();
      JavaFileObject configFile = scopeConfigFile == null ? null : new SourceJavaFileObject( scopeConfigFile.toURI() );
      for( IIssue scopeIssue : scopeIssues )
      {
        Diagnostic.Kind kind = scopeIssue.getKind() == IIssue.Kind.Error ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
        errorHandler.report( new JavacDiagnostic( configFile, kind, scopeIssue.getStartOffset(), scopeIssue.getLine(), scopeIssue.getColumn(), scopeIssue.getMessage() ) );
      }
    }

    List<IIssue> issues = getIssues();
    if( !issues.isEmpty() )
    {
      JavaFileObject file = new SourceJavaFileObject( getFile().toURI() );
      for( IIssue issue : issues )
      {
        int offset = issue.getStartOffset();
        if( getFile() instanceof IFileFragment )
        {
          offset += ((IFileFragment)getFile()).getOffset();
        }
        Diagnostic.Kind kind = issue.getKind() == IIssue.Kind.Error ? Diagnostic.Kind.ERROR : Diagnostic.Kind.WARNING;
        errorHandler.report( new JavacDiagnostic( file, kind, offset, issue.getLine(), issue.getColumn(), issue.getMessage() ) );
      }
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