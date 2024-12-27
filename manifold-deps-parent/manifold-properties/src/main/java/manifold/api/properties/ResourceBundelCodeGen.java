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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.ResourceBundle;

import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcExpression;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.util.cache.FqnCache;
import manifold.util.ReflectUtil;

/**
 *
 */
class ResourceBundelCodeGen extends PropertiesCodeGen
{
  public static final String FIELD_RESOURCE_BUNDLE = "_resourceBundle";

  ResourceBundelCodeGen( FqnCache<SrcExpression> model, IFile file, String fqn )
  {
    super(model, file, fqn);
  }

  protected void extendSrcClass( SrcClass srcClass, FqnCache<SrcExpression> model )
  {
    srcClass.imports( ResourceBundle.class, Locale.class, Field.class, Modifier.class , ReflectUtil.class);

    // Initialize the ResourceBundle with the default locale.
    srcClass.addField(
        new SrcField( srcClass )
            .name( FIELD_RESOURCE_BUNDLE )
            .modifiers( Modifier.PRIVATE | Modifier.STATIC )
            .type( ResourceBundle.class )
            .initializer("ResourceBundle.getBundle(\"" + _fqn + "\", Locale.ROOT);" ) );

    // Method to set the locale
    srcClass.addMethod(
        new SrcMethod(srcClass)
            .name("setLocale")
            .modifiers(Modifier.PUBLIC | Modifier.STATIC)
            .addParam("locale", Locale.class)
            .body(new SrcStatementBlock()
                .addStatement(FIELD_RESOURCE_BUNDLE + " = ResourceBundle.getBundle(\"" + _fqn + "\", locale);")
                .addStatement("resetFields(" + _fqn + ".class, \"\");")
            ));

    // This method uses reflection to update all field values to the new one whenever the locale changes.
    srcClass.addMethod(new SrcMethod(srcClass)
        .name("resetFields")
        .modifiers( Modifier.PRIVATE | Modifier.STATIC)
        .addParam("object", Object.class)
        .addParam("propName", String.class)
        .body("ReflectUtil.fields(object,fieldRef -> !fieldRef.getField().getName().startsWith(\"_\"))\n" +
            "    .forEach(fieldRef -> {\n" +
            "        Field field = fieldRef.getField();\n" +
            "        String newPropName = \"\".equals(propName) ? field.getName() : propName + \".\" + field.getName();\n" +
            "        if (String.class.equals(field.getType())) {\n" +
            "            fieldRef.set("  + _fqn + "." + FIELD_RESOURCE_BUNDLE + ".getString(newPropName));\n" +
            "        } else {\n" +
            "            try {\n" +
            "                resetFields(field.get(object), newPropName);\n" +
            "            } catch (IllegalAccessException e) {\n" +
            "                throw new RuntimeException(e);\n" +
            "            }\n" +
            "        }\n" +
            "    });\n"
        ));
  }
}
