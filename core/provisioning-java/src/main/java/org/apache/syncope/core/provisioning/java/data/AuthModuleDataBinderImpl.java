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
package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.SyncopeClientCompositeException;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.ItemTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModule;
import org.apache.syncope.core.persistence.api.entity.auth.AuthModuleItem;
import org.apache.syncope.core.provisioning.api.data.AuthModuleDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthModuleDataBinderImpl implements AuthModuleDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(AuthModuleDataBinder.class);

    protected final EntityFactory entityFactory;

    public AuthModuleDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    protected void populateItems(final AuthModuleTO authModuleTO, final AuthModule authModule) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
        SyncopeClientException invalidMapping =
                SyncopeClientException.build(ClientExceptionType.InvalidMapping);
        SyncopeClientException requiredValuesMissing =
                SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        authModuleTO.getItems().forEach(itemTO -> {
            if (itemTO == null) {
                LOG.error("Null {}", ItemTO.class.getSimpleName());
                invalidMapping.getElements().add("Null " + ItemTO.class.getSimpleName());
            } else if (itemTO.getIntAttrName() == null) {
                requiredValuesMissing.getElements().add("intAttrName");
                scce.addException(requiredValuesMissing);
            } else {
                // no mandatory condition implies mandatory condition false
                if (!JexlUtils.isExpressionValid(itemTO.getMandatoryCondition() == null
                        ? "false" : itemTO.getMandatoryCondition())) {

                    SyncopeClientException invalidMandatoryCondition =
                            SyncopeClientException.build(ClientExceptionType.InvalidValues);
                    invalidMandatoryCondition.getElements().add(itemTO.getMandatoryCondition());
                    scce.addException(invalidMandatoryCondition);
                }

                AuthModuleItem item = entityFactory.newEntity(AuthModuleItem.class);
                item.setIntAttrName(itemTO.getIntAttrName());
                item.setExtAttrName(itemTO.getExtAttrName());
                item.setMandatoryCondition(itemTO.getMandatoryCondition());
                item.setConnObjectKey(itemTO.isConnObjectKey());
                item.setPassword(itemTO.isPassword());
                item.setPropagationJEXLTransformer(itemTO.getPropagationJEXLTransformer());
                item.setPullJEXLTransformer(itemTO.getPullJEXLTransformer());
                item.setAuthModule(authModule);
                authModule.add(item);
            }
        });

        if (!invalidMapping.getElements().isEmpty()) {
            scce.addException(invalidMapping);
        }
        if (scce.hasExceptions()) {
            throw scce;
        }
    }

    @Override
    public AuthModule create(final AuthModuleTO authModuleTO) {
        AuthModule authModule = entityFactory.newEntity(AuthModule.class);
        authModule.setKey(authModuleTO.getKey());
        return update(authModule, authModuleTO);
    }

    @Override
    public AuthModule update(final AuthModule authModule, final AuthModuleTO authModuleTO) {
        authModule.setDescription(authModuleTO.getDescription());
        authModule.setConf(authModuleTO.getConf());

        authModule.getItems().clear();
        populateItems(authModuleTO, authModule);

        return authModule;
    }

    protected static void populateItems(final AuthModule authModule, final AuthModuleTO authModuleTO) {
        authModule.getItems().forEach(item -> {
            ItemTO itemTO = new ItemTO();
            itemTO.setKey(item.getKey());
            itemTO.setIntAttrName(item.getIntAttrName());
            itemTO.setExtAttrName(item.getExtAttrName());
            itemTO.setMandatoryCondition(item.getMandatoryCondition());
            itemTO.setConnObjectKey(item.isConnObjectKey());
            itemTO.setPassword(item.isPassword());
            itemTO.setPropagationJEXLTransformer(item.getPropagationJEXLTransformer());
            itemTO.setPullJEXLTransformer(item.getPullJEXLTransformer());
            itemTO.setPurpose(MappingPurpose.NONE);

            authModuleTO.add(itemTO);
        });
    }

    @Override
    public AuthModuleTO getAuthModuleTO(final AuthModule authModule) {
        AuthModuleTO authModuleTO = new AuthModuleTO();

        authModuleTO.setKey(authModule.getKey());
        authModuleTO.setDescription(authModule.getDescription());
        authModuleTO.setConf(authModule.getConf());

        populateItems(authModule, authModuleTO);

        return authModuleTO;
    }
}
