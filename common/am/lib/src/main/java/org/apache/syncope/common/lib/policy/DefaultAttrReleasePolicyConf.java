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
package org.apache.syncope.common.lib.policy;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.ArrayList;
import java.util.List;

public class DefaultAttrReleasePolicyConf implements AttrReleasePolicyConf {

    private static final long serialVersionUID = -1969836661359025380L;

    /**
     * Specify the list of allowed attribute to release.
     * Use the special {@code *} to release everything.
     */
    private final List<String> allowedAttrs = new ArrayList<>();

    private final List<String> excludedAttrs = new ArrayList<>();

    private final List<String> includeOnlyAttrs = new ArrayList<>();

    @JacksonXmlElementWrapper(localName = "allowedAttrs")
    @JacksonXmlProperty(localName = "allowedAttr")
    public List<String> getAllowedAttrs() {
        return allowedAttrs;
    }

    @JacksonXmlElementWrapper(localName = "excludedAttrs")
    @JacksonXmlProperty(localName = "excludedAttr")
    public List<String> getExcludedAttrs() {
        return excludedAttrs;
    }

    @JacksonXmlElementWrapper(localName = "includeOnlyAttrs")
    @JacksonXmlProperty(localName = "includeOnlyAttr")
    public List<String> getIncludeOnlyAttrs() {
        return includeOnlyAttrs;
    }
}
