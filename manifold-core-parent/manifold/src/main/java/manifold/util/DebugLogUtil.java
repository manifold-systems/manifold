package manifold.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
}
