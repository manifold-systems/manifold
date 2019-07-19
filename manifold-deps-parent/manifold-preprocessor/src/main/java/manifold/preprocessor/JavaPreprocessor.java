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

package manifold.preprocessor;

import java.net.URI;
import java.util.function.Consumer;
import manifold.api.fs.IFile;
import manifold.api.type.IPreprocessor;
import manifold.internal.javac.JavacPlugin;
import manifold.preprocessor.definitions.Definitions;
import manifold.preprocessor.statement.FileStatement;

public class JavaPreprocessor implements IPreprocessor
{
  @Override
  public Order getPreferredOrder()
  {
    return Order.First;
  }

  @Override
  public CharSequence process( URI sourceFile, CharSequence source )
  {
    return process( sourceFile, source, null );
  }
  public CharSequence process( URI sourceFile, CharSequence source, Consumer<Tokenizer> consumer )
  {
    FileStatement fileStmt = new PreprocessorParser( source, consumer ).parseFile();
    if( fileStmt.hasPreprocessorDirectives() )
    {
      StringBuilder result = new StringBuilder();
      try
      {
        IFile file = JavacPlugin.instance().getHost().getFileSystem().getIFile( sourceFile.toURL() );
        fileStmt.execute( result, source, true, new Definitions( file ) );
        return result;
      }
      catch( Exception e )
      {
        throw new IllegalStateException( e );
      }
    }
    return source;
  }
}
