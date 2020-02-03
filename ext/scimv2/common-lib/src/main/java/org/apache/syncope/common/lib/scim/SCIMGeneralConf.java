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
import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public class SCIMGeneralConf implements Serializable {

    private static final long serialVersionUID = 3228349133950736647L;

    private Date creationDate = new Date();

    private Date lastChangeDate = new Date();

    private int bulkMaxOperations = 1000;

    private int bulkMaxPayloadSize = 1048576;

    private int filterMaxResults = 200;

    public Date getCreationDate() {
        if (creationDate != null) {
            return new Date(creationDate.getTime());
        }
        return null;
    }

    public void setCreationDate(final Date creationDate) {
        if (creationDate != null) {
            this.creationDate = new Date(creationDate.getTime());
        } else {
            this.creationDate = null;
        }
    }

    public Date getLastChangeDate() {
        if (lastChangeDate != null) {
            return new Date(lastChangeDate.getTime());
        }
        return null;
    }

    public void setLastChangeDate(final Date lastChangeDate) {
        if (lastChangeDate != null) {
            this.lastChangeDate = new Date(lastChangeDate.getTime());
        } else {
            this.lastChangeDate = null;
        }
    }

    @JsonIgnore
    public String getETagValue() {
        Date etagDate = getLastChangeDate() == null
                ? getCreationDate() : getLastChangeDate();
        return Optional.ofNullable(etagDate).map(date -> String.valueOf(date.getTime())).orElse(StringUtils.EMPTY);

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
