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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.info.NumbersInfo;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.report.UserReportletConf;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.junit.jupiter.api.Test;

public class JAXBTest {

    @Test
    public void marshal() {
        try {
            JAXBContext context = JAXBContext.newInstance(UserTO.class, UserUR.class, UserReportletConf.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(new UserTO(), new StringWriter());
            marshaller.marshal(new UserUR(), new StringWriter());
        } catch (JAXBException e) {
            fail(() -> ExceptionUtils.getStackTrace(e));
        }
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

    @Test
    public void issueSYNCOPE1541() throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(ExecTO.class);
        Marshaller marshaller = context.createMarshaller();

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.set(Calendar.MILLISECOND, 0);

        ExecTO exec = new ExecTO();
        exec.setStart(cal.getTime());

        StringWriter writer = new StringWriter();
        marshaller.marshal(exec, writer);

        assertTrue(writer.toString().contains(".000"));
    }
}
