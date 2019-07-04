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

package manifold.js.parser.tree;

import manifold.js.parser.Token;

public class ArrowExpressionNode extends Node
{
  private String _params = "";

  public ArrowExpressionNode()
  {
    super(null);
  }

  public void extractParams(FillerNode fillerNode) {
    Token backToke = fillerNode.removeLastNonWhitespaceToken();
    //If prev token is not ')', then there is only one parameter
    if (!backToke.getValue().equals(")")) {
      _params = backToke.getValue();
      return;
    }
    //Otherwise, backtrack through list until opening parens
    backToke = fillerNode.removeLastToken();
    while (!(backToke.getValue().equals("("))) {
      _params = backToke.getValue() + _params;
      backToke = fillerNode.removeLastToken();
    }
  }

  @Override
  public String genCode()
  {
    //## todo: this was ok with nashorn, but maybe not with rhino...

    /*For expressions, use closure extension function (ex. function square(x) x*x;)*/
    return "function ("  + _params + ")" + super.genCode();
  }
}
