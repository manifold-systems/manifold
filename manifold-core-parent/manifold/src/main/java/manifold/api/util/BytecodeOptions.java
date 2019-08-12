/*
 * Copyright (c) 2019 - Manifold Systems LLC
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

package manifold.api.util;

import java.util.List;
import manifold.util.concurrent.LocklessLazyVar;

/**
 */
public class BytecodeOptions
{
  public static LocklessLazyVar<Boolean> JDWP_ENABLED =
    new LocklessLazyVar<Boolean>()
    {
      protected Boolean init()
      {
        List<String> values = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
        for( String value : values )
        {
          if( value.startsWith( "-Xrunjdwp:" ) ||
              value.startsWith( "-agentlib:jdwp=" ) )
          {
            return true;
          }
        }
        return false;
      }
    };
}
