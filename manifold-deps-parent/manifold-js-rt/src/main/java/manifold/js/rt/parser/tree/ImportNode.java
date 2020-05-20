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


public class ImportNode extends Node
{
  public ImportNode(String packageName )
  {
    super( packageName );
    int lastDotIndex = packageName.lastIndexOf('.') + 1;
    if (lastDotIndex < 0) lastDotIndex = 0;
    _packageClass = packageName.substring(lastDotIndex);
  }


  private String _packageClass;

  @Override
  public String genCode()
  {
    return "var " + _packageClass + " = Java.type(\'" + getName() + "\');";
  }

  public String getPackageClass() {
    return _packageClass;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ImportNode)) return false;
    ImportNode node = (ImportNode) obj;
    return getName() != node.getName();
  }
}
