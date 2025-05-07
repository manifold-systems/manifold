package manifold.json.rt.api;

import manifold.ext.rt.RuntimeMethods;
import manifold.json.rt.Json;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;

public class Serializer implements Externalizable
{
  private String _interface;
  private String _json;

  // no-arg ctor necessary for Serializable
  public Serializer()
  {
  }

  // called from generated code
  @SuppressWarnings("unused")
  public Serializer( IJsonBindingsBacked jsonBindingsBacked )
  {
    Class<?>[] interfaces = jsonBindingsBacked.getClass().getInterfaces();
    assert interfaces.length == 1;
    _interface = interfaces[0].getTypeName();
    _json = jsonBindingsBacked.write().toJson();
  }

  @Override
  public void writeExternal( ObjectOutput out ) throws IOException
  {
    out.writeObject( _interface );
    out.writeObject( _json );
  }

  @Override
  public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException
  {
    _interface = (String)in.readObject();
    _json = (String)in.readObject();
  }

  Object readResolve() throws ObjectStreamException
  {
    try
    {
      Class<?> iface = Class.forName( _interface );
      return RuntimeMethods.coerceFromBindingsValue( Json.fromJson( _json ), iface );
    }
    catch( ClassNotFoundException e )
    {
      throw new RuntimeException( e );
    }
  }
}
