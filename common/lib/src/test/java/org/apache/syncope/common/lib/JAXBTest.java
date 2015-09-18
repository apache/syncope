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

import static org.junit.Assert.fail;

import java.io.StringWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.report.UserReportletConf;
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
}
