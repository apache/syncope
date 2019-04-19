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
package org.apache.syncope.client.ui.commons.wizards.any;

import org.apache.syncope.common.lib.to.UserTO;

public class UserWrapper extends AnyWrapper<UserTO> {

    private static final long serialVersionUID = 263119743040080245L;

    private boolean storePasswordInSyncope = true;

    private UserTO previousUserTO;

    public UserWrapper(final UserTO userTO) {
        this(null, userTO);
    }

    public UserWrapper(final UserTO previousUserTO, final UserTO userTO) {
        super(userTO);
        this.previousUserTO = previousUserTO;
    }

    public boolean isStorePasswordInSyncope() {
        return storePasswordInSyncope;
    }

    public void setStorePasswordInSyncope(final boolean storePasswordInSyncope) {
        this.storePasswordInSyncope = storePasswordInSyncope;
    }

    public UserTO getPreviousUserTO() {
        return previousUserTO;
    }
}
