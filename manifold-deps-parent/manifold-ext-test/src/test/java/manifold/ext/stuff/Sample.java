package manifold.ext.stuff;

import java.util.HashMap;

public class Sample
{
  private boolean _booleanField;
  private char _charField;
  private byte _byteField;
  private short _shortField;
  private int _intField;
  private long _longField;
  private float _floatField;
  private double _doubleField;
  private String _stringField;

  private String classParam( String param )
  {
    return param;
  }

  private int primParam( int param )
  {
    return param;
  }

  private HashMap.Entry<String, String> innerClassParam( HashMap.Entry<String, String> param )
  {
    return param;
  }

  private int[][] primArrayParam( int[][] param )
  {
    return param;
  }

  private String[][] classArrayParam( String[][] param )
  {
    return param;
  }
}
