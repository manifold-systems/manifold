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

package manifold.util;

/**
 * Logger-independent logging system.
 */
public interface ILogger
{
  String getName();

  void debug( Object o );

  void debug( Object o, Throwable throwable );

  boolean isDebugEnabled();

  void trace( Object o );

  void trace( Object o, Throwable throwable );

  boolean isTraceEnabled();

  void info( Object o );

  void info( Object o, Throwable throwable );

  boolean isInfoEnabled();

  void warn( Object o );

  void warn( Object o, Throwable throwable );

  void error( Object o );

  void error( Object o, Throwable throwable );

  void fatal( Object o );

  void fatal( Object o, Throwable throwable );
}
