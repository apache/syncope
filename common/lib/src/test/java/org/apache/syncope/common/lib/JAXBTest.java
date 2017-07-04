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
package org.apache.syncope.common.lib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.UUID;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.report.UserReportletConf;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.Test;

public class JAXBTest {

    @Test
    public void marshal() {
        try {
            JAXBContext context = JAXBContext.newInstance(UserTO.class, UserPatch.class, UserReportletConf.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(new UserTO(), new StringWriter());
            marshaller.marshal(new UserPatch(), new StringWriter());
        } catch (Exception e) {
            fail(ExceptionUtils.getStackTrace(e));
        }
    }

    @Test
    public void provisioningResult() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(ProvisioningResult.class);
        Marshaller marshaller = context.createMarshaller();
        Unmarshaller unmarshaller = context.createUnmarshaller();

        GroupTO group = new GroupTO();
        group.setName(UUID.randomUUID().toString());
        group.setRealm(SyncopeConstants.ROOT_REALM);
        group.getVirAttrs().add(new AttrTO.Builder().schema("rvirtualdata").value("rvirtualvalue").build());
        group.getADynMembershipConds().put("USER", "username==a*");

        ProvisioningResult<GroupTO> original = new ProvisioningResult<>();
        original.setEntity(group);

        PropagationStatus status = new PropagationStatus();
        status.setFailureReason("failed");
        status.setBeforeObj(new ConnObjectTO());
        original.getPropagationStatuses().add(status);

        StringWriter writer = new StringWriter();
        marshaller.marshal(original, writer);

        Object actual = unmarshaller.unmarshal(new StringReader(writer.toString()));
        assertEquals(original, actual);
    }

    @Test
    public void numbersInfo() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(NumbersInfo.class);
        Marshaller marshaller = context.createMarshaller();
        Unmarshaller unmarshaller = context.createUnmarshaller();

        NumbersInfo original = new NumbersInfo();
        original.setTotalUsers(5);
        original.getUsersByRealm().put("/", 4);
        original.getUsersByRealm().put("/even", 1);
        original.getUsersByStatus().put("active", 5);
        original.setTotalGroups(16);
        original.getGroupsByRealm().put("/", 14);
        original.getGroupsByRealm().put("/even", 1);
        original.getGroupsByRealm().put("/odd", 1);
        original.setAnyType1("PRINTER");
        original.setTotalAny1(3);
        original.getAny1ByRealm().put("/", 2);
        original.getAny1ByRealm().put("/even/two", 1);
        original.setTotalResources(21);
        original.setTotalRoles(4);
        original.getConfCompleteness().put(NumbersInfo.ConfItem.ANY_TYPE.name(), Boolean.TRUE);
        original.getConfCompleteness().put(NumbersInfo.ConfItem.PULL_TASK.name(), Boolean.TRUE);
        original.getConfCompleteness().put(NumbersInfo.ConfItem.ROLE.name(), Boolean.TRUE);
        original.getConfCompleteness().put(NumbersInfo.ConfItem.NOTIFICATION.name(), Boolean.TRUE);
        original.getConfCompleteness().put(NumbersInfo.ConfItem.PASSWORD_POLICY.name(), Boolean.TRUE);
        original.getConfCompleteness().put(NumbersInfo.ConfItem.ACCOUNT_POLICY.name(), Boolean.TRUE);
        original.getConfCompleteness().put(NumbersInfo.ConfItem.RESOURCE.name(), Boolean.TRUE);
        original.getConfCompleteness().put(NumbersInfo.ConfItem.VIR_SCHEMA.name(), Boolean.TRUE);
        original.getConfCompleteness().put(NumbersInfo.ConfItem.SECURITY_QUESTION.name(), Boolean.TRUE);

        StringWriter writer = new StringWriter();
        marshaller.marshal(original, writer);

        Object actual = unmarshaller.unmarshal(new StringReader(writer.toString()));
        assertEquals(original, actual);
    }
}
