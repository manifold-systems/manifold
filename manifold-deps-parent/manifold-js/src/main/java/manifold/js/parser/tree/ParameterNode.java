package manifold.js.parser.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import manifold.api.gen.SrcParameter;

public class ParameterNode extends Node
{
  private ArrayList<String> _params;
  private ArrayList<String> _types;

  public ParameterNode()
  {
    super( null );
    _params = new ArrayList<>();
    _types = new ArrayList<>();
  }

  //Takes in parameter and type in string form
  public void addParam( String param, String type )
  {
    _params.add( param );
    String paramType = (type != null && !type.isEmpty()) ? type : "java.lang.Object";
    _types.add( paramType );
  }

  public ArrayList<String> getTypes()
  {
    return _types;
  }

  public List<SrcParameter> toParamList()
  {
    List<SrcParameter> parameterInfoBuilders = new ArrayList<>( _params.size() );
    for( int i = 0; i < _params.size(); i++ )
    {
      parameterInfoBuilders.add( new SrcParameter( _params.get( i ), _types.get( i ) ) );
    }
    return parameterInfoBuilders;
  }

  @Override
  public String genCode()
  {
    return _params.stream().collect( Collectors.joining( "," ) );
  }
}
