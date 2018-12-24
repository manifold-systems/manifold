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

import java.util.ArrayList;
import java.util.List;

/**
 */
public class IssueMsg
{
  public static final IssueMsg MSG_COULD_NOT_FIND_TYPE_FOR_FILE = new IssueMsg( "Could not find type for file: {0}" );

  private final String _msg;

  public IssueMsg( String msg )
  {                              
    _msg = msg;
  }

  public String get( Object... args )
  {
    String msg = _msg;
    
    for( int i = 0; i < args.length; i++ )
    {
      Object arg = args[i];
      msg = msg.replace( "{"+i+"}", arg.toString() );
    }
    return msg;
  }

  public boolean isMessageSimilar( String message )
  {
    String raw = get(); // raw message includes {i} params
    List<String> parts = new ArrayList<>();
    int iLast = 0;
    int iClose = 0;
    while( iClose < raw.length() )
    {
      int iOpen = raw.indexOf( '{', iClose );
      if( iOpen < 0 )
      {
        parts.add( raw.substring( iLast ) );
        break;
      }
      iClose = raw.indexOf( '}', iOpen );
      if( iClose < 0 )
      {
        parts.add( raw.substring( iLast ) );
        break;
      }
      if( isInt( raw.substring( iOpen+1, iClose ) ) )
      {
        parts.add( raw.substring( iLast, iOpen ) );
        iLast = iClose+1;
      }
    }

    int i = 0;
    for( String part: parts )
    {
      if( message.indexOf( part, i ) < 0 )
      {
        return false;
      }
      i += part.length();
    }
    return true;
  }

  private boolean isInt( String str )
  {
    try
    {
      //noinspection ResultOfMethodCallIgnored
      Integer.parseInt( str );
      return true;
    }
    catch( Exception e )
    {
      return false;
    }
  }
}
