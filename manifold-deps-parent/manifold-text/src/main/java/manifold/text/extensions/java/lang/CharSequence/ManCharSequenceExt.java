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

package manifold.text.extensions.java.lang.CharSequence;

import java.util.Collection;

import manifold.ext.rt.api.Expires;
import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import manifold.ext.rt.api.CharPredicate;
import manifold.rt.api.util.Pair;

/**
 */
@Extension
public class ManCharSequenceExt
{
  /**
   * Returns a sub sequence of this char sequence having leading and trailing characters matching the {@code predicate} trimmed.
   */
  public static CharSequence trim( @This CharSequence thiz, CharPredicate predicate )
  {
    int startIndex = 0;
    int endIndex = thiz.length() - 1;
    boolean startFound = false;

    while( startIndex <= endIndex )
    {
      int index = startFound ? endIndex : startIndex;
      boolean match = predicate.test( thiz.charAt( index ) );

      if( !startFound )
      {
        if( !match )
        {
          startFound = true;
        }
        else
        {
          startIndex += 1;
        }
      }
      else
      {
        if( !match )
        {
          break;
        }
        else
        {
          endIndex -= 1;
        }
      }
    }

    return thiz.subSequence( startIndex, endIndex + 1 );
  }

  /**
   * Returns a sub sequence of this char sequence having leading characters matching the {@code predicate} trimmed.
   */
  public static CharSequence trimStart( @This CharSequence thiz, CharPredicate predicate )
  {
    for( int index = 0; index < thiz.length(); index++ )
    {
      if( !predicate.test( thiz.charAt( index ) ) )
      {
        return thiz.subSequence( index, thiz.length() );
      }
    }

    return "";
  }

  /**
   * Returns a sub sequence of this char sequence having trailing characters matching the {@code predicate} trimmed.
   */
  public static CharSequence trimEnd( @This CharSequence thiz, CharPredicate predicate )
  {
    for( int index = thiz.length() - 1; index >= 0; index-- )
    {
      if( !predicate.test( thiz.charAt( index ) ) )
      {
        return thiz.subSequence( 0, index + 1 );
      }
    }

    return "";
  }

  /**
   * Returns a sub sequence of this char sequence having leading and trailing characters from the {@code chars} array trimmed.
   */
  public static CharSequence trim( @This CharSequence thiz, char... chars )
  {
    return thiz.trim( it ->
                      {
                        for( int i = 0; i < chars.length; i++ )
                        {
                          char c = chars[i];
                          if( c == it )
                          {
                            return true;
                          }
                        }
                        return false;
                      } );
  }

  /**
   * Returns a sub sequence of this char sequence having leading and trailing characters from the {@code chars} array trimmed.
   */
  public static CharSequence trimStart( @This CharSequence thiz, char... chars )
  {
    return thiz.trimStart( it ->
                           {
                             for( int i = 0; i < chars.length; i++ )
                             {
                               char c = chars[i];
                               if( c == it )
                               {
                                 return true;
                               }
                             }
                             return false;
                           } );
  }

  /**
   * Returns a sub sequence of this char sequence having trailing characters from the {@code chars} array trimmed.
   */
  public static CharSequence trimEnd( @This CharSequence thiz, char... chars )
  {
    return thiz.trimEnd( it ->
                         {
                           for( int i = 0; i < chars.length; i++ )
                           {
                             char c = chars[i];
                             if( c == it )
                             {
                               return true;
                             }
                           }
                           return false;
                         } );
  }

  /**
   * Returns a sub sequence of this char sequence having leading whitespace removed.
   */
  public static CharSequence trimStart( @This CharSequence thiz )
  {
    return thiz.trimStart( c -> Character.isWhitespace( c ) );
  }

  /**
   * Returns a sub sequence of this char sequence having trailing whitespace removed.
   */
  public static CharSequence trimEnd( @This CharSequence thiz )
  {
    return thiz.trimEnd( c -> Character.isWhitespace( c ) );
  }

  /**
   * Returns a char sequence with content of this char sequence padded at the beginning
   * to the specified {@code length} with the specified character or space.
   *
   * @param length  the desired string length.
   * @param padChar the character to pad string with, if it has length less than the {@code length} specified.
   *
   * @return Returns a string, of length at least {@code length}, consisting of string prepended with {@code padChar} as many times.
   * as are necessary to reach that length.
   */

