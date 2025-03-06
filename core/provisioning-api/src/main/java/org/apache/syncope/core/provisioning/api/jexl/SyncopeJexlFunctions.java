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
package org.apache.syncope.core.provisioning.api.jexl;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;

/**
 * Utility functions for usage with JEXL engine.
 */
public class SyncopeJexlFunctions {

    /**
     * Converts realm's full path into the equivalent DN.
     *
     * Example: {@code /a/b/c} becomes {@code ou=c,ou=b,ou=a}.
     *
     * @param fullPath realm's full path
     * @param attr attribute name for DN
     * @return DN equivalent of the provided full path
     */
    public String fullPath2Dn(final String fullPath, final String attr) {
        return fullPath2Dn(fullPath, attr, StringUtils.EMPTY);
    }

    /**
     * Converts realm's full path into the equivalent DN.
     *
     * Example: {@code /a/b/c} becomes {@code ,ou=c,ou=b,ou=a}, when {@code prefix} is
     * {@code &quot;,&quot;}
     *
     * @param fullPath realm's full path
     * @param attr attribute name for DN
     * @param prefix result's prefix
     * @return DN equivalent of the provided full path
     */
    public String fullPath2Dn(final String fullPath, final String attr, final String prefix) {
        String[] fullPathSplitted = fullPath.split("/");
        if (fullPathSplitted.length <= 1) {
            return StringUtils.EMPTY;
        }

        List<String> headless = Arrays.asList(fullPathSplitted).subList(1, fullPathSplitted.length);
        Collections.reverse(headless);
        return prefix + attr + "=" + headless.stream().collect(Collectors.joining("," + attr + "="));
    }

    /**
     * Extracts the values of the attribute with given name from the given connector object, or empty list if not found.
     *
     * @param connObj connector object
     * @param name attribute name
     * @return the values of the attribute with given name from the given connector object, or empty list if not found
     */
    public List<Object> connObjAttrValues(final ConnectorObject connObj, final String name) {
        return Optional.ofNullable(connObj).
                flatMap(obj -> Optional.ofNullable(obj.getAttributeByName(name)).
                map(Attribute::getValue)).
            orElseGet(List::of);
    }
}
