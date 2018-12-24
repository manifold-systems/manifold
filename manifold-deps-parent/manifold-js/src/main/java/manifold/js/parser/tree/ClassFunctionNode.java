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


public class ClassFunctionNode extends FunctionNode
{

  private String _className;
  private Boolean _isStatic = false;
  private Boolean _isOverride = false;
  private String _returnType = "Object";

  public ClassFunctionNode(String name )
  {
    super( name );
  }

  //Test constructors
  public ClassFunctionNode(String name, String className, boolean isStatic )
  {
    super( name );
    _className = className;
    _isStatic = isStatic;
  }

  public ClassFunctionNode(String name, String className)
  {
    super( name );
    _className = className;
  }



  public boolean isStatic() {
    return _isStatic;
  }

  public void setStatic(boolean isStatic) {
    _isStatic = isStatic;
  }

  public boolean isOverride() {
    return _isOverride;
  }

  public void setOverride(boolean isOverride) {
    _isOverride = isOverride;
  }

  public void setReturnType(String returnType){  _returnType = returnType; }

  public String getReturnType() {return _returnType;}


  @Override
  public String genCode()
  {
    String parameterCode = (getFirstChild(ParameterNode.class) == null) ?
            "" : getFirstChild(ParameterNode.class).genCode();
    String functionBodyCode = (getFirstChild(FunctionBodyNode.class) == null) ?
            "{}" : getFirstChild(FunctionBodyNode.class).genCode();

    //If it's an override function, give as key value pair for Java.extend codegen from ClassNode
    if (isOverride()) {
      return getName() + ": function(" + parameterCode + ")" + functionBodyCode;
    }
    else return _className + (_isStatic?".":".prototype.") + //If static, can be method of class directly
            getName() + " = " + "function" + "(" + parameterCode + ")" + functionBodyCode;
  }


  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ClassFunctionNode)) return false;
    ClassFunctionNode node = (ClassFunctionNode) obj;
    return getName().equals(node.getName()) && _isStatic == ((ClassFunctionNode) obj).isStatic();
  }
}
