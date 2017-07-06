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

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.HistoryConf;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

@MappedSuperclass
public abstract class AbstractHistoryConf<E extends Entity, T extends EntityTO>
        extends AbstractGeneratedKeyEntity implements HistoryConf<E, T> {

    private static final long serialVersionUID = -7210303753586235180L;

    @Column(nullable = false)
    private String creator;

    @Temporal(TemporalType.TIMESTAMP)
    private Date creation;

    @Lob
    protected String conf;

    @Override
    public String getCreator() {
        return creator;
    }

    @Override
    public void setCreator(final String creator) {
        this.creator = creator;
    }

    @Override
    public Date getCreation() {
        return creation == null
                ? null
                : new Date(creation.getTime());
    }

    @Override
    public void setCreation(final Date creation) {
        this.creation = creation == null
                ? null
                : new Date(creation.getTime());
    }

    @Override
    public void setConf(final T conf) {
        this.conf = POJOHelper.serialize(conf);
    }

}
