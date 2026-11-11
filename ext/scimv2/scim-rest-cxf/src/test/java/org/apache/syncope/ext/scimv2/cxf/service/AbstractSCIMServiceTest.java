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
package org.apache.syncope.ext.scimv2.cxf.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMGeneralConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.scim.SCIMUserNameConf;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.logic.AnyObjectLogicOp;
import org.apache.syncope.core.logic.GroupLogicOp;
import org.apache.syncope.core.logic.SCIMDataBinder;
import org.apache.syncope.core.logic.UserLogicOp;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.SCIMResource;
import org.apache.syncope.ext.scimv2.api.data.SCIMSearchRequest;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class AbstractSCIMServiceTest {

    private static final int MAX_RESULTS = 5;

    private static class TestSCIMService extends AbstractSCIMService<SCIMResource> {

        TestSCIMService(
                final UserDAO userDAO,
                final GroupDAO groupDAO,
                final AnyObjectDAO anyObjectDAO,
                final UserLogicOp userLogic,
                final GroupLogicOp groupLogic,
                final AnyObjectLogicOp anyObjectLogic,
                final SCIMDataBinder binder,
                final SCIMConfManager confManager) {

            super(userDAO, groupDAO, anyObjectDAO, userLogic, groupLogic, anyObjectLogic, binder, confManager);
        }

        @Override
        protected SCIMResource getResource(final String key) {
            return null;
        }
    }

    private UserLogicOp userLogic;

    private AnyObjectLogicOp anyObjectLogic;

    private SCIMDataBinder binder;

    private UriInfo uriInfo;

    private TestSCIMService service;

    @BeforeEach
    void setup() {
        SCIMConf conf = new SCIMConf();
        conf.setGeneralConf(new SCIMGeneralConf());
        conf.getGeneralConf().setFilterMaxResults(MAX_RESULTS);
        conf.setUserConf(new SCIMUserConf());
        conf.getUserConf().setName(new SCIMUserNameConf());
        conf.getUserConf().getName().setFamilyName("surname");

        SCIMConfManager confManager = mock(SCIMConfManager.class);
        when(confManager.get()).thenReturn(conf);

        userLogic = mock(UserLogicOp.class);
        anyObjectLogic = mock(AnyObjectLogicOp.class);
        binder = mock(SCIMDataBinder.class);
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(UriBuilder.fromPath("http://localhost/Users"));

        service = new TestSCIMService(
                mock(UserDAO.class),
                mock(GroupDAO.class),
                mock(AnyObjectDAO.class),
                userLogic,
                mock(GroupLogicOp.class),
                anyObjectLogic,
                binder,
                confManager);
        service.uriInfo = uriInfo;
    }

    @Test
    void unsupportedType() {
        SCIMSearchRequest request = new SCIMSearchRequest(null, null, null, 1, MAX_RESULTS);

        assertThrows(UnsupportedOperationException.class, () -> service.doSearch(null, request));
    }

    @Test
    void tooManyResultsRequested() {
        SCIMSearchRequest request = new SCIMSearchRequest(null, null, null, 1, MAX_RESULTS + 1);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.doSearch(Resource.User.name(), request));
        assertEquals(ErrorType.tooMany, ex.getErrorType());
    }

    @Test
    void unsupportedStartIndex() {
        SCIMSearchRequest request = new SCIMSearchRequest(null, null, null, 2, MAX_RESULTS);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.doSearch(Resource.User.name(), request));
        assertEquals(ErrorType.invalidValue, ex.getErrorType());
    }

    @Test
    void tooManyResultsFound() {
        when(userLogic.search(any(), any(), any(), anyBoolean(), anyBoolean())).
                thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(MAX_RESULTS), MAX_RESULTS + 1L));

        SCIMSearchRequest request = new SCIMSearchRequest(null, null, null, 1, MAX_RESULTS);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.doSearch(Resource.User.name(), request));
        assertEquals(ErrorType.tooMany, ex.getErrorType());
    }

    @Test
    void searchUsers() {
        UserTO userTO = new UserTO();
        userTO.setKey("1");
        Page<UserTO> page = new PageImpl<>(List.of(userTO), Pageable.ofSize(MAX_RESULTS), 1);
        when(userLogic.search(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(page);

        when(binder.toSCIMUser(any(), any(), any(), any())).thenReturn(new SCIMUser(
                "1", List.of(Resource.User.schema()), null, "username", true));

        SCIMSearchRequest request = new SCIMSearchRequest(
                null, "name.familyName", SortOrder.descending, 1, MAX_RESULTS);

        ListResponse<SCIMResource> response = service.doSearch(Resource.User.name(), request);

        assertEquals(1, response.getTotalResults());
        assertEquals(1, response.getStartIndex());
        assertEquals(MAX_RESULTS, response.getItemsPerPage());
        assertEquals(1, response.getResources().size());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userLogic).search(
                any(),
                pageableCaptor.capture(),
                eq(SyncopeConstants.ROOT_REALM),
                eq(true),
                eq(true));

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("surname");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
        assertEquals(0, pageableCaptor.getValue().getPageNumber());
    }

    @Test
    void searchAnyObjects() {
        Page<AnyObjectTO> page = new PageImpl<>(List.of(), Pageable.ofSize(MAX_RESULTS), 0);
        when(anyObjectLogic.search(any(), any(), any(), anyBoolean(), anyBoolean())).thenReturn(page);

        SCIMSearchRequest request = new SCIMSearchRequest("", null, null, 1, MAX_RESULTS);

        ListResponse<SCIMResource> response = service.doSearch("other", request);

        assertEquals(0, response.getTotalResults());
        assertEquals(1, response.getStartIndex());
        assertEquals(MAX_RESULTS, response.getItemsPerPage());

        ArgumentCaptor<SearchCond> condCaptor = ArgumentCaptor.forClass(SearchCond.class);
        verify(anyObjectLogic).search(
                condCaptor.capture(),
                any(),
                eq(SyncopeConstants.ROOT_REALM),
                eq(true),
                eq(true));

        assertTrue(condCaptor.getValue().asLeaf(AnyTypeCond.class).isPresent());
        assertEquals("other", condCaptor.getValue().asLeaf(AnyTypeCond.class).orElseThrow().getAnyTypeKey());
    }
}
