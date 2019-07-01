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

import com.sun.tools.javac.parser.Tokens;

public enum HostKind
{
  LINE_COMMENT, BLOCK_COMMENT, JAVADOC_COMMENT, DOUBLE_QUOTE_LITERAL, BACKTICK_LITERAL;

  static HostKind from( Tokens.Comment.CommentStyle s )
  {
    switch( s )
    {
      case LINE:
        return LINE_COMMENT;
      case BLOCK:
        return BLOCK_COMMENT;
      case JAVADOC:
        return JAVADOC_COMMENT;
    }
    throw new IllegalStateException();
  }
}
