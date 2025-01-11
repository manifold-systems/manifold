package manifold.api.gen;

public class TypeNameParserException extends RuntimeException
{
  public TypeNameParserException( String message )
  {
    super( message );
  }

  public TypeNameParserException( String message, Throwable cause )
  {
    super( message, cause );
  }

  public TypeNameParserException( Throwable cause )
  {
    super( cause );
  }

  public TypeNameParserException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace )
  {
    super( message, cause, enableSuppression, writableStackTrace );
  }
}
