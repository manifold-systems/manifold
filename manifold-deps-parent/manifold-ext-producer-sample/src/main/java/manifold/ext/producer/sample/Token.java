package manifold.ext.producer.sample;

import manifold.api.fs.IFile;

class Token
{
  int _pos;
  StringBuilder _value;
  IFile _file;

  Token( int pos, IFile file )
  {
    _value = new StringBuilder();
    _pos = pos;
    _file = file;
  }

  void append( char c )
  {
    _value.append( c );
  }

  public String toString()
  {
    return _value.toString();
  }
}
