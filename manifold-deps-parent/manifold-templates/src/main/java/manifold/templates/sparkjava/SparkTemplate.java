package manifold.templates.sparkjava;

import manifold.templates.runtime.BaseTemplate;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

public class SparkTemplate extends BaseTemplate {

    private static ThreadLocal<Request> REQUEST = new ThreadLocal<Request>();
    private static ThreadLocal<Response> RESPONSE = new ThreadLocal<Response>();

    public Response getResponse() {
        return RESPONSE.get();
    }

    public Request getRequest() {
        return REQUEST.get();
    }

    public static void init() {
        before((request, response) -> {
            REQUEST.set(request);
            RESPONSE.set(response);
        });

        afterAfter((request, response) -> {
            REQUEST.set(null);
            RESPONSE.set(null);
        });
    }

    @Override
    public String toS(Object o) {
        if (o instanceof RawObject) {
            return super.toS(o.toString());
        } else if (o == null) {
            return "";
        } else {
            return escapeHTML(o.toString());
        }
    }

    private String escapeHTML(String str) {
        return str.replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot")
                .replaceAll("'", "&#39");
    }

    public Object raw(Object o) {
        return new RawObject(o);
    }

    private static class RawObject {
        private final Object in;

        RawObject(Object in) {
            this.in = in;
        }

        public String toString() {
            return in.toString();
        }
    }

}
