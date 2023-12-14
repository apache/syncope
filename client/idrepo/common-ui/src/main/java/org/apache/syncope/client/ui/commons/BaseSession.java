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
package org.apache.syncope.client.ui.commons;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import org.apache.syncope.client.lib.SyncopeAnonymousClient;
import org.apache.wicket.model.ResourceModel;

public interface BaseSession {

    enum Error {
        SESSION_EXPIRED("error.session.expired", "Session expired: please login again"),
        AUTHORIZATION("error.authorization", "Insufficient access rights when performing the requested operation"),
        REST("error.rest", "There was an error while contacting the Core server");

        private final String key;

        private final String fallback;

        Error(final String key, final String fallback) {
            this.key = key;
            this.fallback = fallback;
        }

        public String message() {
            return new ResourceModel(key, fallback).getObject();
        }
    }

    void setDomain(String domain);

    String getDomain();

    String getJWT();

    SyncopeAnonymousClient getAnonymousClient();

    <T> T getAnonymousService(Class<T> serviceClass);

    <T> T getService(Class<T> serviceClass);

    <T> T getService(String etag, Class<T> serviceClass);

    <T> void resetClient(Class<T> service);

    DateOps.Format getDateFormat();

    /**
     * Extract and localize (if translation available) the actual message from the given exception; then, report it
     * via {@link org.apache.wicket.Session#error(java.io.Serializable)}.
     *
     * @see org.apache.syncope.client.lib.RestClientExceptionMapper
     *
     * @param e raised exception
     */
    void onException(Exception e);

    <T> Future<T> execute(Callable<T> command);

    default List<Locale> getSupportedLocales() {
        return List.of(
                Locale.ENGLISH,
                Locale.CANADA_FRENCH,
                Locale.ITALIAN,
                Locale.JAPANESE,
                Locale.of("pt", "BR"),
                Locale.of("ru"));
    }
}
