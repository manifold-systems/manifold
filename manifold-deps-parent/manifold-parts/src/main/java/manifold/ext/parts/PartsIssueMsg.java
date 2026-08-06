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

package manifold.ext.parts;

import manifold.api.util.IssueMsg;

public class PartsIssueMsg
{
  public static final IssueMsg MSG_DELEGATING_CLASS_DOES_NOT_IMPLEMENT = new IssueMsg( "Delegating class '{0}' does not implement '{1}' of linked field '{2}'" );
  public static final IssueMsg MSG_INTERFACE_IS_INTERNAL_TO_DELEGATE = new IssueMsg( "Interface '{0}' is internal to '{1}'" );
  public static final IssueMsg MSG_INTERNAL_ACCESS_NOT_ALLOWED_HERE = new IssueMsg( "'{0}' has internal access in '{1}'" );;
  public static final IssueMsg MSG_ONLY_INTERFACES_HERE = new IssueMsg( "Only interfaces allowed here" );
  public static final IssueMsg MSG_INTERFACE_OVERLAP = new IssueMsg( "Interface '{0}' found in other links: '{1}'. Use '@link(share={0}.class)' to share the '{0}' link with '{1}' or implement the interface directly." );
  public static final IssueMsg MSG_INTERFACE_OVERLAP_AUTO_RESOLVE = new IssueMsg( "Interface '{0}' found in other links: '{1}', is implicitly shared through link '{2}', please add '@link(share={0}.class)' to acknowledge the this." );
  public static final IssueMsg MSG_METHOD_OVERLAP = new IssueMsg( "Method '{0}' found in multiple links '{1}', this method must be implemented directly" );
  public static final IssueMsg MSG_AMBIGUOUS_RECEIVER = new IssueMsg( "Ambiguous call. '{0}' is declared in multiple interfaces of this part ({1}), which dispatch independently and may reach different implementations: qualify with the intended interface e.g. ((A)this).{0}" );
  public static final IssueMsg MSG_LINK_STATIC_FIELD = new IssueMsg( "@link is not supported on static members" );
  public static final IssueMsg MSG_MODIFIER_REDUNDANT_FOR_LINK = new IssueMsg( "Modifier '{0}' is redundant for part links" );
  public static final IssueMsg MSG_MODIFIER_NOT_ALLOWED_HERE = new IssueMsg( "Modifier '{0}' not allowed here" );
  public static final IssueMsg MSG_PART_THIS_NONINTERFACE_USE = new IssueMsg( "'this' in a part class must be used as an interface here" );
  public static final IssueMsg MSG_PART_LINKFIELD_USE = new IssueMsg( "@link fields may only be used as direct method-call receivers, like 'super'" );
  public static final IssueMsg MSG_MULTIPLE_SHARING = new IssueMsg( "Interface '{0}' is shared by multiple links: '{1}'" );
  public static final IssueMsg MSG_SUPERCLASS_PART = new IssueMsg( "@part superclass requires @part subclass" );
  public static final IssueMsg MSG_SUPERCLASS_NOT_PART = new IssueMsg( "@part subclass requires @part superclass" );
  public static final IssueMsg MSG_INTERFACE_LINK_FIELD_TYPE_EXPECTED = new IssueMsg( "@link field must have an interface type" );
}