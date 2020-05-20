/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.core.provisioning.java.data;

import org.apache.syncope.common.lib.to.GoogleMfaAuthTokenTO;
import org.apache.syncope.common.lib.types.GoogleMfaAuthToken;
import org.apache.syncope.core.provisioning.api.data.GoogleMfaAuthTokenDataBinder;
import org.springframework.stereotype.Component;

@Component
public class GoogleMfaAuthTokenDataBinderImpl implements GoogleMfaAuthTokenDataBinder {
    @Override
    public GoogleMfaAuthToken create(final GoogleMfaAuthTokenTO tokenTO) {
        return new GoogleMfaAuthToken.Builder()
            .issueDate(tokenTO.getIssueDate())
            .token(tokenTO.getToken())
            .owner(tokenTO.getOwner())
            .build();
    }

    @Override
    public GoogleMfaAuthTokenTO getGoogleMfaAuthTokenTO(final GoogleMfaAuthToken token) {
        return new GoogleMfaAuthTokenTO.Builder()
            .owner(token.getOwner())
            .token(token.getToken())
            .key(token.getKey())
            .issuedDate(token.getIssueDate())
            .build();
    }
}
