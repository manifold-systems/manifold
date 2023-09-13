/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.js;

import java.net.MalformedURLException;
import java.util.Objects;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import manifold.api.fs.IFile;
import manifold.api.fs.IFileFragment;
import manifold.api.gen.SrcClass;
import manifold.api.type.ResourceFileTypeManifold;
import manifold.internal.javac.SourceJavaFileObject;
import manifold.js.rt.parser.Parser;
import manifold.js.rt.parser.TemplateParser;
import manifold.js.rt.parser.TemplateTokenizer;
import manifold.js.rt.parser.Token;
import manifold.js.rt.parser.Tokenizer;
import manifold.js.rt.parser.tree.ParseError;
import manifold.js.rt.parser.tree.ProgramNode;
import manifold.js.rt.parser.tree.template.JSTNode;
import manifold.api.util.JavacDiagnostic;


class JavascriptCodeGen
{
  private final JavascriptModel _model;
  private final IFile _file;
  private final String _fqn;

  JavascriptCodeGen( JavascriptModel model, String topLevelFqn )
  {
    _model = model;
    _file = model.getFiles().iterator().next();
    _fqn = topLevelFqn;
  }

  SrcClass make( DiagnosticListener<JavaFileObject> errorHandler )
  {
    String url;
    try
    {
      url = _file.toURI().toURL().toString();
    }
    catch( MalformedURLException e )
    {
      throw new RuntimeException( e );
    }

    String content = ResourceFileTypeManifold.getContent( _file );

    if( Objects.equals( _file.getExtension(), "jst" ) )
    {
      TemplateParser parser = new TemplateParser( new TemplateTokenizer( _fqn, content, url, true ) );
      return JavascriptTemplate.genClass( _model, _fqn, (JSTNode)parser.parse() );
    }
    else
    {
      Parser parser = new Parser( new Tokenizer( content, url ) );
      ProgramNode programNode = (ProgramNode)parser.parse();

      reportErrors( errorHandler, programNode );

      if( parser.isES6Class() )
      {
        return JavascriptClass.genClass( _fqn, _model, programNode, _file );
      }
      else
      {
        return JavascriptProgram.genProgram( _fqn, programNode, _file );
      }
    }
  }

  private void reportErrors( DiagnosticListener<JavaFileObject> errorHandler, ProgramNode programNode )
  {
    if( programNode.errorCount() > 0 )
    {
      JavaFileObject file;
      try
      {
        file = new SourceJavaFileObject( _file.toURI() );
      }
      catch( Exception e )
      {
        file = null;
      }

      for( ParseError error : programNode.getErrorList() )
      {
        Token token = error.getToken();
        int offset = token.getOffset();
        if( _file instanceof IFileFragment )
        {
          offset += ((IFileFragment)_file).getOffset();
        }
        errorHandler.report( new JavacDiagnostic( file, Diagnostic.Kind.ERROR, offset, token.getLineNumber(), token.getCol(), error.getMessage() ) );
      }
    }
  }

}
