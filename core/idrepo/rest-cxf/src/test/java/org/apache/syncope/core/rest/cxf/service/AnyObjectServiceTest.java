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
package org.apache.syncope.core.rest.cxf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jakarta.rs.xml.JacksonXMLProvider;
import com.fasterxml.jackson.jakarta.rs.yaml.JacksonYAMLProvider;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.cxf.transport.local.LocalConduit;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.EntityTOUtils;
import org.apache.syncope.common.lib.jackson.SyncopeJsonMapper;
import org.apache.syncope.common.lib.jackson.SyncopeXmlMapper;
import org.apache.syncope.common.lib.jackson.SyncopeYAMLMapper;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.rest.api.DateParamConverterProvider;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.core.logic.AnyObjectLogic;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.api.search.SyncopePage;
import org.apache.syncope.core.rest.cxf.AddETagFilter;
import org.apache.syncope.core.rest.cxf.RestServiceExceptionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;

@SpringJUnitConfig(classes = { IdRepoRESTCXFTestContext.class })
public class AnyObjectServiceTest {

    private static final String LOCAL_ADDRESS = "local://anyObjects";

    private static Server SERVER;

    @Autowired
    private DateParamConverterProvider dateParamConverterProvider;

    @Autowired
    private JacksonJsonProvider jsonProvider;

    @Autowired
    private JacksonXMLProvider xmlProvider;

    @Autowired
    private JacksonYAMLProvider yamlProvider;

    @Autowired
    private RestServiceExceptionMapper exceptionMapper;

    @Autowired
    private JAXRSBeanValidationInInterceptor validationInInterceptor;

    @Autowired
    private GZIPInInterceptor gzipInInterceptor;

    @Autowired
    private GZIPOutInterceptor gzipOutInterceptor;

    @Autowired
    private SearchContextProvider searchContextProvider;

    @Autowired
    private AddETagFilter addETagFilter;

    @BeforeEach
    public void setup() {
        if (SERVER == null) {
            AnyObjectDAO anyObjectDAO = mock(AnyObjectDAO.class);

            AnyObjectLogic logic = mock(AnyObjectLogic.class);
            when(logic.search(
                    any(SearchCond.class), any(Pageable.class), anyString(), anyBoolean(), anyBoolean())).
                    thenAnswer(ic -> {
                        AnyObjectTO printer1 = new AnyObjectTO();
                        printer1.setKey(UUID.randomUUID().toString());
                        printer1.setName("printer1");
                        printer1.setType("PRINTER");
                        printer1.getPlainAttrs().add(new Attr.Builder("location").value("here").build());

                        AnyObjectTO printer2 = new AnyObjectTO();
                        printer2.setKey(UUID.randomUUID().toString());
                        printer2.setName("printer2");
                        printer2.setType("PRINTER");
                        printer2.getPlainAttrs().add(new Attr.Builder("location").value("there").build());

                        return new SyncopePage<>(List.of(printer1, printer2), ic.getArgument(1), 2);
                    });
            when(logic.create(any(AnyObjectCR.class), anyBoolean())).thenAnswer(ic -> {
                AnyObjectTO anyObjectTO = new AnyObjectTO();
                EntityTOUtils.toAnyTO(ic.getArgument(0), anyObjectTO);
                anyObjectTO.setKey(UUID.randomUUID().toString());

                ProvisioningResult<AnyObjectTO> result = new ProvisioningResult<>();
                result.setEntity(anyObjectTO);
                return result;
            });

            SearchCondVisitor searchCondVisitor = mock(SearchCondVisitor.class);
            when(searchCondVisitor.getQuery()).thenReturn(new SearchCond());

            @SuppressWarnings("unchecked")
            SearchCondition<SearchBean> sc = mock(SearchCondition.class);
            doNothing().when(sc).accept(any());
            SearchContext searchContext = mock(SearchContext.class);
            when(searchContext.getCondition(anyString(), eq(SearchBean.class))).thenReturn(sc);

            UriInfo uriInfo = mock(UriInfo.class);
            when(uriInfo.getAbsolutePathBuilder()).thenReturn(new UriBuilderImpl());
            when(uriInfo.getQueryParameters()).thenReturn(new MetadataMap<>());

            MessageContext messageContext = mock(MessageContext.class);
            MockHttpServletRequest httpRequest = new MockHttpServletRequest();
            httpRequest.addHeader(RESTHeaders.NULL_PRIORITY_ASYNC, "false");
            when(messageContext.getHttpServletRequest()).thenReturn(httpRequest);
            when(messageContext.getHttpServletResponse()).thenReturn(new MockHttpServletResponse());

            Request request = mock(Request.class);
            when(request.evaluatePreconditions(any(Date.class))).thenReturn(Response.notModified());
            when(messageContext.getRequest()).thenReturn(request);

            AnyObjectServiceImpl service = new AnyObjectServiceImpl(searchCondVisitor, anyObjectDAO, logic);
            ReflectionTestUtils.setField(service, "searchContext", searchContext);
            ReflectionTestUtils.setField(service, "uriInfo", uriInfo);
            ReflectionTestUtils.setField(service, "messageContext", messageContext);

            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setAddress(LOCAL_ADDRESS);
            sf.setResourceClasses(AnyObjectService.class);
            sf.setResourceProvider(
                    AnyObjectService.class,
                    new SingletonResourceProvider(service, true));

            sf.setInInterceptors(List.of(gzipInInterceptor, validationInInterceptor));
            sf.setOutInterceptors(List.of(gzipOutInterceptor));

            sf.setProviders(List.of(dateParamConverterProvider, jsonProvider, xmlProvider, yamlProvider,
                    exceptionMapper, searchContextProvider, addETagFilter));

            SERVER = sf.create();
        }

        assertNotNull(SERVER);
    }

