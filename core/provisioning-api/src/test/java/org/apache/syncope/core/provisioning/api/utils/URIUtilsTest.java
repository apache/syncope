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
package org.apache.syncope.core.provisioning.api.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.syncope.core.provisioning.api.AbstractTest;
import org.junit.jupiter.api.Test;

public class URIUtilsTest extends AbstractTest {

    @Test
    public void buildForConnId() throws URISyntaxException, MalformedURLException {
        AtomicReference<String> location = new AtomicReference<>();
        location.set("www.tirasa.net");
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> URIUtils.buildForConnId(location.get()));
        assertEquals(exception.getClass(), IllegalArgumentException.class);

        location.set("connid:test/location");
        URI expectedURI = new URI(location.get().trim());
        assertEquals(expectedURI, URIUtils.buildForConnId(location.get()));
    }
}
