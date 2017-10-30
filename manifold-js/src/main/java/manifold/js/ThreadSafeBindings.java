package manifold.js;

import javax.script.Bindings;
import java.util.concurrent.ConcurrentHashMap;

class ThreadSafeBindings extends ConcurrentHashMap<String, Object> implements Bindings {}
