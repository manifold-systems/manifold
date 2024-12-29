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
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import manifold.api.fs.IFile;
import manifold.api.gen.SrcClass;
import manifold.api.gen.SrcExpression;
import manifold.api.gen.SrcField;
import manifold.api.gen.SrcMethod;
import manifold.api.gen.SrcRawExpression;
import manifold.api.gen.SrcReturnStatement;
import manifold.api.gen.SrcStatementBlock;
import manifold.api.gen.SrcType;
import manifold.api.util.cache.FqnCache;
import manifold.api.util.cache.FqnCacheNode;
import manifold.util.ReflectUtil;

/**
 *
 */
class ResourceBundelCodeGen extends CommonCodeGen
{
  public static final String FIELD_RESOURCE_BUNDLE = "_resourceBundle";

  ResourceBundelCodeGen( FqnCache<SrcRawExpression> model, IFile file, String fqn )
  {
    super(model, file, fqn);
  }

  protected void extendSrcClass( SrcClass srcClass, FqnCache<SrcRawExpression> model )
  {
    srcClass.imports( ResourceBundle.class, Locale.class, Field.class, Modifier.class , ReflectUtil.class);

    // Initialize the ResourceBundle with the default locale.
    srcClass.addField(
        new SrcField( srcClass )
            .name( FIELD_RESOURCE_BUNDLE )
            .modifiers( Modifier.PRIVATE | Modifier.STATIC )
            .type( ResourceBundle.class )
            .initializer("ResourceBundle.getBundle(\"" + _fqn + "\", Locale.ROOT)" ) );

    // Method to set the locale
    srcClass.addMethod(
        new SrcMethod( srcClass )
            .name("setLocale")
            .modifiers( Modifier.PUBLIC | Modifier.STATIC )
            .addParam("locale", Locale.class )
            .body( new SrcStatementBlock()
                .addStatement(FIELD_RESOURCE_BUNDLE + " = ResourceBundle.getBundle(\"" + _fqn + "\", locale);" )
            ));

    // Method to get the locale
    srcClass.addMethod(
        new SrcMethod( srcClass )
            .name( "getLocale" )
            .modifiers( Modifier.PUBLIC | Modifier.STATIC )
            .returns( Locale.class )
            .body( "return " + FIELD_RESOURCE_BUNDLE + ".getLocale();" ) );
  }

  @Override
  protected SrcExpression createPropertyValueField( FqnCacheNode<SrcRawExpression> node ) {
    return new SrcRawExpression("\"\"");
  }

  @Override
  protected void addRegularGetter( SrcClass srcClass, FqnCacheNode<SrcRawExpression> node )
  {
    for( FqnCacheNode<SrcRawExpression> childNode: node.getChildren() )
    {
      if (childNode.getUserData() != null && childNode.getChildren().isEmpty() ) {
        srcClass.addMethod(
            new SrcMethod(srcClass)
                .name(createGetterForPropertyName(childNode.getName()))
                .modifiers(Modifier.PUBLIC | (isRootProperty(childNode) ? Modifier.STATIC : 0))
                .returns(new SrcType("String"))
                .body(
                    new SrcStatementBlock()
                        .addStatement(new SrcReturnStatement(childNode.getUserData()))));
      }
    }
  }

  public static String createGetterForPropertyName(String name) {
    return "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
  }
}
