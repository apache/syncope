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

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.apache.syncope.core.rest.cxf.batch.BatchProcess;
import org.springframework.stereotype.Service;
import org.apache.syncope.core.persistence.api.dao.BatchDAO;
import org.apache.syncope.core.persistence.api.entity.Batch;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class SyncopeServiceImpl extends AbstractService implements SyncopeService {

    private static final String CONTENT_XML = "Content.xml";

    protected final SyncopeLogic logic;

    protected final ThreadPoolTaskExecutor batchExecutor;

    protected final Bus bus;

    protected final BatchDAO batchDAO;

    protected final EntityFactory entityFactory;

    public SyncopeServiceImpl(
            final SyncopeLogic logic,
            final ThreadPoolTaskExecutor batchExecutor,
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

        Pair<Integer, List<GroupTO>> result = logic.searchAssignableGroups(
                StringUtils.prependIfMissing(realm, SyncopeConstants.ROOT_REALM), term, page, size);
        return buildPagedResult(result.getRight(), page, size, result.getLeft());
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

        if (batchDAO.find(boundary) != null) {
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
        batchProcess.setBasePath(uriInfo.getBaseUri().toASCIIString());
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

        Batch batch = batchDAO.find(boundary);
        if (batch == null) {
            throw new NotFoundException("Batch " + boundary);
        }

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

        batchDAO.delete(boundary);

        return response;
    }

    @Override
    public Response exportInternalStorageContent() {
        StreamingOutput sout = (os) -> logic.exportInternalStorageContent(os);

        return Response.ok(sout).
                type(MediaType.TEXT_XML).
                header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + AuthContextUtils.getDomain() + CONTENT_XML).
                build();
    }
}
