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

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import javax.script.Bindings;
import junit.framework.TestCase;
import manifold.api.json.Yaml;
import manifold.util.JsonUtil;
import manifold.util.StreamUtil;
import abc.yaml.Invoice;
import abc.yaml.Contact;

public class YamlTest extends TestCase
{
  public void testYaml() throws IOException
  {
    Bindings bindings = Yaml.fromYaml( StreamUtil.getContent( new InputStreamReader( getClass().getResourceAsStream( "/abc/yaml/Invoice.yaml" ) ) ) );
    System.out.println( JsonUtil.toJson( bindings ) );
  }

  public void testInvoice()
  {
    Invoice invoice = Invoice.load().fromYamlUrl( getClass().getResource( "/abc/yaml/Invoice.yaml" ) );
    System.out.println( invoice.toYaml() );
  }

  public void testContact()
  {
    Contact contact = Contact.builder()
      .withName("Scott McKinney")
      .withDateOfBirth( LocalDate.of(1986, 8, 9))
      .withPrimaryAddress(Contact.Address.create("111 Main St.", "Cupertino", "CA")).build();
    Contact.Address primaryAddress = contact.getPrimaryAddress();
    assertEquals( "111 Main St.", primaryAddress.getStreet_address() );
    assertEquals( "Cupertino", primaryAddress.getCity() );
    assertEquals( "CA", primaryAddress.getState() );
    assertEquals( "Scott McKinney", contact.getName() );
    assertEquals( LocalDate.of(1986, 8, 9), contact.getDateOfBirth() );
    assertEquals( "{\n" +
                  "  \"DateOfBirth\": \"1986-08-09\",\n" +
                  "  \"PrimaryAddress\": {\n" +
                  "    \"street_address\": \"111 Main St.\",\n" +
                  "    \"city\": \"Cupertino\",\n" +
                  "    \"state\": \"CA\"\n" +
                  "  },\n" +
                  "  \"Name\": \"Scott McKinney\"\n" +
                  "}", contact.toJson() );
  }

}
