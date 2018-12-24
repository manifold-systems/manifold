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

package manifold.api.json;

import java.util.List;

/**
 */
public interface IJsonParentType extends IJsonType
{
  void addChild( String name, IJsonParentType child );

  IJsonType findChild( String name );

  List<JsonIssue> getIssues();
  void addIssue( JsonIssue issue );

  void render( StringBuilder sb, int indent, boolean mutable );
}
