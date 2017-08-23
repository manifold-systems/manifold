package manifold.templates;

import manifold.templates.runtime.ILayout;

import java.util.HashMap;

/**
 * Created by eim on 7/17/2017.
 * For configuration purposes
 */
public class ManifoldTemplates {
    private static HashMap<String, ILayout> DEFAULT_TEMPLATE_MAP;
    private static TraceCallback TRACER = (c, t) -> {}; // NO-OP tracer by default

    static {
        DEFAULT_TEMPLATE_MAP = new HashMap<>();
        DEFAULT_TEMPLATE_MAP.put("", ILayout.EMPTY);
    }

    public static void resetDefaultTemplates() {
        DEFAULT_TEMPLATE_MAP = new HashMap<>();
        DEFAULT_TEMPLATE_MAP.put("", ILayout.EMPTY);
    }
    public static void setDefaultTemplate(ILayout layout) {
        DEFAULT_TEMPLATE_MAP.put("", layout);
    }

    public static void setDefaultTemplate(String somePackage, ILayout layout) {
        DEFAULT_TEMPLATE_MAP.put(somePackage, layout);
    }

    public static void trace() {
        traceWith((template, timeToRender) -> System.out.println(" - Template " + template.getName() + " rendered in " + timeToRender + "ms"));
    }

    public static void traceWith(TraceCallback tracer) {
        TRACER = tracer;
    }

    public static ILayout getDefaultTemplate(String packageName) {
        if (DEFAULT_TEMPLATE_MAP.containsKey(packageName)) {
            return DEFAULT_TEMPLATE_MAP.get(packageName);
        } else {
            return getDefaultTemplate(packageName.substring(0, Math.max(0, packageName.lastIndexOf('.'))));
        }
    }

    public static TraceCallback getTracer() {
        return TRACER;
    }

    public interface TraceCallback {
        public void trace(Class template, long timeToRender);
    }

}
