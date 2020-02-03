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
package org.apache.syncope.fit.core.reference;

import java.util.Optional;
import org.apache.syncope.common.lib.request.AnyCR;
import org.apache.syncope.common.lib.request.AnyUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.provisioning.api.pushpull.IgnoreProvisionException;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PullActions;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.quartz.JobExecutionException;

/**
 * Test pull action.
 */
public class TestPullActions implements PullActions {

    private int counter;

    @Override
    public void beforeProvision(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final AnyCR anyCR)
            throws JobExecutionException {

        Optional<Attr> attrTO = anyCR.getPlainAttrs().stream().
                filter(attr -> "fullname".equals(attr.getSchema())).findFirst();
        if (attrTO.isEmpty()) {
            attrTO = Optional.of(new Attr());
            attrTO.get().setSchema("fullname");
            anyCR.getPlainAttrs().add(attrTO.get());
        }
        attrTO.get().getValues().clear();
        attrTO.get().getValues().add(String.valueOf(counter++));
    }

    @Override
    public void beforeAssign(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final AnyCR anyCR)
            throws JobExecutionException {

        if (anyCR instanceof UserCR && "test2".equals(UserCR.class.cast(anyCR).getUsername())) {
            throw new IgnoreProvisionException();
        }
    }

    @Override
    public void beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entityTO,
            final AnyUR anyUR) throws JobExecutionException {

        AttrPatch fullnamePatch = null;
        for (AttrPatch attrPatch : anyUR.getPlainAttrs()) {
            if ("fullname".equals(attrPatch.getAttr().getSchema())) {
                fullnamePatch = attrPatch;
            }
        }
        if (fullnamePatch == null) {
            fullnamePatch = new AttrPatch.Builder(new Attr.Builder("fullname").build()).
                    operation(PatchOperation.ADD_REPLACE).
                    build();
        }

        fullnamePatch.getAttr().getValues().clear();
        fullnamePatch.getAttr().getValues().add(String.valueOf(counter++));
    }
}