  public static CharSequence padStart( @This CharSequence thiz, int length, char padChar )
  {
    if( length < 0 )
    {
      throw new IllegalArgumentException( "Desired length " + length + " is less than zero." );
    }
    if( length <= thiz.length() )
    {
      return thiz.subSequence( 0, thiz.length() );
    }

    StringBuilder sb = new StringBuilder( length );
    for( int i = 1; i <= (length - thiz.length()); i++ )
    {
      sb.append( padChar );
    }
    sb.append( thiz );
    return sb;
  }

  /**
   * Returns a char sequence with content of this char sequence padded at the end
   * to the specified {@code length} with the specified character or space.
   *
   * @param length  the desired string length.
   * @param padChar the character to pad string with, if it has length less than the {@code length} specified.
   *
   * @return Returns a string, of length at least {@code length}, consisting of string prepended with {@code padChar} as many times.
   * as are necessary to reach that length.
   */
  public static CharSequence padEnd( @This CharSequence thiz, int length, char padChar )
  {
    if( length < 0 )
    {
      throw new IllegalArgumentException( "Desired length $length is less than zero." );
    }
    if( length <= thiz.length() )
    {
      return thiz.subSequence( 0, thiz.length() );
    }

    StringBuilder sb = new StringBuilder( length );
    sb.append( thiz );
    for( int i = 1; i <= (length - thiz.length()); i++ )
    {
      sb.append( padChar );
    }
    return sb;
  }

  /**
   * Returns {@code true} if this nullable char sequence is either {@code null} or empty.
   */
  public static boolean isNullOrEmpty( @This CharSequence thiz )
  {
    return thiz == null || thiz.length() == 0;
  }

  /**
   * Returns {@code true} if this char sequence is empty (contains no characters).
   */
  @Expires( 15 )
  public static boolean isEmpty( @This CharSequence thiz )
  {
    return thiz.length() == 0;
  }

  /**
   * Returns {@code true} if this char sequence is not empty.
   */
  public static boolean isNotEmpty( @This CharSequence thiz )
  {
    return thiz.length() > 0;
  }

  /**
   * Returns {@code true} if this char sequence is empty or contains only whitespace characters.
   */
  public static boolean isBlank( @This CharSequence thiz )
  {
    return thiz.length() == 0 || thiz.all( it -> Character.isWhitespace( it ) );
  }

