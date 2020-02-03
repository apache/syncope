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
import java.util.Optional;

import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.syncope.core.persistence.api.entity.Batch;

@Entity
@Table(name = JPABatch.TABLE)
public class JPABatch extends AbstractProvidedKeyEntity implements Batch {

    private static final long serialVersionUID = 468423182798249255L;

    public static final String TABLE = "SyncopeBatch";

    @Temporal(TemporalType.TIMESTAMP)
    private Date expiryTime;

    @Lob
    private String results;

    @Override
    public Date getExpiryTime() {
        return Optional.ofNullable(expiryTime).map(time -> new Date(time.getTime())).orElse(null);
    }

    @Override
    public void setExpiryTime(final Date expiryTime) {
        if (expiryTime == null) {
            this.expiryTime = null;
        } else {
            this.expiryTime = new Date(expiryTime.getTime());
        }
    }

    @Override
    public String getResults() {
        return results;
    }

    @Override
    public void setResults(final String results) {
        this.results = results;
    }
}
