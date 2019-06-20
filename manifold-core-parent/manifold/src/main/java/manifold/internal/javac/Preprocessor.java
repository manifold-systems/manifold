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

package manifold.internal.javac;

public class Preprocessor
{
  private final ManParserFactory _parserFactory;

  public Preprocessor( ManParserFactory parserFactory )
  {
    _parserFactory = parserFactory;
  }

  public CharSequence process( CharSequence input )
  {
    // note, a preprocessor that needs to tokenize the input can use the Java scanner like this:
    //Scanner scanner = ScannerFactory.instance( JavacPlugin.instance().getContext() ).newScanner( input, true );

    //## todo: provide service interface for preprocessor implementors
    return input;
  }
}