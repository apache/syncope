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

import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;
import org.apache.syncope.client.ui.commons.rest.ResponseHolder;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IFixedLocationResourceStream;

public class HttpResourceStream extends AbstractResourceStream implements IFixedLocationResourceStream {

    private static final long serialVersionUID = 5811207817876330189L;

    private final ResponseHolder responseHolder;

    public HttpResourceStream(final ResponseHolder responseHolder) {
        super();
        this.responseHolder = responseHolder;
    }

    @Override
    public InputStream getInputStream() {

        return responseHolder.getInputStream() == null
                ? new ByteArrayInputStream(new byte[0])
                : responseHolder.getInputStream();
    }

    @Override
    public Bytes length() {
        return responseHolder.getInputStream() == null
                ? Bytes.bytes(0)
                : null;
    }

    @Override
    public void close() {
        // No need for explict closing
    }

    @Override
    public String locationAsString() {
        return responseHolder.getLocation();
    }

    @Override
    public String getContentType() {
        return Optional.ofNullable(responseHolder.getContentType()).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    public String getFilename() {
        return responseHolder.getFilename();
    }
}
