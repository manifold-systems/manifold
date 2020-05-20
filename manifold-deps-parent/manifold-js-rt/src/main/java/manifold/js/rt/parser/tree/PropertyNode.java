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

public class PropertyNode extends ClassFunctionNode
{
  private boolean _isSetter;

  public PropertyNode( String name )
  {
    super( name );
  }

  //Test Constructor
  public PropertyNode(String name, boolean isSetter) {
    super(name);
    _isSetter = isSetter;
  }

  public PropertyNode(String name, String className, boolean isStatic, boolean isSetter) {
    super(name, className);
    _isSetter = isSetter;
    setStatic(isStatic);
  }

  public boolean isSetter() {
    return _isSetter;
  }

  @Override
  public String genCode()
  {
    String parameterCode = (getFirstChild(ParameterNode.class) == null) ?
            "" : getFirstChild(ParameterNode.class).genCode();
    String functionBodyCode = (getFirstChild(FunctionBodyNode.class) == null) ?
            "{}" : getFirstChild(FunctionBodyNode.class).genCode();
    return  (_isSetter?"set":"get") +
            ": function " + (_isSetter?"set":"get") + "(" + parameterCode + ")" +
            functionBodyCode; //Should have one FunctionBodyNode child
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof PropertyNode)) return false;
    PropertyNode node = (PropertyNode) obj;
    return getName().equals(node.getName()) && isStatic() == node.isStatic() && _isSetter == node.isSetter() ;
  }

}
