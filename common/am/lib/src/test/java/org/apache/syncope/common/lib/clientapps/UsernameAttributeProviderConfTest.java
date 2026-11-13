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
package org.apache.syncope.common.lib.clientapps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.apache.syncope.common.lib.types.CaseCanonicalizationMode;
import org.apache.syncope.common.lib.types.PersistentIdGenerator;
import org.junit.jupiter.api.Test;

class UsernameAttributeProviderConfTest {

    private final UsernameAttributeProviderConf.Mapper mapper = mock(UsernameAttributeProviderConf.Mapper.class);

    @Test
    void anonymous() {
        AnonymousUsernameAttributeProviderConf conf = new AnonymousUsernameAttributeProviderConf();
        conf.map(mapper);
        verify(mapper).map(conf);
    }

    @Test
    void defaultConf() {
        DefaultUsernameAttributeProviderConf conf = new DefaultUsernameAttributeProviderConf();
        conf.map(mapper);
        verify(mapper).map(conf);
    }

    @Test
    void groovy() {
        GroovyUsernameAttributeProviderConf conf = new GroovyUsernameAttributeProviderConf();
        conf.setGroovyScript("groovy");
        conf.setCaseCanonicalizationMode(CaseCanonicalizationMode.LOWER);

        conf.map(mapper);
        verify(mapper).map(conf);
        assertEquals("groovy", conf.getGroovyScript());
        assertEquals(CaseCanonicalizationMode.LOWER, conf.getCaseCanonicalizationMode());
    }

    @Test
    void pairwiseOidc() {
        PairwiseOidcUsernameAttributeProviderConf conf = new PairwiseOidcUsernameAttributeProviderConf();
        conf.setPersistentIdGenerator(PersistentIdGenerator.SHIBBOLETH);
        conf.setCaseCanonicalizationMode(CaseCanonicalizationMode.UPPER);

        conf.map(mapper);
        verify(mapper).map(conf);
        assertEquals(PersistentIdGenerator.SHIBBOLETH, conf.getPersistentIdGenerator());
        assertEquals(CaseCanonicalizationMode.UPPER, conf.getCaseCanonicalizationMode());
    }

    @Test
    void principalAttribute() {
        PrincipalAttributeUsernameAttributeProviderConf conf =
                new PrincipalAttributeUsernameAttributeProviderConf();
        conf.setUsernameAttribute("email");
        conf.setCaseCanonicalizationMode(CaseCanonicalizationMode.NONE);

        conf.map(mapper);
        verify(mapper).map(conf);
        assertEquals("email", conf.getUsernameAttribute());
    }

    @Test
    void equalsAndHashCode() {
        PrincipalAttributeUsernameAttributeProviderConf first = new PrincipalAttributeUsernameAttributeProviderConf();
        first.setUsernameAttribute("email");
        PrincipalAttributeUsernameAttributeProviderConf second = new PrincipalAttributeUsernameAttributeProviderConf();
        second.setUsernameAttribute("email");
        PrincipalAttributeUsernameAttributeProviderConf other = new PrincipalAttributeUsernameAttributeProviderConf();
        other.setUsernameAttribute("uid");

        assertEquals(first, first);
        assertNotEquals(first, null);
        assertNotEquals(first, "string");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, other);
    }
}
