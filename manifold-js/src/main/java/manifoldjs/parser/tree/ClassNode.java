package manifoldjs.parser.tree;


import java.util.*;
import java.util.stream.Collectors;

public class ClassNode extends Node {
    /*Boiler plate code segments taken from babel.js*/
    //Used for defining object properties
    private static final String CREATE_CLASS = "var _createClass = function () { " +
        "function defineProperties(target, props) { for (var i = 0; i < props.length; i++) " +
        "{ var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; " +
        "descriptor.configurable = true; if (\"value\" in descriptor) descriptor.writable = true; " +
        "Object.defineProperty(target, descriptor.key, descriptor); } } " +
        "return function (Constructor, protoProps, staticProps) { if (protoProps) " +
        "defineProperties(Constructor.prototype, protoProps); if (staticProps) " +
        "defineProperties(Constructor, staticProps); return Constructor; }; }();\n";

    //Used to make sure classes can not be called as a function
    private static final String CLASS_CALL_CHECK = "function _classCallCheck(instance, Constructor) { " +
        "if (!(instance instanceof Constructor)) { " +
        "throw new TypeError(\"Cannot call a class as a function\") } }\n";

    //name of generated supertype object
    public static final String SUPERTYPE_OBJECT = "_superClassObject";

    private String _superClass = null;

    public ClassNode(String name ) {
        super(name);
    }

    public ClassNode(String name, String superClass ) {
        super(name);
        _superClass = superClass;
    }

    public void setSuperClass(String superClass) {
        _superClass = superClass;
    }
    public String getSuperClass() {
        return  _superClass;
    }


    @Override
    public String genCode() {
        StringBuilder code = new StringBuilder(CLASS_CALL_CHECK); //Makes sure constructor is called correctly
        if (!getChildren(PropertyNode.class).isEmpty()) code.append(CREATE_CLASS); //Defines getters and setters

        code.append("var ").append(getName()).append(" = function(")
                .append((getSuperClass() == null? "" : "_" + getSuperClass()))
                .append(") { ");

        String constructorCode;
        if (getChildren(ConstructorNode.class).isEmpty()) {
            //Gen default constructor if no child found
            constructorCode = "\n\t" + new ConstructorNode(getName() ).genCode();
        } else {
            //Should only have one constructor
            constructorCode = "\n\t" + getFirstChild(ConstructorNode.class).genCode();
        }

        //If superclass exists, instantiate superclass object inside constructor
        if (getSuperClass() != null) {
            StringBuilder superClassObjectCode = new StringBuilder();
            String superClassArg = "_" + getSuperClass();
            //Create extended superclass object
            superClassObjectCode.append("\n\tvar ").append(SUPERTYPE_OBJECT)
                    .append("= new (Java.extend(").append(superClassArg).append("))(){")
                    .append(genOverrideFunctionCode(getChildren(ClassFunctionNode.class))) //Add overridden methods
                    .append("};");
            //Create property reference for the superclass object
            superClassObjectCode.append("\n\t").append("this.").append(SUPERTYPE_OBJECT)
                    .append(" = ").append(SUPERTYPE_OBJECT).append(";");
            constructorCode = constructorCode.replaceFirst("[{]", "{" + superClassObjectCode.toString());
        }
        code.append(constructorCode);

        //Create method for getting super object
        if (getSuperClass() != null) {
            code.append("\n\t").append(getName()).append(".prototype._getSuperClass = function _getSuperClass(){" +
                    "return this._superClassObject}");
        }

        for (ClassFunctionNode node : getChildren(ClassFunctionNode.class)) {
            if (!node.isOverride()) code.append("\n\t").append(node.genCode());
        }

        code.append(genPropertyObjectCode(getChildren(PropertyNode.class)));

        code.append("\n\treturn " + getName() + ";\n}(")
                .append((getSuperClass() == null? "" : getSuperClass())) //Possibly give superclass as arg
                .append(");");

        return code.toString();
    }

    private String genOverrideFunctionCode(List<ClassFunctionNode> functionNodes) {
        return String.join(",", functionNodes.stream()
                .filter(node -> node.isOverride())
                .map(node -> node.genCode())
                .collect(Collectors.toList()));
    }


    private String genPropertyObjectCode (List<PropertyNode> propertyNodes) {
        //Wrapper to hold getters and setters for the same property
        class PropertyNodeWrapper {
            private String _name;
            private boolean _isStatic;
            private PropertyNode _getter = null;
            private PropertyNode _setter = null;

            public PropertyNodeWrapper(String name) {
                _name = name;
            }

            public void add(PropertyNode node) {
                if (node.isSetter()) _setter = node;
                else _getter = node;
            }

            public String genCode() {
                return "\n\t\t" + "{key: \"" + _name + "\"," +
                        (_setter != null?_setter.genCode()+",":"") +
                        (_getter != null?_getter.genCode():"") +
                        "}";
            }
        }

        String propCode = "";
        //combines getters and setters for each property
        if (!propertyNodes.isEmpty()) {
            //Separate static and non-static properties
            HashMap<String, PropertyNodeWrapper> propertyNodeBucket = new HashMap();
            HashMap<String, PropertyNodeWrapper> staticPropertyNodeBucket = new HashMap();
            propCode += "\n\t_createClass(" + getName() + ", ";
            for (PropertyNode node : propertyNodes) {
                //Get wrapper by property name, and insert name
                PropertyNodeWrapper wrapper;
                if (node.isStatic()) {
                    wrapper = staticPropertyNodeBucket.get(node.getName());
                    if (wrapper == null) wrapper = new PropertyNodeWrapper(node.getName());
                    staticPropertyNodeBucket.put(node.getName(), wrapper);
                }
                else  {
                    wrapper = propertyNodeBucket.get(node.getName());
                    if (wrapper == null) wrapper = new PropertyNodeWrapper(node.getName());
                    propertyNodeBucket.put(node.getName(), wrapper);
                }
                wrapper.add(node);
            }

            //Combine the properties into an array
            String nonStaticProps = (propertyNodeBucket.isEmpty()) ? "null" :
                    "[" + String.join(",", propertyNodeBucket.values().stream()
                        .map(prop->prop.genCode())
                        .collect(Collectors.toList())) + "]";
            String staticProps = (staticPropertyNodeBucket.isEmpty()) ? "null" :
                    "[" + String.join(",", staticPropertyNodeBucket.values().stream()
                            .map(prop->prop.genCode())
                            .collect(Collectors.toList())) + "]";
            propCode += nonStaticProps + "," + staticProps + ");";
        }
        return  propCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ClassNode)) return false;
        ClassNode node = (ClassNode) obj;
        return getName().equals(node.getName()) && getSuperClass().equals(node.getSuperClass());
    }
}
