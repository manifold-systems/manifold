/*
 * Copyright (c) 2018 - Manifold Systems LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package manifold.js.parser.tree.template;


import manifold.js.parser.tree.ImportNode;
import manifold.js.parser.tree.Node;
import manifold.js.parser.tree.ParameterNode;

/*Serves as the root for template files (.jst) . Generates a function with the template parameters as well as a
placeholder
parameter for the raw strings that will be passed in when called from the call handler
 */

public class JSTNode extends Node
{
  //boiler plate code for constructing string
  private final String STR_BUILDER = "_strTemplateBuilder"; //javascript variable that builds and returns string
  private final String RAW_STR_LIST = "_rawStrList"; //parameter of raw strings inputted into the template

  private String TEMPLATE_HEADER1 = "function renderToString("; //name of function; leaves room for parameters
  private String TEMPLATE_HEADER2 = RAW_STR_LIST + ") {" + STR_BUILDER + " = '';"; //initializes str builder

  private final String TEMPLATE_FOOTER =
    "\n\treturn " + STR_BUILDER + ";\n}";

  public JSTNode()
  {
    super( null );
  }

  @Override
  public String genCode()
  {
    ParameterNode paramNode = getFirstChild( ParameterNode.class );
    String parameterCode = "";
    if( paramNode != null )
    {
      parameterCode = paramNode.genCode();
      if( paramNode.genCode().length() > 0 )
      {
        parameterCode = parameterCode + ", ";
      }
    }

    StringBuilder code = new StringBuilder();
    for( ImportNode node : getChildren( ImportNode.class ) )
    {
      code.append( "\n" ).append( node.genCode() );
    }

    //Add the header and parameters for the generated function
    code.append( "\n" ).append( TEMPLATE_HEADER1 ).append( parameterCode ).append( TEMPLATE_HEADER2 );

    //Keep track of number of raw strings added to know which index of the raw string array to add
    int rawStringCount = 0;
    for( Node node : getChildren() )
    {
      if( node instanceof RawStringNode )
      {
        addRawString( code, rawStringCount );
        rawStringCount++;
      }
      else if( node instanceof ExpressionNode )
      {
        addExpression( code, (ExpressionNode)node );
      }
      else if( node instanceof StatementNode )
      {
        addStatement( code, (StatementNode)node );
      }
    }
    code.append( TEMPLATE_FOOTER );
    return code.toString();
  }

  //Add whatever the expression evaluates to into the generated code
  private void addExpression( StringBuilder code, ExpressionNode node )
  {
    code.append( "\n\t" ).append( STR_BUILDER )
      .append( " += " )
      .append( node.genCode() );
  }

  //Statement code not directly included in template output; instead is simply added into the genCode logic
  private void addStatement( StringBuilder code, StatementNode node )
  {
    code.append( "\n" ).append( node.genCode() );
  }

  /*raw strings will be passed into the function as a list, so just add the element from the argument list*/
  private void addRawString( StringBuilder code, int count )
  {
    code.append( "\n\t" ).append( STR_BUILDER )
      .append( " += " )
      .append( RAW_STR_LIST )
      .append( "[" + count + "]" );
  }
}
