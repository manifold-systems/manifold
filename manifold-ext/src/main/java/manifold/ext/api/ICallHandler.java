package manifold.ext.api;

/**
 * Facilitates "self" proxying via Manifold interface extension.
 * <p/>
 * Unlike a conventional proxy, a self proxy does not lose its identity.
 * Essentially via Menifold interface extensions a type can proxy itself
 * via ICallHandler.
 */
public interface ICallHandler
{
  /**
   * Dispatch a call to an extension interface method.
   *
   * @param iface The extended interface and owner of the method
   * @param name The name of the method
   * @param returnType The return type of the method
   * @param paramTypes The parameter types of the method
   * @param args The arguments from the call site
   * @return The result of the method call.  Null if the method's return type is void.
   */
  Object call( Class iface, String name, Class returnType, Class[] paramTypes, Object[] args );
}
