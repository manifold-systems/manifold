package manifold.ext.props.infer;

/**
 * Exercises the naming collision possible with boolean properties. Here we have getObject() and
 * isObject() which result in the same property name "object". However, property inference handles
 * this case by keeping the name for boolean "is" prefix getters as-is. Thus, the inferred name
 * for isObject() is "isObject" resolving the collision.
 */
public class IsCollisionClass {
    private Object object;

    public IsCollisionClass(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return object;
    }
    public void setObject(Object value) {
        object = value;
    }

    public boolean isObject() {
        return !(object instanceof Number) &&
                !(object instanceof Character) &&
                !(object instanceof Boolean);
    }
}
