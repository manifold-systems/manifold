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

package manifold.api.yaml;

import abc.yaml.Contact;
import abc.yaml.Invoice;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import javax.script.Bindings;
import junit.framework.TestCase;
import manifold.api.json.Yaml;
import manifold.util.JsonUtil;
import manifold.util.StreamUtil;

public class YamlTest extends TestCase
{
  public void testYaml() throws IOException
  {
    Bindings bindings = (Bindings)Yaml.fromYaml( StreamUtil.getContent( new InputStreamReader( getClass().getResourceAsStream( "/abc/yaml/Invoice.yaml" ) ) ) );
    System.out.println( JsonUtil.toJson( bindings ) );
  }

  public void testSampleYaml() throws FileNotFoundException
  {
    Invoice invoice = Invoice.load().fromYamlUrl( getClass().getResource( "/abc/yaml/Invoice.yaml" ) );
    Integer invoiceId = invoice.getInvoice();
    assertEquals( 34843, invoiceId.intValue() );
    String date = invoice.getDate();
    assertEquals( "2001-01-23", date );
    Invoice.bill_to bill_to = invoice.getBill_to();
    String given = bill_to.getGiven();
    assertEquals( "Chris", given );
    String family = bill_to.getFamily();
    assertEquals( "Dumars", family );
    Invoice.bill_to.address address = bill_to.getAddress();
    String lines = address.getLines();
    assertEquals( "458 Walkman Dr.\nSuite #292\n", lines );
    String city = address.getCity();
    assertEquals( "Royal Oak", city );
    String state = address.getState();
    assertEquals( "MI", state );
    Integer postal = address.getPostal();
    assertEquals( 48046, postal.intValue() );
    Invoice.ship_to ship_to = invoice.getShip_to();
    String given2 = ship_to.getGiven();
    String family2 = ship_to.getFamily();
    Invoice.ship_to.address address2 = ship_to.getAddress();
    String lines2 = address2.getLines();
    String city2 = address2.getCity();
    String state2 = address2.getState();
    Integer postal2 = address2.getPostal();
    Invoice.product products = (Invoice.product)invoice.getProduct();
    Invoice.product.productItem product = products.get( 0 );
    String sku = product.getSku();
    Integer quantity = product.getQuantity();
    String description = product.getDescription();
    Double price = product.getPrice();
    Double tax = invoice.getTax();
    Double total = invoice.getTotal();
    assertEquals( 4443.52, Math.round( total * 100d ) / 100d );
    String comments = invoice.getComments();
    assertEquals( "Late afternoon is best. Backup contact is Nancy Billsmer @ 338-4338.", comments );
  }

  public void testJsonSchemaYaml()
  {
    Contact contact = Contact.builder()
      .withName( "Scott McKinney" )
      .withDateOfBirth( LocalDate.of( 1986, 8, 9 ) )
      .withPrimaryAddress( Contact.Address.create( "111 Main St.", "Cupertino", "CA" ) ).build();
    Contact.Address primaryAddress = contact.getPrimaryAddress();
    assertEquals( "111 Main St.", primaryAddress.getStreet_address() );
    assertEquals( "Cupertino", primaryAddress.getCity() );
    assertEquals( "CA", primaryAddress.getState() );
    assertEquals( "Scott McKinney", contact.getName() );
    assertEquals( LocalDate.of( 1986, 8, 9 ), contact.getDateOfBirth() );
    assertEquals( "{\n" +
                  "  \"Name\": \"Scott McKinney\",\n" +
                  "  \"DateOfBirth\": \"1986-08-09\",\n" +
                  "  \"PrimaryAddress\": {\n" +
                  "    \"street_address\": \"111 Main St.\",\n" +
                  "    \"city\": \"Cupertino\",\n" +
                  "    \"state\": \"CA\"\n" +
                  "  }\n" +
                  "}", contact.write().toJson() );
    assertEquals( "Name: Scott McKinney\n" +
                  "DateOfBirth: 1986-08-09\n" +
                  "PrimaryAddress:\n" +
                  "  street_address: 111 Main St.\n" +
                  "  city: Cupertino\n" +
                  "  state: CA\n", contact.write().toYaml() );
  }

  public void testFragmentValue()
  {
    /*[>MyYamlObject.yaml<]
    name: Scott
    location:
      planet: fubar
      coordinates: 123456
    */
    MyYamlObject sample = MyYamlObject.fromSource();
    assertEquals( "Scott", sample.getName() );
    MyYamlObject.location location = sample.getLocation();
    assertEquals( "fubar", location.getPlanet() );
    assertEquals( 123456, (int)location.getCoordinates() );
  }
}
