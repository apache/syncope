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
package org.apache.syncope.core.provisioning.api;

import java.util.Map;
import org.apache.commons.jexl3.JexlBuilder;
import org.apache.commons.jexl3.JexlEngine;
import org.apache.commons.jexl3.introspection.JexlPermissions;
import org.apache.syncope.core.provisioning.api.jexl.EmptyClassLoader;
import org.apache.syncope.core.provisioning.api.jexl.JexlTools;
import org.apache.syncope.core.provisioning.api.jexl.SyncopeJexlFunctions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
@SuppressWarnings("squid:S2187")
public class AbstractTest {

    protected JexlTools jexlTools() {
        JexlEngine jexlEngine = new JexlBuilder().
                loader(new EmptyClassLoader()).
                permissions(JexlPermissions.RESTRICTED.compose("java.time.*", "org.apache.syncope.*")).
                namespaces(Map.of("syncope", new SyncopeJexlFunctions())).
                cache(512).
                silent(false).
                strict(false).
                create();
        return new JexlTools(jexlEngine);
    }

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }
}
