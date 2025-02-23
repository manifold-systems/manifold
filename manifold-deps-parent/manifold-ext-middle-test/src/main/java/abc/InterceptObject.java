package abc;

public class InterceptObject
{

  private final String text;

  public InterceptObject( String text )
  {
    this.text = text;
  }

  public static String sayHello( String name )
  {
    return "hello " + name;
  }

  public String repeatSelf( int times )
  {
    StringBuilder stringBuilder = new StringBuilder();
    for( int i = 0; i < times; i++ )
    {
      stringBuilder.append( text );
    }
    return stringBuilder.toString();
  }

  public String foo( )
  {
    return "foo";
  }
}
