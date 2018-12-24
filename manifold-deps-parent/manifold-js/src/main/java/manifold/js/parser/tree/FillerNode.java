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
import manifold.js.parser.TokenType;

import java.util.LinkedList;

/*(Hopefully) Temporary Node to hold tokens that we don't parse and blindly concatenate*/
public class FillerNode extends Node
{

  /*Either consists of a list of tokens or just a string of concatenated tokens*/
  private LinkedList<Token> _tokens;
  private String _content;
  boolean _inOverrideFunction;

  public FillerNode()
  {
    super( null );
    _tokens = new LinkedList<>();
  }

  public FillerNode(boolean inOverrideFunction)
  {
    super( null );
    _tokens = new LinkedList<>();
    _inOverrideFunction = inOverrideFunction;
  }

  public FillerNode(String content)
  {
    super( null );
    _content = content;
  }

  public void concatToken(Token token) {
    _tokens.add(token);
  }

  public Token removeLastToken() {
    return _tokens.removeLast();
  }

  public Token removeLastNonWhitespaceToken() {
    Token toke = _tokens.removeLast();
    while (toke.getType().equals(TokenType.WHITESPACE)) {
      toke = _tokens.removeLast();
    }
    return toke;
  }

  @Override
  public String genCode()
  {
    if (_content != null) return _content;
    StringBuilder code = new StringBuilder();
    for (Token token : _tokens) {
      //Replace super with Java.super(_superClassObject) to support java-style super
      if (token.getType() == TokenType.KEYWORD && token.getValue().equals("super")) {
        //needs "this._superClassObject" to reference super if not function does not override
        code.append("Java.super(" + (_inOverrideFunction?"":"this.") + ClassNode.SUPERTYPE_OBJECT + ")");
      } else {
        code.append(token.getValue());
      }
    }
    return code.toString();
  }
}
