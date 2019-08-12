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

package manifold.ext.producer.sample;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.api.util.JavacDiagnostic;
import manifold.api.util.StreamUtil;

class FavsParser
{
  private static final FavsParser INSTANCE = new FavsParser();

  static FavsParser instance()
  {
    return INSTANCE;
  }

  Map<Token, Token> parseFavsForType( Set<IFile> files, String extensionClassFqn, DiagnosticListener<JavaFileObject> errorHandler )
  {
    // Using LinkedHashMap to preserve insertion order, an impl detail currently required by the IJ plugin for rename
    // refactoring i.e., renaming a json property should result in a source file that differs only in the naming
    // difference -- there should be no difference in ordering of methods etc.
    Map<Token, Token> mapFavToValue = new LinkedHashMap<>();

    for( IFile file : files )
    {
      Objects.requireNonNull( file );
      List<List<Token>> rows = tokenize( file );
      Token token = null;
      for( List<Token> line : rows )
      {
        Iterator<Token> tokens = line.iterator();
        if( !tokens.hasNext() )
        {
          errorHandler.report( new JavacDiagnostic( makeJavaObjectFile( file ), Diagnostic.Kind.ERROR, token == null ? 0 : token._pos, 0, 0, "Expecting a qualified type name" ) );
          continue;
        }
        Token fqn = token = tokens.next();
        String extensionFqn = Model.makeExtensionClassName( fqn.toString() );
        if( extensionFqn.equals( extensionClassFqn ) )
        {
          if( !tokens.hasNext() )
          {
            errorHandler.report( new JavacDiagnostic( makeJavaObjectFile( file ), Diagnostic.Kind.ERROR, fqn._pos + fqn._value.length()-1, 0, 0, "Expecting a 'favorite' name" ) );
            continue;
          }
          Token fav = token = tokens.next();

          if( !tokens.hasNext() )
          {
            errorHandler.report( new JavacDiagnostic( makeJavaObjectFile( file ), Diagnostic.Kind.ERROR, fav._pos + fav._value.length()-1, 0, 0, "Expecting a value for '" + fav._value + "'" ) );
            continue;
          }
          Token value = token = tokens.next();

          mapFavToValue.put( fav, value );
        }
      }
    }
    return mapFavToValue;
  }

  private List<List<Token>> tokenize( IFile file )
  {
    String content;
    try
    {
      content = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }

    List<List<Token>> rows = new ArrayList<>();
    List<Token> row = null;
    Token token = null;
    for( int pos = 0; pos <= content.length(); pos++ )
    {
      char c = pos == content.length() ? 0 : content.charAt( pos );
      if( c == '|' ||
          c == '\n' ||
          c == 0 )
      {
        if( token == null )
        {
          // eof
          break;
        }

        row = row == null ? new ArrayList<>() : row;
        row.add( token );
        token = null;

        if( c == '\n' ||
            c == 0 )
        {
          rows.add( row );
          row = null;
        }
      }
      else if( c != '\r' )
      {
        token = token == null ? new Token( pos, file ) : token;
        token.append( c );
      }
    }

    return rows;
  }

  private JavaFileObject makeJavaObjectFile( IFile file )
  {
    JavaFileObject jfo;
    try
    {
      jfo = new SourceJavaFileObject( file.toURI() );
    }
    catch( Exception e )
    {
      jfo = null;
    }
    return jfo;
  }
}
