package manifold.templates.runtime;

/**
 * Created by eim on 8/1/2017.
 */
public class TemplateRuntimeException extends RuntimeException {
    public TemplateRuntimeException(){
    }

    public TemplateRuntimeException(String message) {
        super(message);
    }

    public TemplateRuntimeException(Exception e) {
        super(e);
    }
}
