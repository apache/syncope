/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.fit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.to.SAML2IdPTO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SAML2SPITCase extends AbstractITCase {

    @BeforeAll
    public static void samlSetup() {
        WebClient.client(saml2IdPService).
                accept(MediaType.APPLICATION_XML_TYPE).
                type(MediaType.APPLICATION_XML_TYPE);
        try {
            saml2IdPService.importFromMetadata((InputStream) WebClient.create(
                    "http://localhost:9090/auth/realms/master/protocol/saml/descriptor/").get().getEntity());
        } finally {
            WebClient.client(saml2IdPService).
                    accept(clientFactory.getContentType().getMediaType()).
                    type(clientFactory.getContentType().getMediaType());
        }

        List<SAML2IdPTO> idps = saml2IdPService.list();
        assertEquals(1, idps.size());

        SAML2IdPTO keycloak = idps.get(0);
        keycloak.setName("Keyloack");
        keycloak.setCreateUnmatching(true);
        keycloak.getItems().clear();

        ItemTO item = new ItemTO();
        item.setIntAttrName("username");
        item.setExtAttrName("username");
        item.setConnObjectKey(true);
        keycloak.getItems().add(item);

        item = new ItemTO();
        item.setIntAttrName("email");
        item.setExtAttrName("email");
        keycloak.getItems().add(item);

        item = new ItemTO();
        item.setIntAttrName("userId");
        item.setExtAttrName("email");
        keycloak.getItems().add(item);

        item = new ItemTO();
        item.setIntAttrName("firstname");
        item.setExtAttrName("givenName");
        keycloak.getItems().add(item);

        item = new ItemTO();
        item.setIntAttrName("surname");
        item.setExtAttrName("surname");
        keycloak.getItems().add(item);

        item = new ItemTO();
        item.setIntAttrName("fullname");
        item.setExtAttrName("fullName");
        keycloak.getItems().add(item);

        saml2IdPService.update(keycloak);
    }

    private List<Node> find(final NodeList children, final String match) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            if (match.equals(children.item(i).getNodeName())) {
                nodes.add(children.item(i));
            }
        }
        return nodes;
    }

    private void sso(final String baseURL) throws IOException, XMLStreamException {
        CloseableHttpClient httpclient = HttpClients.custom().setMaxConnPerRoute(100).build();
        HttpClientContext context = HttpClientContext.create();
        context.setCookieStore(new BasicCookieStore());

        // 1. fetch login page
        HttpGet get = new HttpGet(baseURL);
        CloseableHttpResponse response = httpclient.execute(get, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        // 2. click on the SAML 2.0 IdP
        get = new HttpGet(baseURL + "saml2sp/login?idp=http%3A%2F%2Flocalhost%3A9090%2Fauth%2Frealms%2Fmaster");
        response = httpclient.execute(get, context);

        // 3. process autosubmit form for login request
        Document autosubmitForm = StaxUtils.read(response.getEntity().getContent());

        Node body = find(autosubmitForm.getChildNodes().item(0).getChildNodes(), "body").get(0);
        assertNotNull(body);

        Node form = find(body.getChildNodes(), "form").get(0);
        assertNotNull(form);

        List<NameValuePair> toSubmit = new ArrayList<>();
        for (Node input : find(form.getChildNodes(), "input")) {
            Node name = input.getAttributes().getNamedItem("name");
            Node value = input.getAttributes().getNamedItem("value");
            if (name != null && value != null) {
                toSubmit.add(new BasicNameValuePair(name.getTextContent(), value.getTextContent()));
            }
        }

        HttpPost post = new HttpPost(form.getAttributes().getNamedItem("action").getTextContent());
        post.setEntity(new UrlEncodedFormEntity(toSubmit, Consts.UTF_8));
        response = httpclient.execute(post, context);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());

        // 4. get login form from Keycloack and submit with expected username and password
        get = new HttpGet(response.getFirstHeader(HttpHeaders.LOCATION).getValue());
        response = httpclient.execute(get, context);

        String action = StringUtils.substringBefore(
                StringUtils.substringAfter(EntityUtils.toString(response.getEntity()),
                        "<form id=\"kc-form-login\" onsubmit=\"login.disabled = true; return true;\" action=\""),
                "\" method=\"post\">").replace("&amp;", "&");

        toSubmit.clear();
        toSubmit.add(new BasicNameValuePair("username", "john.doe"));
        toSubmit.add(new BasicNameValuePair("password", "password"));

        post = new HttpPost(action);
        post.addHeader(new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.5"));
        post.setEntity(new UrlEncodedFormEntity(toSubmit, Consts.UTF_8));
        response = httpclient.execute(post, context);

        // 5. process autosubmit form for login response
        autosubmitForm = StaxUtils.read(
                new ByteArrayInputStream(EntityUtils.toString(response.getEntity()).replace("&", "&amp;").getBytes()));

        body = find(autosubmitForm.getChildNodes().item(0).getChildNodes(), "BODY").get(0);
        assertNotNull(body);

        form = find(body.getChildNodes(), "FORM").get(0);
        assertNotNull(form);

        toSubmit.clear();
        for (Node input : find(form.getChildNodes(), "INPUT")) {
            Node name = input.getAttributes().getNamedItem("NAME");
            Node value = input.getAttributes().getNamedItem("VALUE");
            if (name != null && value != null) {
                toSubmit.add(new BasicNameValuePair(name.getTextContent(), value.getTextContent()));
            }
        }

        post = new HttpPost(form.getAttributes().getNamedItem("ACTION").getTextContent());
        post.setEntity(new UrlEncodedFormEntity(toSubmit, Consts.UTF_8));
        response = httpclient.execute(post, context);
        assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, response.getStatusLine().getStatusCode());

        // 6. verify that user is now authenticated
        get = new HttpGet(baseURL + StringUtils.removeStart(
                response.getFirstHeader(HttpHeaders.LOCATION).getValue(), "../"));
        response = httpclient.execute(get, context);
        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
        assertTrue(EntityUtils.toString(response.getEntity()).contains("john.doe"));
    }

    @Test
    public void sso2Console() throws IOException, XMLStreamException {
        sso("http://localhost:9080/syncope-console/");
    }

    @Test
    public void sso2Enduser() throws IOException, XMLStreamException {
        sso("http://localhost:9080/syncope-enduser/");
    }
}
