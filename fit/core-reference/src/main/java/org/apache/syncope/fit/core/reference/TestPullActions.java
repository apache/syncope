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
import org.apache.syncope.common.lib.patch.AnyPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.UserTO;
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
    public SyncDelta beforeProvision(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity)
            throws JobExecutionException {

        if (entity instanceof AnyTO) {
            AnyTO any = (AnyTO) entity;

            Optional<AttrTO> attrTO = any.getPlainAttr("fullname");
            if (!attrTO.isPresent()) {
                attrTO = Optional.of(new AttrTO());
                attrTO.get().setSchema("fullname");
                any.getPlainAttrs().add(attrTO.get());
            }
            attrTO.get().getValues().clear();
            attrTO.get().getValues().add(String.valueOf(counter++));
        }

        return delta;
    }

    @Override
    public SyncDelta beforeAssign(
            final ProvisioningProfile<?, ?> profile, final SyncDelta delta, final EntityTO entity)
            throws JobExecutionException {

        if (entity instanceof UserTO && "test2".equals(UserTO.class.cast(entity).getUsername())) {
            throw new IgnoreProvisionException();
        }

        return delta;
    }

    @Override
    public <M extends AnyPatch> SyncDelta beforeUpdate(
            final ProvisioningProfile<?, ?> profile,
            final SyncDelta delta,
            final EntityTO entityTO,
            final M anyPatch) throws JobExecutionException {

        AttrPatch fullnamePatch = null;
        for (AttrPatch attrPatch : anyPatch.getPlainAttrs()) {
            if ("fullname".equals(attrPatch.getAttrTO().getSchema())) {
                fullnamePatch = attrPatch;
            }
        }
        if (fullnamePatch == null) {
            fullnamePatch = new AttrPatch.Builder().
                    operation(PatchOperation.ADD_REPLACE).
                    attrTO(new AttrTO.Builder().schema("fullname").build()).
                    build();
        }

        fullnamePatch.getAttrTO().getValues().clear();
        fullnamePatch.getAttrTO().getValues().add(String.valueOf(counter++));

        return delta;
    }
}
