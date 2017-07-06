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
package org.apache.syncope.core.persistence.jpa.entity;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ConnInstanceHistoryConf;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@Entity
@Table(name = JPAConnInstanceHistoryConf.TABLE)
public class JPAConnInstanceHistoryConf
        extends AbstractHistoryConf<ConnInstance, ConnInstanceTO> implements ConnInstanceHistoryConf {

    private static final long serialVersionUID = -4152915369607435186L;

    public static final String TABLE = "ConnInstanceHistoryConf";

    @OneToOne
    private JPAConnInstance entity;

    @Override
    public ConnInstance getEntity() {
        return entity;
    }

    @Override
    public void setEntity(final ConnInstance entity) {
        checkType(entity, JPAConnInstance.class);
        this.entity = (JPAConnInstance) entity;
    }

    @Override
    public ConnInstanceTO getConf() {
        return StringUtils.isBlank(conf)
                ? null
                : POJOHelper.deserialize(conf, ConnInstanceTO.class);
    }
}
