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

package manifold.js.rt.parser.tree.template;


import manifold.js.rt.parser.tree.Node;

/*Holds template literals inside javascript files. Supports interpolation and multiline characters*/

public class TemplateLiteralNode extends Node
{

  public TemplateLiteralNode()
  {
    super( null );
  }

  @Override
  public String genCode()
  {
    StringBuilder string = new StringBuilder();
    for (Node node:getChildren()) {
      if (node != getChildren().get(0)) string.append("+");
      if (node instanceof RawStringNode) {
        string.append("\"").append(node.genCode()).append("\"");
      } else if (node instanceof ExpressionNode) {
        string.append("(").append(node.genCode()).append(")");
      }
    }
    return string.toString();
  }

}