  public static boolean all( @This CharSequence thiz, CharPredicate predicate )
  {
    for( int i = 0; i < thiz.length(); i++ )
    {
      if( !predicate.test( thiz.charAt( i ) ) )
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if this nullable char sequence is either {@code null} or empty or consists solely of whitespace characters.
   */
  public static boolean isNullOrBlank( @This CharSequence thiz )
  {
    return thiz == null || thiz.isBlank();
  }

  /**
   * Returns the index of the last character in the char sequence or -1 if it is empty.
   */
  public static int lastIndex( @This CharSequence thiz )
  {
    return thiz.length() - 1;
  }

  /**
   * Returns a substring of chars from a range of this char sequence starting at the {@code startIndex} and ending right before the {@code endIndex}.
   *
   * @param startIndex the start index (inclusive).
   * @param endIndex   the end index (exclusive). If not specified, the length of the char sequence is used.
   */
  public static String substring( @This CharSequence thiz, int startIndex, int endIndex )
  {
    return thiz.subSequence( startIndex, endIndex ).toString();
  }

  public static String substring( @This CharSequence thiz, int startIndex )
  {
    return thiz.subSequence( startIndex, thiz.length() ).toString();
  }

  /**
   * If this char sequence starts with the given {@code prefix}, returns a new char sequence
   * with the prefix removed. Otherwise, returns a new char sequence with the same characters.
   */
  public static CharSequence removePrefix( @This CharSequence thiz, CharSequence prefix )
  {
    if( thiz.startsWith( prefix ) )
    {
      return thiz.subSequence( prefix.length(), thiz.length() );
    }
    return thiz.subSequence( 0, thiz.length() );
  }

  /**
   * If this char sequence ends with the given {@code suffix}, returns a new char sequence
   * with the suffix removed. Otherwise, returns a new char sequence with the same characters.
   */
  public static CharSequence removeSuffix( @This CharSequence thiz, CharSequence suffix )
  {
    if( thiz.endsWith( suffix ) )
    {
      return thiz.subSequence( 0, thiz.length() - suffix.length() );
    }
    return thiz.subSequence( 0, thiz.length() );
  }

  public static char first( @This CharSequence thiz )
  {
    return thiz.charAt( 0 );
  }

  public static char last( @This CharSequence thiz )
  {
    return thiz.charAt( thiz.lastIndex() );
  }

  /**
   * Returns {@code true} if this char sequence starts with the specified character.
   */
  public static boolean startsWith( @This CharSequence thiz, char c )
  {
    return thiz.length() > 0 && thiz.first() == c;
  }

  public static boolean startsWithIgnoreCase( @This CharSequence thiz, char c )
  {
    return thiz.length() > 0 && (thiz.first() == c || Character.toLowerCase( thiz.first() ) == Character.toLowerCase( c ));
  }

  /**
   * Returns {@code true} if this char sequence ends with the specified character.
   */
  public static boolean endsWith( @This CharSequence thiz, char c )
  {
    return thiz.length() > 0 && thiz.last() == c;
  }

  public static boolean endsWithIgnoreCase( @This CharSequence thiz, char c )
  {
    return thiz.length() > 0 && (thiz.last() == c || Character.toLowerCase( thiz.last() ) == Character.toLowerCase( c ));
  }

  /**
   * Implementation of {@code regionMatches} for CharSequences.
   * Invoked when it's already known that arguments are not Strings, so that no additional type checks are performed.
   */
  private static boolean regionMatchesImpl( CharSequence thiz, int thisOffset, CharSequence other, int otherOffset, int length, boolean ignoreCase )
  {
    if( (otherOffset < 0) || (thisOffset < 0) || (thisOffset > thiz.length() - length)
        || (otherOffset > other.length() - length) )
    {
      return false;
    }

    for( int index = 0; index < length; index++ )
    {
      char thisChar = thiz.charAt( thisOffset + index );
      char otherChar = other.charAt( otherOffset + index );
      if( !(thisChar == otherChar || ignoreCase && Character.toLowerCase( thisChar ) == Character.toLowerCase( otherChar )) )
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if this char sequence starts with the specified prefix.
   */
  public static boolean startsWith( @This CharSequence thiz, CharSequence prefix )
  {
    if( thiz instanceof String && prefix instanceof String )
    {
      return ((String)thiz).startsWith( (String)prefix );
    }
    else
    {
      return regionMatchesImpl( thiz, 0, prefix, 0, prefix.length(), false );
    }
  }

  /**
   * Returns {@code true} if this char sequence starts with the specified prefix.
   */
  public static boolean startsWithIgnoreCase( @This CharSequence thiz, CharSequence prefix )
  {
    return regionMatchesImpl( thiz, 0, prefix, 0, prefix.length(), true );
  }

  /**
   * Returns {@code true} if this char sequence ends with the specified suffix.
   */
  public static boolean endsWith( @This CharSequence thiz, CharSequence suffix )
  {
    if( thiz instanceof String && suffix instanceof String )
    {
      return ((String)thiz).endsWith( (String)suffix );
    }
    else
    {
      return regionMatchesImpl( thiz, thiz.length() - suffix.length(), suffix, 0, suffix.length(), false );
    }
  }

  /**
   * Returns {@code true} if this char sequence ends with the specified prefix.
   */
  public static boolean endsWithIgnoreCase( @This CharSequence thiz, CharSequence suffix )
  {
    return regionMatchesImpl( thiz, thiz.length() - suffix.length(), suffix, 0, suffix.length(), true );
  }


  // indexOfAny()

  private static Pair<Integer, Character> findAnyOf( CharSequence thiz, char[] chars, int startIndex, boolean ignoreCase, boolean backward )
  {
    if( !ignoreCase && chars.length == 1 && thiz instanceof String )
    {
      char ch = chars[0];
      int index = (!backward) ? thiz.toString().indexOf( ch, startIndex ) : thiz.toString().lastIndexOf( ch, startIndex );
      return (index < 0) ? null : new Pair<>( index, ch );
    }

    if( !backward )
    {
      int start = Math.max( startIndex, 0 );
      int end = thiz.lastIndex();

      for( int index = start; index <= end; index++ )
      {
        char charAtIndex = thiz.charAt( index );
        int matchingCharIndex = indexOfFirst( chars, charAtIndex, ignoreCase );
        if( matchingCharIndex >= 0 )
        {
          return new Pair<>( index, chars[matchingCharIndex] );
        }
      }
    }
    else
    {
      int start = Math.min( startIndex, thiz.lastIndex() );

      for( int index = start; index >= 0; index-- )
      {
        char charAtIndex = thiz.charAt( index );
        int matchingCharIndex = indexOfFirst( chars, charAtIndex, ignoreCase );
        if( matchingCharIndex >= 0 )
        {
          return new Pair<>( index, chars[matchingCharIndex] );
        }
      }
    }
    return null;
  }

  private static int indexOfFirst( char[] chars, char ch, boolean ignoreCase )
  {
    for( int i = 0; i < chars.length; i++ )
    {
      char csr = chars[i];
      if( csr == ch || (ignoreCase && Character.toLowerCase( csr ) == Character.toLowerCase( ch )) )
      {
        return i;
      }
    }
    return -1;
  }

  public static int indexOfAny( @This CharSequence thiz, char[] chars )
  {
    return thiz.indexOfAny( chars, 0, false );
  }
  public static int indexOfAny( @This CharSequence thiz, char[] chars, int startIndex )
  {
    return thiz.indexOfAny( chars, startIndex, false );
  }
  /**
   * Finds the index of the first occurrence of any of the specified {@code chars} in this char sequence,
   * starting from the specified {@code startIndex} and optionally ignoring the case.
   *
   * @param ignoreCase {@code true} to ignore character case when matching a character.
   *
   * @return An index of the first occurrence of matched character from {@code chars} or -1 if none of {@code chars} are found.
   */
  public static int indexOfAny( @This CharSequence thiz, char[] chars, int startIndex, boolean ignoreCase )
  {
    Pair<Integer, Character> result = findAnyOf( thiz, chars, startIndex, ignoreCase, false );
    return result == null ? -1 : result.getFirst();
  }

  public static int lastIndexOfAny( @This CharSequence thiz, char[] chars )
  {
    return thiz.lastIndexOfAny( chars, thiz.lastIndex(), false );
  }
  public static int lastIndexOfAny( @This CharSequence thiz, char[] chars, int startIndex )
  {
    return thiz.lastIndexOfAny( chars, startIndex, false );
  }
  /**
   * Finds the index of the last occurrence of any of the specified {@code chars} in this char sequence,
   * starting from the specified {@code startIndex} and optionally ignoring the case.
   *
   * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
   * @param ignoreCase {@code true} to ignore character case when matching a character.
   *
   * @return An index of the last occurrence of matched character from {@code chars} or -1 if none of {@code chars} are found.
   */
  public static int lastIndexOfAny( @This CharSequence thiz, char[] chars, int startIndex, boolean ignoreCase )
  {
    Pair<Integer, Character> result = findAnyOf( thiz, chars, startIndex, ignoreCase, true );
    return result == null ? -1 : result.getFirst();
  }

  private static int indexOf( CharSequence thiz, CharSequence other, int startIndex, int endIndex, boolean ignoreCase )
  {
    return indexOf( thiz, other, startIndex, endIndex, ignoreCase, false );
  }

  private static int indexOf( CharSequence thiz, CharSequence other, int startIndex, int endIndex, boolean ignoreCase, boolean last )
  {
    if( !last )
    {
      int start = Math.max( startIndex, 0 );
      int end = Math.min( endIndex, thiz.length() );

      if( thiz instanceof String && other instanceof String )
      {
        for( int index = start; index <= end; index++ )
        {
          if( ((String)other).regionMatches( ignoreCase, 0, (String)thiz, index, other.length() ) )
          {
            return index;
          }
        }
      }
      else
      {
        for( int index = start; index <= end; index++ )
        {
          if( regionMatchesImpl( other, 0, thiz, index, other.length(), ignoreCase ) )
          {
            return index;
          }
        }
      }
    }
    else
    {
      int start = Math.min( startIndex, thiz.lastIndex() );
      int end = Math.max( endIndex, 0 );

      if( thiz instanceof String && other instanceof String )
      {
        for( int index = start; index >= 0; index-- )
        {
          if( ((String)other).regionMatches( ignoreCase, 0, (String)thiz, index, other.length() ) )
          {
            return index;
          }
        }
      }
      else
      {
        for( int index = start; index >= 0; index-- )
        {
          if( regionMatchesImpl( other, 0, thiz, index, other.length(), ignoreCase ) )
          {
            return index;
          }
        }
      }
    }

    return -1;
  }

  private static Pair<Integer, String> findAnyOf( CharSequence thiz, Collection<String> strings, int startIndex, final boolean ignoreCase, boolean last )
  {
    if( !ignoreCase && strings.size() == 1 )
    {
      String string = strings.single();
      int index = (!last) ? thiz.indexOf( string, startIndex ) : thiz.lastIndexOf( string, startIndex );
      return (index < 0) ? null : new Pair<>( index, string );
    }

    if( !last )
    {
      int start = Math.max( startIndex, 0 );
      int end = thiz.length();

      if( thiz instanceof String )
      {
        for( int index = start; index <= end; index++ )
        {
          int finalIndex = index;
          String matchingString = strings.firstOrNull( it -> it.regionMatches( ignoreCase, 0, (String)thiz, finalIndex, it.length() ) );
          if( matchingString != null )
          {
            return new Pair<>( index, matchingString );
          }
        }
      }
      else
      {
        for( int index = start; index <= end; index++ )
        {
          int finalIndex = index;
          String matchingString = strings.firstOrNull( it -> regionMatchesImpl( it, 0, thiz, finalIndex, it.length(), ignoreCase ) );
          if( matchingString != null )
          {
            return new Pair<>( index, matchingString );
          }
        }
      }
    }
    else
    {
      int start = Math.min( startIndex, thiz.lastIndex() );

      if( thiz instanceof String )
      {
        for( int index = start; index >= 0; index-- )
        {
          int finalIndex = index;
          String matchingString = strings.firstOrNull( it -> it.regionMatches( ignoreCase, 0, (String)thiz, finalIndex, it.length() ) );
          if( matchingString != null )
          {
            return new Pair<>( index, matchingString );
          }
        }
      }
      else
      {
        for( int index = start; index >= 0; index-- )
        {
          int finalIndex = index;
          String matchingString = strings.firstOrNull( it -> regionMatchesImpl( it, 0, thiz, finalIndex, it.length(), ignoreCase ) );
          if( matchingString != null )
          {
            return new Pair<>( index, matchingString );
          }
        }
      }
    }
    return null;
  }

  public static Pair<Integer, String> findAnyOf( @This CharSequence thiz, Collection<String> strings )
  {
    return findAnyOf( thiz, strings, 0, false );
  }
  public static Pair<Integer, String> findAnyOf( @This CharSequence thiz, Collection<String> strings, int startIndex )
  {
    return findAnyOf( thiz, strings, startIndex, false );
  }
  /**
   * Finds the first occurrence of any of the specified {@code strings} in this char sequence,
   * starting from the specified {@code startIndex} and optionally ignoring the case.
   *
   * @param ignoreCase {@code true} to ignore character case when matching a string.
   *
   * @return A pair of an index of the first occurrence of matched string from {@code strings} and the string matched
   * or {@code null} if none of {@code strings} are found.
   * <p>
   * To avoid ambiguous results when strings in {@code strings} have characters in common, this method proceeds from
   * the beginning to the end of this string, and finds at each position the first element in {@code strings}
   * that matches this string at that position.
   */
  public static Pair<Integer, String> findAnyOf( @This CharSequence thiz, Collection<String> strings, int startIndex, boolean ignoreCase )
  {
    return findAnyOf( thiz, strings, startIndex, ignoreCase, false );
  }

  public static Pair<Integer, String> findLastAnyOf( @This CharSequence thiz, Collection<String> strings )
  {
    return findLastAnyOf( thiz, strings, thiz.lastIndex(), false );
  }
  public static Pair<Integer, String> findLastAnyOf( @This CharSequence thiz, Collection<String> strings, int startIndex )
  {
    return findLastAnyOf( thiz, strings, startIndex, false );
  }
  /**
   * Finds the last occurrence of any of the specified {@code strings} in this char sequence,
   * starting from the specified {@code startIndex} and optionally ignoring the case.
   *
   * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
   * @param ignoreCase {@code true} to ignore character case when matching a string.
   *
   * @return A pair of an index of the last occurrence of matched string from {@code strings} and the string matched or {@code null} if none of {@code strings} are found.
   * <p>
   * To avoid ambiguous results when strings in {@code strings} have characters in common, this method proceeds from
   * the end toward the beginning of this string, and finds at each position the first element in {@code strings}
   * that matches this string at that position.
   */
  public static Pair<Integer, String> findLastAnyOf( @This CharSequence thiz, Collection<String> strings, int startIndex, boolean ignoreCase )
  {
    return findAnyOf( thiz, strings, startIndex, ignoreCase, false );
  }

  public static int indexOfAny( @This CharSequence thiz, Collection<String> strings )
  {
    return thiz.indexOfAny( strings, 0, false );
  }
  public static int indexOfAny( @This CharSequence thiz, Collection<String> strings, int startIndex )
  {
    return thiz.indexOfAny( strings, startIndex, false );
  }
  /**
   * Finds the index of the first occurrence of any of the specified {@code strings} in this char sequence,
   * starting from the specified {@code startIndex} and optionally ignoring the case.
   *
   * @param ignoreCase {@code true} to ignore character case when matching a string.
   *
   * @return An index of the first occurrence of matched string from {@code strings} or -1 if none of {@code strings} are found.
   * <p>
   * To avoid ambiguous results when strings in {@code strings} have characters in common, this method proceeds from
   * the beginning to the end of this string, and finds at each position the first element in {@code strings}
   * that matches this string at that position.
   */
  public static int indexOfAny( @This CharSequence thiz, Collection<String> strings, int startIndex, boolean ignoreCase )
  {
    Pair<Integer, String> result = findAnyOf( thiz, strings, startIndex, ignoreCase, false );
    return result == null ? -1 : result.getFirst();
  }

  public static int lastIndexOfAny( @This CharSequence thiz, Collection<String> strings )
  {
    return lastIndexOfAny( thiz, strings, thiz.lastIndex(), false );
  }
  public static int lastIndexOfAny( @This CharSequence thiz, Collection<String> strings, int startIndex )
  {
    return lastIndexOfAny( thiz, strings, startIndex, false );
  }
  /**
   * Finds the index of the last occurrence of any of the specified {@code strings} in this char sequence,
   * starting from the specified {@code startIndex} and optionally ignoring the case.
   *
   * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
   * @param ignoreCase {@code true} to ignore character case when matching a string.
   *
   * @return An index of the last occurrence of matched string from {@code strings} or -1 if none of {@code strings} are found.
   * <p>
   * To avoid ambiguous results when strings in {@code strings} have characters in common, this method proceeds from
   * the end toward the beginning of this string, and finds at each position the first element in {@code strings}
   * that matches this string at that position.
   */
  public static int lastIndexOfAny( @This CharSequence thiz, Collection<String> strings, int startIndex, boolean ignoreCase )
  {
    Pair<Integer, String> result = findAnyOf( thiz, strings, startIndex, ignoreCase, true );
    return result == null ? -1 : result.getFirst();
  }

  // indexOf

  public static int indexOf( @This CharSequence thiz, int ch )
  {
    return thiz.indexOf( ch, 0, false );
  }
  public static int indexOf( @This CharSequence thiz, int ch, boolean ignoreCase )
  {
    return thiz.indexOf( ch, 0, ignoreCase );
  }
  public static int indexOf( @This CharSequence thiz, int ch, int startIndex )
  {
    return thiz.indexOf( ch, startIndex, false );
  }
  /**
   * Returns the index within this string of the first occurrence of the specified character, starting from the specified {@code startIndex}.
   *
   * @param ignoreCase {@code true} to ignore character case when matching a character.
   *
   * @return An index of the first occurrence of {@code char} or -1 if none is found.
   */
  public static int indexOf( @This CharSequence thiz, int ch, int startIndex, boolean ignoreCase )
  {
    return (ignoreCase || !(thiz instanceof String))
           ? indexOfAny( thiz, new char[]{(char)ch}, startIndex, ignoreCase )
           : thiz.toString().indexOf( ch, startIndex );
  }

  public static int indexOf( @This CharSequence thiz, String string )
  {
    return thiz.indexOf( string, 0, false );
  }
  public static int indexOf( @This CharSequence thiz, String string, boolean ignoreCase )
  {
    return thiz.indexOf( string, 0, ignoreCase );
  }
  public static int indexOf( @This CharSequence thiz, String string, int startIndex )
  {
    return thiz.indexOf( string, startIndex, false );
  }
  /**
   * Returns the index within this char sequence of the first occurrence of the specified {@code string},
   * starting from the specified {@code startIndex}.
   *
   * @param ignoreCase {@code true} to ignore character case when matching a string.
   *
   * @return An index of the first occurrence of {@code string} or `-1` if none is found.
   */
  public static int indexOf( @This CharSequence thiz, String string, int startIndex, boolean ignoreCase )
  {
    return (ignoreCase || !(thiz instanceof String))
           ? indexOf( thiz, string, startIndex, thiz.length(), ignoreCase )
           : thiz.toString().indexOf( string, startIndex );
  }

  public static int lastIndexOf( @This CharSequence thiz, int ch )
  {
    return thiz.lastIndexOf( ch, thiz.lastIndex(), false );
  }
  public static int lastIndexOf( @This CharSequence thiz, int ch, boolean ignoreCase )
  {
    return thiz.lastIndexOf( ch, thiz.lastIndex(), ignoreCase );
  }
  public static int lastIndexOf( @This CharSequence thiz, int ch, int startIndex )
  {
    return thiz.lastIndexOf( ch, startIndex, false );
  }
  /**
   * Returns the index within this char sequence of the last occurrence of the specified character,
   * starting from the specified {@code startIndex}.
   *
   * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
   * @param ignoreCase {@code true} to ignore character case when matching a character.
   *
   * @return An index of the first occurrence of {@code char} or -1 if none is found.
   */
  public static int lastIndexOf( @This CharSequence thiz, int ch, int startIndex, boolean ignoreCase )
  {
    return (ignoreCase || !(thiz instanceof String))
           ? thiz.lastIndexOfAny( new char[]{(char)ch}, startIndex, ignoreCase )
           : thiz.toString().lastIndexOf( ch, startIndex );
  }

  public static int lastIndexOf( @This CharSequence thiz, String string )
  {
    return thiz.lastIndexOf( string, thiz.lastIndex(), false );
  }
  public static int lastIndexOf( @This CharSequence thiz, String string, boolean ignoreCase )
  {
    return thiz.lastIndexOf( string, thiz.lastIndex(), ignoreCase );
  }
  public static int lastIndexOf( @This CharSequence thiz, String string, int startIndex )
  {
    return thiz.lastIndexOf( string, startIndex, false );
  }
  /**
   * Returns the index within this char sequence of the last occurrence of the specified {@code string},
   * starting from the specified {@code startIndex}.
   *
   * @param startIndex The index of character to start searching at. The search proceeds backward toward the beginning of the string.
   * @param ignoreCase {@code true} to ignore character case when matching a string.
   *
   * @return An index of the first occurrence of {@code string} or -1 if none is found.
   */
  public static int lastIndexOf( @This CharSequence thiz, String string, int startIndex, boolean ignoreCase )
  {
    return (ignoreCase || !(thiz instanceof String))
           ? indexOf( thiz, string, startIndex, 0, ignoreCase, true )
           : thiz.toString().indexOf( string, startIndex );
  }

  public static boolean contains( @This CharSequence thiz, CharSequence other )
  {
    return thiz.contains( other, false );
  }
  /**
   * Returns {@code true} if this char sequence contains the specified {@code other} sequence of characters as a substring.
   *
   * @param ignoreCase {@code true} to ignore character case when comparing strings.
   */
  public static boolean contains( @This CharSequence thiz, CharSequence other, boolean ignoreCase )
  {
    return other instanceof String
           ? thiz.indexOf( (String)other, ignoreCase ) >= 0
           : indexOf( thiz, other, 0, thiz.length(), ignoreCase ) >= 0;
  }


  public static boolean contains( @This CharSequence thiz, char ch )
  {
    return thiz.contains( ch, false );
  }

  /**
   * Returns {@code true} if this char sequence contains the specified character {@code char}.
   *
   * @param ignoreCase {@code true} to ignore character case when comparing characters.
   */
  public static boolean contains( @This CharSequence thiz, char ch, boolean ignoreCase )
  {
    return thiz.indexOf( ch, 0, ignoreCase ) >= 0;
  }

  /**
   * Implement the <b>index operator</b> to enable indexed access to characters such as {@code text[i]}.
   *
   * @param      index   the index of the {@code char} value.
   * @return     the {@code char} value at the specified index of this string.
   *             The first {@code char} value is at index {@code 0}.
   * @exception  IndexOutOfBoundsException  if the {@code index}
   *             argument is negative or not less than the length of this
   *             string.
   */
  public static char get( @This CharSequence thiz, int index )
  {
    return thiz.charAt( index );
  }
}
