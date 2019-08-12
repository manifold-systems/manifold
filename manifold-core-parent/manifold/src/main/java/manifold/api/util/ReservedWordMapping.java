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

package manifold.api.util;

import java.util.HashMap;
import java.util.Map;

/**
 */
public class ReservedWordMapping
{
  private static final String JAVA_KEYWORDS[] = {
    "abstract", "assert", "boolean",
    "break", "byte", "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else", "extends", "false",
    "final", "finally", "float", "for", "goto", "if", "implements",
    "import", "instanceof", "int", "interface", "long", "native",
    "new", "null", "package", "private", "protected", "public",
    "return", "short", "static", "strictfp", "super", "switch",
    "synchronized", "this", "throw", "throws", "transient", "true",
    "try", "void", "volatile", "while"};

  private static final Map<String, String> RESERVED_WORD_TO_IDENTIFIER = new HashMap<>();

  static
  {
    // Capitalize/Uncapitalize reserved keywords to avoid parse errors.
    // Internally we perserve the case of the keys, but in structure types
    // we expose them as alternate versions of the reserved words.

    for( String kw : JAVA_KEYWORDS )
    {
      String identifier;
      char c = kw.charAt( 0 );
      if( Character.isLowerCase( c ) )
      {
        identifier = Character.toUpperCase( c ) + kw.substring( 1 );
      }
      else
      {
        identifier = Character.toLowerCase( c ) + kw.substring( 1 );
      }
      RESERVED_WORD_TO_IDENTIFIER.put( kw, identifier );
    }
  }

  public static String getIdentifierForName( String key )
  {
    return RESERVED_WORD_TO_IDENTIFIER.getOrDefault( key, key );
  }
}
