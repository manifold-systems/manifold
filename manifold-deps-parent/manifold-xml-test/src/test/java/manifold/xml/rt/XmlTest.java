/*
 * Copyright (c) 2020 - Manifold Systems LLC
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

package manifold.xml.rt;

import java.util.ArrayList;
import java.util.Arrays;
import manifold.rt.api.Bindings;

import manifold.api.yaml.rt.Yaml;
import manifold.xml.rt.Xml;
import org.junit.Test;
import abc.xml.Catelog;
import abc.xml.Stuff;

import static junit.framework.TestCase.assertEquals;

public class XmlTest
{
  @Test
  public void testManifold()
  {
    Stuff stuff = Stuff.fromSource();
    String writeXml = stuff.write().toXml( (String)null );
    assertEquals(
      "<stuff one=\"hello\" two=\"bye\">\n" +
      "  my stuff\n" +
      "  <things one=\"hello\" two=\"bye\">\n" +
      "    my things\n" +
      "  </things>\n" +
      "  <other attr1=\"a\" attr2=\"b\">\n" +
      "    <![CDATA[\n" +
      "      <message> other things </message>\n" +
      "    ]]>\n" +
      "  </other>\n" +
      "  <listElem foo=\"bar\"/>\n" +
      "  <listElem fu=\"barf\"/>\n" +
      "</stuff>\n", writeXml );

    stuff = Stuff.create();
    stuff.setStuff( Stuff.stuff.create() );
    stuff.getStuff().setTextContent( "hey now" );
    stuff.getStuff().put( "custom", 8 );
    assertEquals(
      "<stuff custom=\"8\">\n" +
      "  hey now\n" +
      "</stuff>\n", stuff.write().toXml() );
  }

  @Test
  public void testXmlToBindingsToXml()
  {
    String xml =
      "<stuff one=\"hello\" two=\"bye\">\n" +
      "  my stuff\n" +
      "  <things one=\"hello\" two=\"bye\">\n" +
      "    my things\n" +
      "  </things>\n" +
      "  <other attr1=\"a\" attr2=\"b\">\n" +
      "    <![CDATA[\n" +
      "      <message> other things </message>\n" +
      "    ]]>\n" +
      "  </other>\n" +
      "  <listElem foo=\"bar\"/>\n" +
      "  <listElem fu=\"barf\"/>\n" +
      "</stuff>\n";

    Bindings bindings = Xml.fromXml( xml, true );
    String toXml = Xml.toXml( bindings );
    assertEquals( xml, toXml );
  }

  @Test
  public void testYamlToXMlBindingsEqual()
  {
    String yaml =
      "--- #!<tag:clarkevans.com,2002:invoice>\n" +
      "invoice: '34843'\n" +
      "date   : 2001-01-23\n" +
      "bill-to: &id001\n" +
      "  given  : Chris\n" +
      "  family : Dumars\n" +
      "  address:\n" +
      "    lines: |\n" +
      "      458 Walkman Dr.\n" +
      "      Suite #292\n" +
      "    city    : Royal Oak\n" +
      "    state   : MI\n" +
      "    postal  : '48046'\n" +
      "ship-to: *id001\n" +
      "product:\n" +
      "- sku         : BL394D\n" +
      "  quantity    : '4'\n" +
      "  description : Basketball\n" +
      "  price       : '450.00'\n" +
      "- sku         : BL4438H\n" +
      "  quantity    : '1'\n" +
      "  description : Super Hoop\n" +
      "  price       : '2392.00'\n" +
      "tax  : '251.42'\n" +
      "total: '4443.52'\n" +
      "comments:\n" +
      "  Late afternoon is best.\n" +
      "  Backup contact is Nancy\n" +
      "  Billsmer @ 338-4338.";

    Bindings yamlBindings = (Bindings)Yaml.fromYaml( yaml );
    String xml = Xml.toXml( yamlBindings );
    Bindings xmlBindings = Xml.fromXml( xml );
    assertEquals( yamlBindings, xmlBindings );
  }

  @Test
  public void testProlog()
  {
    final String xml =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
      "<nothing/>";
    Bindings bindings = Xml.fromXml( xml, true );
    String toXml = Xml.toXml( bindings );
    assertEquals( "<nothing/>\n", toXml );
  }

  @Test
  public void testArrayInForeachLoop()
  {
    ArrayList<String> brands = new ArrayList<>();
    Catelog catelog = Catelog.fromSource();
    catelog.getProductListing().getProduct().forEach(p -> {
      if (p.getDepartment().equals("Men's")) {
        brands.add(p.getBrand());
      }
    });
    assertEquals(Arrays.asList("Joe's", "Squarepants"), brands);

    brands.clear();
    for (Catelog.ProductListing.Product.ProductItem p : catelog.getProductListing().getProduct()) {
      if( p.getDepartment().equals("Men's") ) {
        brands.add(p.getBrand());
      }
    }
    assertEquals(Arrays.asList("Joe's", "Squarepants"), brands);

    brands.clear();
    Catelog.ProductListing.Product list = catelog.getProductListing().getProduct();
    for (Catelog.ProductListing.Product.ProductItem p : list) {
      if( p.getDepartment().equals("Men's") ) {
        brands.add(p.getBrand());
      }
    }
    assertEquals(Arrays.asList("Joe's", "Squarepants"), brands);
  }

}
