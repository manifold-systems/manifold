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

package manifold.internal.javac;

import java.util.Locale;
import javax.tools.Diagnostic;

/**
 */
public class JavaIssue implements IIssue
{
  private final Diagnostic _diagnostic;

  public JavaIssue( Diagnostic diagnostic )
  {
    _diagnostic = diagnostic;
  }

  @Override
  public Kind getKind()
  {
    return _diagnostic.getKind() == Diagnostic.Kind.ERROR
           ? Kind.Error
           : Kind.Warning;
  }

  @Override
  public int getStartOffset()
  {
    return (int)_diagnostic.getStartPosition();
  }

  @Override
  public int getEndOffset()
  {
    return (int)_diagnostic.getEndPosition();
  }

  @Override
  public int getLine()
  {
    return (int)_diagnostic.getLineNumber();
  }

  @Override
  public int getColumn()
  {
    return (int)_diagnostic.getColumnNumber();
  }

  @Override
  public String getMessage()
  {
    return _diagnostic.getMessage( Locale.getDefault() );
  }
}
