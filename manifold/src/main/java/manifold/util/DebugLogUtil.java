package manifold.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class DebugLogUtil
{
  /**
   * Log debug message to specified path/file.  Note this is a last ditch logging effort for use-cases
   * where logging to System.err/out or other means fails.
   */
  public static void log( String path, String msg )
  {
    File file = new File( path );
    try( PrintWriter pw = new PrintWriter( file ) )
    {
      pw.write( msg + "\n" );
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
    File file = new File( path );
    try( PrintWriter pw = new PrintWriter( file ) )
    {
      t.printStackTrace( pw );
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }
}
