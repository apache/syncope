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
package org.apache.syncope.fit.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import javax.ws.rs.core.MediaType;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.UserRequestForm;
import org.apache.syncope.common.lib.to.UserRequestTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.rest.api.beans.UserRequestFormQuery;
import org.apache.syncope.fit.AbstractITCase;
import org.apache.syncope.fit.FlowableDetector;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class UserRequestITCase extends AbstractITCase {

    @BeforeAll
    public static void loadBpmnProcesses() throws IOException {
        WebClient.client(bpmnProcessService).type(MediaType.APPLICATION_XML_TYPE);
        bpmnProcessService.set("directorGroupRequest",
                IOUtils.toString(UserRequestITCase.class.getResourceAsStream("/directorGroupRequest.bpmn20.xml")));
    }

    @Test
    public void twoLevelsApproval() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(syncopeService));

        UserTO user = createUser(UserITCase.getUniqueSampleTO("twoLevelsApproval@tirasa.net")).getEntity();
        assertNotNull(user);
        assertFalse(user.getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request
        UserRequestTO req = userRequestService.start("directorGroupRequest", user.getKey());
        assertNotNull(req);

        // 1st approval -> reject
        UserRequestForm form = userRequestService.getForms(user.getKey()).get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("firstLevelApprove").get().setValue(Boolean.FALSE.toString());
        userRequestService.submitForm(form);

        // no more forms, group not assigned
        assertTrue(userRequestService.getForms(user.getKey()).isEmpty());
        assertFalse(userService.read(user.getKey()).getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request again
        req = userRequestService.start("directorGroupRequest", user.getKey());
        assertNotNull(req);

        // 1st approval -> accept
        form = userRequestService.getForms(user.getKey()).get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("firstLevelApprove").get().setValue(Boolean.TRUE.toString());
        userRequestService.submitForm(form);

        // 2nd approval -> reject
        form = userRequestService.getForms(user.getKey()).get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("secondLevelApprove").get().setValue(Boolean.FALSE.toString());
        user = userRequestService.submitForm(form);

        // no more forms, group not assigned
        assertTrue(userRequestService.getForms(user.getKey()).isEmpty());
        assertFalse(userService.read(user.getKey()).getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request again
        req = userRequestService.start("directorGroupRequest", user.getKey());
        assertNotNull(req);

        // 1st approval -> accept
        form = userRequestService.getForms(user.getKey()).get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("firstLevelApprove").get().setValue(Boolean.TRUE.toString());
        userRequestService.submitForm(form);

        // 2nd approval -> accept
        form = userRequestService.getForms(user.getKey()).get(0);
        form = userRequestService.claimForm(form.getTaskId());
        form.getProperty("secondLevelApprove").get().setValue(Boolean.TRUE.toString());
        user = userRequestService.submitForm(form);

        // check that the director group was effectively assigned
        assertTrue(user.getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());
        assertTrue(userService.read(user.getKey()).getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());
    }

    @Test
    public void cancel() {
        assumeTrue(FlowableDetector.isFlowableEnabledForUserWorkflow(syncopeService));

        PagedResult<UserRequestForm> forms =
                userRequestService.getForms(new UserRequestFormQuery.Builder().page(1).size(1000).build());
        int preForms = forms.getTotalCount();

        UserTO user = createUser(UserITCase.getUniqueSampleTO("twoLevelsApproval@tirasa.net")).getEntity();
        assertNotNull(user);
        assertFalse(user.getMembership("ebf97068-aa4b-4a85-9f01-680e8c4cf227").isPresent());

        // start request
        UserRequestTO req = userRequestService.start("directorGroupRequest", user.getKey());
        assertNotNull(req);

        // check that form was generated
        forms = userRequestService.getForms(new UserRequestFormQuery.Builder().page(1).size(1000).build());
        assertEquals(preForms + 1, forms.getTotalCount());

        assertEquals(1, userRequestService.getForms(user.getKey()).size());

        // cancel request
        userRequestService.cancel(req.getExecutionId(), "nothing in particular");

        // check that form was removed
        forms = userRequestService.getForms(new UserRequestFormQuery.Builder().page(1).size(1000).build());
        assertEquals(preForms, forms.getTotalCount());

        assertTrue(userRequestService.getForms(user.getKey()).isEmpty());
    }
}
