/*
 * Copyright (c) 2023 - Manifold Systems LLC
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

package manifold.ext.params;

import manifold.api.util.IssueMsg;

public class ParamsIssueMsg
{
  public static final IssueMsg MSG_OVERRIDE_DEFAULT_VALUES_NOT_ALLOWED = new IssueMsg( "Default values are inherited here" );
  public static final IssueMsg MSG_OPT_PARAM_METHOD_CLASHES_WITH_SUBSIG = new IssueMsg( "'{0}' clashes with '{1}' on overload signature '({2})'" );
  public static final IssueMsg MSG_OPT_PARAM_METHOD_INDIRECTLY_CLASHES = new IssueMsg( "'{0}' indirectly clashes with '{1}'" );
  public static final IssueMsg MSG_OPT_PARAM_METHOD_INDIRECTLY_OVERRIDES = new IssueMsg( "'{0}' indirectly overrides method '{1}' in class '{2}'" );
}