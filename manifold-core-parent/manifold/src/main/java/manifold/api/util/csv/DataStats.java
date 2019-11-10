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

package manifold.api.util.csv;

/**
 * Determines the likelihood that a header exists by comparing the mix of characters used in a potential header field
 * vs. a data field. Profiles a field by percentage of alpha, digit, white, and other characters used as well as the
 * length of the text, all having equal weights. Then, comparing profiles, if data fields are significantly different
 * from corresponding header fields, it is likely the header exists.
 * <p/>
 * <i>DISCLAIMER:</i><p/>
 * Note the algorithm using this analysis amounts to a best guess based on statistical makeup of raw data.  Thus, there
 * is no guarantee the algorithm is suitable for a given use-case.
 */
class DataStats
{
  /**
   * The lower bound percentage to which a data value must be similar to a header value. Data values determined to be
   * "similar" (via {@link #isSimilar(CsvToken)}) in raw makeup to the header imply it is likely there is no header.
   */
  private static final int THRESHOLD_PERCENTAGE = 75;

  private int _alpha;
  private int _digit;
  private int _white;
  private int _other;
  private int _total;

  DataStats( CsvToken token )
  {
    String data = token.getData();
    _total = data.length();
    if( _total == 0 )
    {
      return;
    }
    for( int i = 0; i < _total; i++ )
    {
      char c = data.charAt( i );
      if( Character.isAlphabetic( c ) )
      {
        _alpha++;
      }
      else if( Character.isDigit( c ) )
      {
        _digit++;
      }
      else if( Character.isWhitespace( c ) )
      {
        _white++;
      }
      else
      {
        _other++;
      }
    }
    _alpha = _alpha*100 / _total;
    _digit = _digit*100 / _total;
    _white = _white*100 / _total;
    _other = _other*100 / _total;
  }

  boolean isSimilar( CsvToken token )
  {
    DataStats data = new DataStats( token );
    return isSimilar( _alpha, data._alpha ) &&
           isSimilar( _digit, data._digit ) &&
           isSimilar( _white, data._white ) &&
           isSimilar( _other, data._other ) &&
           isSimilar( _total, data._total );
  }

  private boolean isSimilar( int d1, int d2 )
  {
    if( d1 == d2 )
    {
      return true;
    }
    if( d1 == 0 || d2 == 0 )
    {
      return false;
    }
    int max = (d1 > d2) ? d1 : d2;
    int min = (d1 < d2) ? d1 : d2;
    return 100 - (max - min)*100 / max >= THRESHOLD_PERCENTAGE;
  }
}
