package manifold.js.parser.tree;

import manifold.api.gen.SrcParameter;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Created by lmeyer-teruel on 7/26/2016.
 */

public class ParameterNode extends Node {
    private ArrayList<String> _params;
    private ArrayList<String> _types;

    public ParameterNode()
    {
        super(null);
        _params = new ArrayList<>();
        _types = new ArrayList<>();
    }

    //Takes in parameter and type in string form
    public void addParam(String param, String type) {
        _params.add(param);
        String paramType = (type != null && !type.isEmpty()) ? type :"java.lang.Object";
        _types.add(paramType);
    }

    public ArrayList<String> getTypes() {
        return _types;
    }

    public SrcParameter[] toParamList() {
        SrcParameter[] parameterInfoBuilders = new SrcParameter[_params.size()];
        for (int i = 0; i < _params.size(); i++) {
            parameterInfoBuilders[i] = new SrcParameter(_params.get(i), _types.get(i));
        }
        return parameterInfoBuilders;
    }

    @Override
    public String genCode()
    {
        return _params.stream().collect(Collectors.joining(","));
    }
}
