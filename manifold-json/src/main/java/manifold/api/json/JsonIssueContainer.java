package manifold.api.json;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;
import javax.script.ScriptException;
import manifold.api.fs.IFile;
import manifold.api.json.schema.IllegalSchemaTypeName;
import manifold.internal.javac.IIssue;
import manifold.internal.javac.IIssueContainer;
import manifold.util.StreamUtil;

/**
 */
public class JsonIssueContainer implements IIssueContainer
{
  private final IFile _file;
  private final List<IIssue> _issues;

  public JsonIssueContainer()
  {
    _issues = Collections.emptyList();
    _file = null;
  }

  /**
   * Format of errors reported in ScriptException is:
   * <pre>
   * Found Errors:\n
   * [line:column] first error\n
   * [line:column] second error\n
   * ...
   * </pre>
   */
  public JsonIssueContainer( ScriptException cause, IFile file )
  {
    _issues = new ArrayList<>();
    _file = file;

    addIssues( cause );
  }

  public JsonIssueContainer( IFile file )
  {
    _issues = new ArrayList<>();
    _file = file;
  }

  private int findOffset( IFile file, int lineNum, int column )
  {
    try
    {
      int offset = 0;
      String content = StreamUtil.getContent( new InputStreamReader( file.openInputStream() ) );
      for( int i = 1; i < lineNum; i++ )
      {
        if( content.length() > offset )
        {
          offset = content.indexOf( '\n', offset ) + 1;
        }
      }
      return offset + column - 1;
    }
    catch( IOException e )
    {
      throw new RuntimeException( e );
    }
  }

  private int parseNum( String line, char delim )
  {
    String num = "";
    for( int i = 0; i < line.length(); i++ )
    {
      char c = line.charAt( i );
      if( c != delim )
      {
        num += c;
      }
      else
      {
        try
        {
          return Integer.parseInt( num );
        }
        catch( Exception e )
        {
          return -1;
        }
      }
    }
    return -1;
  }

  @Override
  public List<IIssue> getIssues()
  {
    return _issues;
  }

  @Override
  public List<IIssue> getWarnings()
  {
    return Collections.emptyList();
  }

  @Override
  public List<IIssue> getErrors()
  {
    return getIssues();
  }

  public void addIssues( ScriptException cause )
  {
    String message = cause.getMessage();
    for( StringTokenizer tokenizer = new StringTokenizer( message, "\r\n" ); tokenizer.hasMoreTokens(); )
    {
      String line = tokenizer.nextToken();
      if( line.startsWith( "[" ) )
      {
        int lineNum = parseNum( line.substring( 1 ), ':' );
        int column = parseNum( line.substring( line.indexOf( ':' ) + 1 ) + 1, ']' );
        int offset = findOffset( _file, lineNum, column );
        String msg = line.substring( line.indexOf( ']' ) + 1 );
        _issues.add( new JsonIssue( IIssue.Kind.Error, offset, lineNum, column, msg ) );
      }
    }
  }

  public void addIssues( IllegalSchemaTypeName cause )
  {
    String message = cause.getMessage();
    Token token = cause.getToken();
    int lineNum = token.getLineNumber();
    int column = token.getColumn();
    int offset = token.getOffset();
    String msg = "Unknown schema type: " + cause.getTypeName();
    _issues.add( new JsonIssue( IIssue.Kind.Error, offset, lineNum, column, msg ) );
  }

  @Override
  public boolean isEmpty()
  {
    return _issues == null;
  }
}
