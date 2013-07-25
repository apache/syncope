/*
 * Copyright 2013 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.syncope.common.to;

import java.util.Date;
import org.apache.syncope.common.AbstractBaseBean;

/**
 * Abstract wrapper for common system information.
 */
public class AbstractSysInfoTO extends AbstractBaseBean {

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
        return creationDate;
    }

    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastModifier() {
        return lastModifier;
    }

    public void setLastModifier(final String lastModifier) {
        this.lastModifier = lastModifier;
    }

    public Date getLastChangeDate() {
        return lastChangeDate;
    }

    public void setLastChangeDate(final Date lastChangeDate) {
        this.lastChangeDate = lastChangeDate;
    }
}
