package manifold.ext.stuff;

// not public
class SecretClass
{
  private final SecretParam _param;

  SecretClass( SecretParam param )
  {
    _param = param;
  }

  SecretParam getParam()
  {
    return _param;
  }
}