    private WebClient client(final MediaType mediaType) {
        WebClient client = WebClient.create(LOCAL_ADDRESS, List.of(
                dateParamConverterProvider, jsonProvider, xmlProvider, yamlProvider));
        WebClient.getConfig(client).getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);
        return client.accept(mediaType).type(mediaType).path("anyObjects");
    }

    private InputStream list(final MediaType mediaType) {
        Response response = client(mediaType).
                query("fiql", "$type==PRINTER").
                query("page", "1").
                query("size", "10").
                get();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        return (InputStream) response.getEntity();
    }

    private void checkList(final PagedResult<AnyObjectTO> list) {
        assertEquals(2, list.getTotalCount());
        assertEquals(2, list.getResult().size());

        assertEquals("printer1", list.getResult().get(0).getName());
        assertEquals("PRINTER", list.getResult().get(0).getType());

        Attr location = list.getResult().get(1).getPlainAttr("location").orElse(null);
        assertNotNull(location);
        assertEquals("there", location.getValues().getFirst());
    }

    @Test
    public void jsonList() throws IOException {
        InputStream in = list(MediaType.APPLICATION_JSON_TYPE);

        PagedResult<AnyObjectTO> list = new SyncopeJsonMapper().
                readValue(IOUtils.toString(in), new TypeReference<>() {
                });
        checkList(list);
    }

    @Test
    public void xmlList() throws IOException {
        InputStream in = list(MediaType.APPLICATION_XML_TYPE);

        PagedResult<AnyObjectTO> list = new SyncopeXmlMapper().
                readValue(IOUtils.toString(in), new TypeReference<>() {
                });
        checkList(list);
    }

    @Test
    public void yamlList() throws IOException {
        InputStream in = list(RESTHeaders.APPLICATION_YAML_TYPE);

        PagedResult<AnyObjectTO> list = new SyncopeYAMLMapper().
                readValue(IOUtils.toString(in), new TypeReference<>() {
                });
        checkList(list);
    }

    private void create(final MediaType mediaType) {
        AnyObjectCR newPrinter = new AnyObjectCR();
        newPrinter.setName("newPrinter");
        newPrinter.setType("PRINTER");
        newPrinter.getPlainAttrs().add(new Attr.Builder("location").value("new").build());

        Response response = client(mediaType).post(newPrinter);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(response.getHeaderString(RESTHeaders.RESOURCE_KEY));
    }

    @Test
    public void jsonCreate() {
        create(MediaType.APPLICATION_JSON_TYPE);
    }

    @Test
    public void xmlCreate() {
        create(MediaType.APPLICATION_XML_TYPE);
    }

    @Test
    public void yamlCreate() {
        create(RESTHeaders.APPLICATION_YAML_TYPE);
    }
}
