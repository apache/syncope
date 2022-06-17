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

import org.apache.syncope.common.lib.to.AttrRepoTO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.AttrRepo;
import org.apache.syncope.core.provisioning.api.data.AttrRepoDataBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AttrRepoDataBinderImpl implements AttrRepoDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(AttrRepoDataBinder.class);

    protected final EntityFactory entityFactory;

    public AttrRepoDataBinderImpl(final EntityFactory entityFactory) {
        this.entityFactory = entityFactory;
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

        return attrRepo;
    }

    @Override
    public AttrRepoTO getAttrRepoTO(final AttrRepo attrRepo) {
        AttrRepoTO attrRepoTO = new AttrRepoTO();

        attrRepoTO.setKey(attrRepo.getKey());
        attrRepoTO.setDescription(attrRepo.getDescription());
        attrRepoTO.setState(attrRepo.getState());
        attrRepoTO.setOrder(attrRepo.getOrder());
        attrRepoTO.setConf(attrRepo.getConf());

        return attrRepoTO;
    }
}
