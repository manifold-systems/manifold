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

package manifold.exceptions;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.JCDiagnostic;
import manifold.api.type.ICompilerComponent;
import manifold.internal.javac.JavacPlugin;
import manifold.internal.javac.TypeProcessor;

public class CheckedExceptionSuppressor implements ICompilerComponent
{
  @Override
  public void init( BasicJavacTask javacTask, TypeProcessor typeProcessor )
  {
  }

  @Override
  public boolean isSuppressed( JCDiagnostic.DiagnosticPosition pos, String issueKey, Object[] args )
  {
    return JavacPlugin.instance() != null &&
           issueKey != null &&
           (issueKey.contains( "unreported.exception." ) ||
            issueKey.contains( "incompatible.thrown.types" ));
  }
}
