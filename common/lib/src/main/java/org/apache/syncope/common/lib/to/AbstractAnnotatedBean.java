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
package org.apache.syncope.common.lib.to;

import org.apache.syncope.common.lib.AbstractBaseBean;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Date;
import javax.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.StringUtils;

/**
 * Abstract wrapper for common system information.
 */
@XmlType
public class AbstractAnnotatedBean extends AbstractBaseBean {

    private static final long serialVersionUID = -930797879027642457L;

    /**
     * Username of the user that has created this profile.
     * <p>
     * Cannot be used a reference to an existing user for two main reasons: the creator can be the user <tt>admin</tt>;
     * the creator could have been removed.
     */
    private String creator;

    /**
     * Creation date.
     */
    private Date creationDate;

    /**
     * Username of the user that has performed the last modification to this profile.
     * <p>
     * This field cannot be null: at creation time it have to be initialized with the creator username.
     * <p>
     * The modifier can be the user itself whether the last performed change has been a self-modification.
     * <p>
     * Cannot be used a reference to an existing user for two main reasons: the modifier can be the user <tt>admin</tt>;
     * the modifier could have been removed.
     */
    private String lastModifier;

    /**
     * Last change date.
     * <p>
     * This field cannot be null: at creation time it has to be initialized with <tt>creationDate</tt> field value.
     */
    private Date lastChangeDate;

    public String getCreator() {
        return creator;
    }

    public void setCreator(final String creator) {
        this.creator = creator;
    }

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

    public String getLastModifier() {
        return lastModifier;
    }

    public void setLastModifier(final String lastModifier) {
        this.lastModifier = lastModifier;
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
        return etagDate == null
                ? StringUtils.EMPTY
                : String.valueOf(etagDate.getTime());

    }
}
