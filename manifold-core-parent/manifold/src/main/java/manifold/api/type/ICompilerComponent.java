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

package manifold.api.type;

import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.JCDiagnostic;
import manifold.internal.javac.TypeProcessor;

/**
 * A service provider interface (SPI) for general purpose Java compiler manipulation.
 */
public interface ICompilerComponent
{
  /**
   * Initializes this service.
   *
   * @param javacTask The Javac compiler task. Primarily used to call {@link BasicJavacTask#addTaskListener}.
   * @param typeProcessor Manifold's type processor.
   */
  void init( BasicJavacTask javacTask, TypeProcessor typeProcessor );

  /**
   * Override to control the order in which this class' {@link #init} method is called relative to other {@code ICompilerComponent}
   * services. This method is called for each {@code ICompilerComponent}, excluding this one.
   * <p/>
   * Returning {@code Before} for multiple {@code compilerComponent}s maintains the earliest position. Returning {@code After}
   * multiple times maintains the latest position. Returning both {@code Before} and {@code After} maintains the latest
   * {@code After} position.
   *
   * @param compilerComponent A compiler component service from the set of services currently registered.
   * @return Whether this service should initialize before or after {@code compilerComponent}.
   */
  default InitOrder initOrder( ICompilerComponent compilerComponent )
  {
    return InitOrder.NA;
  }

  /**
   * Suppresses the compiler warning/error specified by {@code issueKey}.
   *
   * @param pos
   * @param issueKey The compiler warning/error in question. These are the javac coded message keys such as those
   *                 beginning with "compiler.err.".
   * @param args
   * @return Returns {@code true} if the message should be suppressed.
   */
  default boolean isSuppressed( JCDiagnostic.DiagnosticPosition pos, String issueKey, Object[] args )
  {
    return false;
  }

  /**
   * Called when the {@code JavacPlugin} initializes and whenever the compiler context changes e.g., when annotation
   * processors make rounds. This is where, if need be, you hack into the compiler before compilation starts e.g., to
   * override or replace part of the compiler.
   */
  default void tailorCompiler()
  {
  }

  /** Used with {@link #initOrder(ICompilerComponent)} to control when this service initializes relative to others.*/
  enum InitOrder {Before, After, NA};
}
