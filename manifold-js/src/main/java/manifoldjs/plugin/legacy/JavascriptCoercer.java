package manifoldjs.plugin.legacy;

import gw.lang.reflect.Type;
import gw.lang.reflect.TypeSystem;
import gw.lang.reflect.IType;

import java.util.HashMap;

/**
 * Created by lmeyer-teruel on 8/2/2016.
 */
public class JavascriptCoercer {


    HashMap<String, CoercionFunction> _JStoJavaMap;
    HashMap<String, CoercionFunction> _JavatoJSMap;

    interface CoercionFunction {
        Object operation(Object a);
    }

    public JavascriptCoercer() {
        _JavatoJSMap = new HashMap<String, CoercionFunction>();
        _JStoJavaMap = new HashMap<String, CoercionFunction>();

        //Initial coercions are from double to int and double to string upon return
        addJStoJavaCoercer("Double", "Integer", (o -> {
            return ((Double) o).intValue();
        }));
        addJStoJavaCoercer("Double", "String", (o -> {
            return ((Double) o).toString();
        }));

        addJavatoJSCoercer("Integer", "Double", (o -> {
            return ((Integer) o).doubleValue();
        }));

    }

    /* function: addJStoJavaCoercer
    *  ---------------------------
    *  This function adds a new coercion rule to the _JStoJavaMap and returns true if
    *  succesfuly added, false if it fails
    */
    public Boolean addJStoJavaCoercer(String fromTypeString, String toTypeString, CoercionFunction func) {
        return addToHashMap(fromTypeString, toTypeString, _JStoJavaMap, func);
    }

    /* function: addJavatoJSCoercer
    *  ---------------------------
    *  This function adds a new coercion rule to the _JavatoJSMap and returns true if
    *  succesfuly added, false if it fails
    */
    public Boolean addJavatoJSCoercer(String fromTypeString, String toTypeString, CoercionFunction func) {
        return addToHashMap(fromTypeString, toTypeString, _JavatoJSMap, func);
    }


    /* function: addToHashMap
    *  ---------------------------
    *  Converts the two strings into ITypes and returns false if either of the types is not found, it then converts
    *
    */
    private Boolean addToHashMap(String fromTypeString, String toTypeString, HashMap<String, CoercionFunction> converter, CoercionFunction coercer) {
        IType toType;
        IType fromType;
        try {
            fromType = TypeSystem.getByRelativeName(fromTypeString);
            toType = TypeSystem.getByRelativeName(toTypeString);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        converter.put(fromType.getName() + toType.getName(), coercer);
        return true;
    }



    public Object coerceTypesJavatoJS(Object anyType, String typeTo) {
        return coerceTypes(anyType, typeTo, _JavatoJSMap);
    }

    public Object coerceTypesJStoJava(Object anyType, String typeTo) {
        return coerceTypes(anyType, typeTo, _JStoJavaMap);
    }

    private Object coerceTypes(Object anyType, String typeTo, HashMap<String, CoercionFunction> map) {
        String currentType = anyType.getClass().getName();
         CoercionFunction resultant = map.get(currentType + typeTo);
        if (resultant == null) {
            return anyType;
        }
        return resultant.operation(anyType);
    }
}


