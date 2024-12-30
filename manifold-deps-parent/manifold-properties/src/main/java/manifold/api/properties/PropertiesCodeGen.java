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

package manifold.api.properties;

import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcRawExpression;
import manifold.api.util.cache.FqnCache;

class PropertiesCodeGen extends CommonCodeGen
{

    PropertiesCodeGen( FqnCache<SrcRawExpression> model, IFile file, String fqn )
    {
        super(model, file, fqn);
    }

    @Override
    protected void extendPropertyValueClass(SrcClass leafClass) {

    }

    @Override
    protected void extendSrcClass(SrcClass srcClass, FqnCache<SrcRawExpression> model)
    {
    }
}
