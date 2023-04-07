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
package org.apache.syncope.common.lib.scim;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

public class SCIMGeneralConf implements Serializable {

    private static final long serialVersionUID = 3228349133950736647L;

    private OffsetDateTime creationDate = OffsetDateTime.now();

    private OffsetDateTime lastChangeDate = OffsetDateTime.now();

    private int bulkMaxOperations = 1000;

    private int bulkMaxPayloadSize = 1048576;

    private int filterMaxResults = 200;

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public OffsetDateTime getLastChangeDate() {
        return lastChangeDate;
    }

    public void setLastChangeDate(final OffsetDateTime lastChangeDate) {
        this.lastChangeDate = lastChangeDate;
    }

    @JsonIgnore
    public String getETagValue() {
        OffsetDateTime etagDate = getLastChangeDate() == null
                ? getCreationDate() : getLastChangeDate();
        return Optional.ofNullable(etagDate).
                map(date -> String.valueOf(date.toInstant().toEpochMilli())).
                orElse(StringUtils.EMPTY);

    }

    public int getBulkMaxOperations() {
        return bulkMaxOperations;
    }

    public void setBulkMaxOperations(final int bulkMaxOperations) {
        this.bulkMaxOperations = bulkMaxOperations;
    }

    public int getBulkMaxPayloadSize() {
        return bulkMaxPayloadSize;
    }

    public void setBulkMaxPayloadSize(final int bulkMaxPayloadSize) {
        this.bulkMaxPayloadSize = bulkMaxPayloadSize;
    }

    public int getFilterMaxResults() {
        return filterMaxResults;
    }

    public void setFilterMaxResults(final int filterMaxResults) {
        this.filterMaxResults = filterMaxResults;
    }
}
