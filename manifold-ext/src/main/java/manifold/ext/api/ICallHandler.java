package manifold.ext.api;

/**
 * Facilitates "self" proxying via Manifold interface extension.
 * <p/>
 * Unlike a conventional proxy, a self proxy automatically applies to all
 * instances of the wrapped type without a "wrapper" object, and thus does
 * not suffer from lost type identity.  Essentially via Manifold interface
 * extensions a type can proxy itself using ICallHandler.
 *
 * @see extensions.java.util.Map.MapStructExt
 */
public interface ICallHandler
{
  /**
   * A value resulting from #call() indicating the call could not be dispatched dispatched.
   */
  Object UNHANDLED = new Object()
  {
    @Override
    public String toString()
    {
      return "Unhandled";
    }
  };

  /**
   * Dispatch a call to an extension interface method.
   *
   * @param iface The extended interface and owner of the method
   * @param name The name of the method
   * @param returnType The return type of the method
   * @param paramTypes The parameter types of the method
   * @param args The arguments from the call site
   * @return The result of the method call or UNHANDLED if the method is not dispatched.  Null if the method's return type is void.
   */
  Object call( Class iface, String name, Class returnType, Class[] paramTypes, Object[] args );
}
