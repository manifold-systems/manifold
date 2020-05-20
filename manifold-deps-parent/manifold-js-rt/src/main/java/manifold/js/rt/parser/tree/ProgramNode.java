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

package manifold.js.rt.parser.tree;


import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import manifold.js.rt.parser.Token;

public class ProgramNode extends Node
{
  private List<ParseError> _errorList;
  private URL _url;

  public ProgramNode( URL url )
  {
    super( null );
    _url = url;
    _errorList = new LinkedList<>();
  }

  public void addError( String msg, Token token ) {
    _errorList.add(new ParseError(msg, token));
  }

  @Override
  public ProgramNode getProgramNode()
  {
    return this;
  }

  public URL getUrl()
  {
    return _url;
  }

  public int errorCount() {
    return _errorList.size();
  }

  public List<ParseError> getErrorList() { return _errorList; }

  /*Returns the full package name of imported class (ex. java.util.ArrayList)
   *from class name (ex. ArrayList)*/
   public String getPackageFromClassName(String packageClass) {
    for (ImportNode node: getChildren(ImportNode.class)) {
      if (node.getPackageClass().equals(packageClass))
        return node.getName();
    }
     return null;
  }

  @Override
  public String genCode() {
    String code = super.genCode();
    return code;
  }
}
