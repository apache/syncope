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
package org.syncope.core.scheduling;

import java.util.Collections;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;
import org.syncope.client.mod.AttributeMod;
import org.syncope.client.mod.UserMod;
import org.syncope.client.to.AttributeTO;
import org.syncope.client.to.UserTO;

public class TestSyncJobActions extends DefaultSyncJobActions {

    private int counter = 0;

    @Override
    public void beforeCreate(final SyncDelta delta, final UserTO user)
            throws JobExecutionException {

        AttributeTO attrTO = null;
        for (int i = 0; i < user.getAttributes().size(); i++) {
            if ("fullname".equals(user.getAttributes().get(i).getSchema())) {
                attrTO = user.getAttributes().get(i);
            }
        }
        if (attrTO == null) {
            attrTO = new AttributeTO();
            attrTO.setSchema("fullname");
            user.addAttribute(attrTO);
        }
        attrTO.setValues(Collections.singletonList(String.valueOf(counter++)));
    }

    @Override
    public void beforeUpdate(final SyncDelta delta, final UserTO user,
            final UserMod userMod)
            throws JobExecutionException {

        userMod.addAttributeToBeRemoved("fullname");

        AttributeMod fullnameMod = null;
        for (AttributeMod attrMod : userMod.getAttributesToBeUpdated()) {
            if ("fullname".equals(attrMod.getSchema())) {
                fullnameMod = attrMod;
            }
        }
        if (fullnameMod == null) {
            fullnameMod = new AttributeMod();
            fullnameMod.setSchema("fullname");
            userMod.addAttributeToBeUpdated(fullnameMod);
        }

        fullnameMod.setValuesToBeAdded(
                Collections.singletonList(String.valueOf(counter++)));
    }
}
