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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

public class DebugLogUtil
{
  /**
   * Log debug message to specified path/file.  Note this is a last ditch logging effort for use-cases
   * where logging to System.err/out or other means fails.
   */
  public static void log( String path, String msg )
  {
    log( path, msg, false );
  }
  public static void log( String path, String msg, boolean append )
  {
    try( PrintWriter pw = new PrintWriter( new BufferedWriter( new FileWriter( path, append ) ) ) )
    {
      pw.write( LocalDateTime.now() + ": " + msg + "\n" );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  /**
   * Log exception stack trace to specified path/file.  Note this is a last ditch logging effort for use-cases
   * where logging to System.err/out or other means fails.
   */
  public static void log( String path, Throwable t )
  {
    log( path, t, false );
  }
  public static void log( String path, Throwable t, boolean append )
  {
    try( PrintWriter pw = new PrintWriter( new BufferedWriter( new FileWriter( path, append ) ) ) )
    {
      pw.write( LocalDateTime.now() + "\n" );
      t.printStackTrace( pw );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  public static String getStackTrace( Throwable e )
  {
    StringWriter sw = new StringWriter();
    e.printStackTrace( new PrintWriter( sw ) );
    return sw.toString();
  }
}
