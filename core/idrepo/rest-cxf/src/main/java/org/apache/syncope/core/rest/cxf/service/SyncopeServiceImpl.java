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

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPTransportFactory;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.TypeExtensionTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchPayloadParser;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.core.logic.SyncopeLogic;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.BatchDAO;
import org.apache.syncope.core.persistence.api.entity.Batch;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.rest.cxf.batch.BatchProcess;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.task.VirtualThreadPoolTaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;

public class SyncopeServiceImpl extends AbstractService implements SyncopeService {

    protected final SyncopeLogic logic;

    protected final VirtualThreadPoolTaskExecutor batchExecutor;

    protected final Bus bus;

    protected final BatchDAO batchDAO;

    protected final EntityFactory entityFactory;

    public SyncopeServiceImpl(
            final SyncopeLogic logic,
            final VirtualThreadPoolTaskExecutor batchExecutor,
            final Bus bus,
            final BatchDAO batchDAO,
            final EntityFactory entityFactory) {

        this.logic = logic;
        this.batchExecutor = batchExecutor;
        this.bus = bus;
        this.batchDAO = batchDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public PagedResult<GroupTO> searchAssignableGroups(
            final String realm, final String term, final int page, final int size) {

        Page<GroupTO> result = logic.searchAssignableGroups(
                StringUtils.prependIfMissing(realm, SyncopeConstants.ROOT_REALM),
                term,
                PageRequest.of(page < 1 ? 0 : page - 1, size < 1 ? 1 : size, Sort.by(Sort.Direction.ASC, "name")));
        return buildPagedResult(result);
    }

    @Override
    public TypeExtensionTO readUserTypeExtension(final String groupName) {
        return logic.readTypeExtension(groupName);
    }

    private DestinationRegistry getDestinationRegistryFromBusOrDefault() {
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        try {
            HTTPTransportFactory df = (HTTPTransportFactory) dfm.
                    getDestinationFactory("http://cxf.apache.org/transports/http/configuration");
            return df.getRegistry();
        } catch (Exception e) {
            throw new InternalServerErrorException("Could not find CXF's DestinationRegistry", e);
        }
    }

    @Override
    public Response batch(final InputStream input) {
        // parse Content-Type, expect appropriate boundary
        MediaType mediaType = MediaType.valueOf(messageContext.getHttpServletRequest().getContentType());
        String boundary = mediaType.getParameters().get(RESTHeaders.BOUNDARY_PARAMETER);

        if (batchDAO.existsById(boundary)) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.EntityExists);
            sce.getElements().add("Batch with boundary " + boundary + " already processing");
            throw sce;
        }

        // parse batch request
        List<BatchRequestItem> batchRequestItems;
        try {
            batchRequestItems = BatchPayloadParser.parse(input, mediaType, new BatchRequestItem());
        } catch (IOException e) {
            LOG.error("Could not parse batch request with boundary {}", boundary, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidEntity);
            sce.getElements().add("Batch request with boundary " + boundary);
            throw sce;
        }

        // prepare for batch processing
        Batch batch = entityFactory.newEntity(Batch.class);
        batch.setKey(boundary);
        batch.setExpiryTime(OffsetDateTime.now().plusMinutes(5));
        batchDAO.save(batch);

        BatchProcess batchProcess = ApplicationContextProvider.getBeanFactory().createBean(BatchProcess.class);
        batchProcess.setBoundary(boundary);
        batchProcess.setScheme(messageContext.getHttpServletRequest().getScheme());
        batchProcess.setServerName(messageContext.getHttpServletRequest().getServerName());
        batchProcess.setServerPort(messageContext.getHttpServletRequest().getServerPort());
        batchProcess.setContextPath(messageContext.getHttpServletRequest().getContextPath());
        batchProcess.setServletPath(messageContext.getHttpServletRequest().getServletPath());
        batchProcess.setPathInfo(messageContext.getHttpServletRequest().getPathInfo());
        batchProcess.setCharacterEncoding(messageContext.getHttpServletRequest().getCharacterEncoding());
        batchProcess.setBaseURI(uriInfo.getBaseUri().toASCIIString());
        batchProcess.setBatchRequestItems(batchRequestItems);
        batchProcess.setDestinationRegistry(getDestinationRegistryFromBusOrDefault());
        batchProcess.setServletConfig(messageContext.getServletConfig());
        batchProcess.setServletRequest(messageContext.getHttpServletRequest());
        batchProcess.setAuthentication(SecurityContextHolder.getContext().getAuthentication());

        // manage synchronous Vs asynchronous batch processing
        if (getPreference() == Preference.RESPOND_ASYNC) {
            batchExecutor.execute(batchProcess);

            return Response.accepted().
                    header(RESTHeaders.PREFERENCE_APPLIED, getPreference().toString()).
                    header(HttpHeaders.LOCATION, uriInfo.getAbsolutePathBuilder().build()).
                    type(RESTHeaders.multipartMixedWith(boundary)).
                    build();
        } else {
            batchProcess.run();
            return batch();
        }
    }

    @Override
    public Response batch() {
        MediaType mediaType = MediaType.valueOf(messageContext.getHttpServletRequest().getContentType());
        String boundary = mediaType.getParameters().get(RESTHeaders.BOUNDARY_PARAMETER);

        Batch batch = batchDAO.findById(boundary).
                orElseThrow(() -> new NotFoundException("Batch " + boundary));

        if (batch.getResults() == null) {
            return Response.accepted().
                    type(RESTHeaders.multipartMixedWith(boundary)).
                    header(HttpHeaders.RETRY_AFTER, 5).
                    header(HttpHeaders.LOCATION, uriInfo.getAbsolutePathBuilder().build()).
                    build();
        }

        Response response = Response.ok(batch.getResults()).
                type(RESTHeaders.multipartMixedWith(boundary)).
                build();

        batchDAO.deleteById(boundary);

        return response;
    }

    @Override
    public Response exportInternalStorageContent(final int threshold, final List<String> elements) {
        StreamingOutput sout = os -> logic.exportInternalStorageContent(threshold, os, elements);

        return Response.ok(sout).
                type(MediaType.TEXT_XML).
                header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + AuthContextUtils.getDomain() + "Content.xml").
                build();
    }
}
