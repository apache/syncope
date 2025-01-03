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
package org.apache.syncope.core.persistence.api.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;

public final class URIUtils {

    private URIUtils() {
        // empty constructor for static utility class
    }

    /**
     * Build a valid URI out of the given location.
     * Only "file", "connid" and "connids" schemes are allowed.
     *
     * @param location the candidate location for URI
     * @return valid URI for the given location
     * @throws URISyntaxException if the given location does not correspond to a valid URI
     */
    public static URI buildForConnId(final String location) throws URISyntaxException {
        String candidate = location.trim();

        if (!candidate.startsWith("file:")
                && !candidate.startsWith("connid:") && !candidate.startsWith("connids:")) {

            throw new IllegalArgumentException(candidate + " is not a valid URI for file or connid(s) schemes");
        }

        URI uri;
        if (candidate.startsWith("file:")) {
            candidate = StringUtils.substringAfter(candidate, "file:");
            if (SystemUtils.IS_OS_WINDOWS) {
                candidate = StringUtils.stripStart(candidate, "/");
            }

            uri = Path.of(candidate).toFile().getAbsoluteFile().toURI();
        } else {
            uri = new URI(candidate);
        }

        return uri;
    }
}
