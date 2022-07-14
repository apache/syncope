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
import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.AttrRepo;
import org.apache.syncope.core.provisioning.api.data.AttrRepoDataBinder;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttrRepoDataBinderImpl implements AttrRepoDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(AttrRepoDataBinder.class);

    protected final EntityFactory entityFactory;

    public AttrRepoDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
    }

    protected void populateItems(final AttrRepoTO attrRepoTO, final AttrRepo attrRepo) {
        SyncopeClientCompositeException scce = SyncopeClientException.buildComposite();
        SyncopeClientException invalidMapping =
                SyncopeClientException.build(ClientExceptionType.InvalidMapping);
        SyncopeClientException requiredValuesMissing =
                SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        attrRepoTO.getItems().forEach(itemTO -> {
            if (itemTO == null) {
                LOG.error("Null {}", Item.class.getSimpleName());
                invalidMapping.getElements().add("Null " + Item.class.getSimpleName());
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

                Item item = new Item();
                item.setIntAttrName(itemTO.getIntAttrName());
                item.setExtAttrName(itemTO.getExtAttrName());
                item.setMandatoryCondition(itemTO.getMandatoryCondition());
                item.setConnObjectKey(itemTO.isConnObjectKey());
                item.setPassword(itemTO.isPassword());
                item.setPropagationJEXLTransformer(itemTO.getPropagationJEXLTransformer());
                item.setPullJEXLTransformer(itemTO.getPullJEXLTransformer());
                attrRepo.getItems().add(item);
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
    public AttrRepo create(final AttrRepoTO attrRepoTO) {
        AttrRepo attrRepo = entityFactory.newEntity(AttrRepo.class);
        attrRepo.setKey(attrRepoTO.getKey());
        return update(attrRepo, attrRepoTO);
    }

    @Override
    public AttrRepo update(final AttrRepo attrRepo, final AttrRepoTO attrRepoTO) {
        attrRepo.setDescription(attrRepoTO.getDescription());
        attrRepo.setState(attrRepoTO.getState());
        attrRepo.setOrder(attrRepoTO.getOrder());
        attrRepo.setConf(attrRepoTO.getConf());

        attrRepo.getItems().clear();
        populateItems(attrRepoTO, attrRepo);

        return attrRepo;
    }

    protected void populateItems(final AttrRepo attrRepo, final AttrRepoTO attrRepoTO) {
        attrRepo.getItems().forEach(item -> {
            Item itemTO = new Item();
            itemTO.setIntAttrName(item.getIntAttrName());
            itemTO.setExtAttrName(item.getExtAttrName());
            itemTO.setMandatoryCondition(item.getMandatoryCondition());
            itemTO.setConnObjectKey(item.isConnObjectKey());
            itemTO.setPassword(item.isPassword());
            itemTO.setPropagationJEXLTransformer(item.getPropagationJEXLTransformer());
            itemTO.setPullJEXLTransformer(item.getPullJEXLTransformer());
            itemTO.setPurpose(MappingPurpose.NONE);

            attrRepoTO.getItems().add(itemTO);
        });
    }

    @Override
    public AttrRepoTO getAttrRepoTO(final AttrRepo attrRepo) {
        AttrRepoTO attrRepoTO = new AttrRepoTO();

        attrRepoTO.setKey(attrRepo.getKey());
        attrRepoTO.setDescription(attrRepo.getDescription());
        attrRepoTO.setState(attrRepo.getState());
        attrRepoTO.setOrder(attrRepo.getOrder());
        attrRepoTO.setConf(attrRepo.getConf());

        populateItems(attrRepo, attrRepoTO);

        return attrRepoTO;
    }
}
