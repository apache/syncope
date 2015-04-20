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
package org.apache.syncope.core.persistence.api.dao;

import java.util.List;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.VirAttr;

public interface SubjectDAO<P extends PlainAttr, D extends DerAttr, V extends VirAttr>
        extends DAO<Subject<P, D, V>, Long> {

    List<? extends Subject<P, D, V>> findByAttrValue(
            String schemaName, PlainAttrValue attrValue, AttributableUtils attrUtils);

    Subject<P, D, V> findByAttrUniqueValue(
            String schemaName, PlainAttrValue attrUniqueValue, AttributableUtils attrUtils);

    List<? extends Subject<P, D, V>> findByDerAttrValue(
            String schemaName, String value, AttributableUtils attrUtils);

    List<? extends Subject<P, D, V>> findByResource(
            ExternalResource resource, AttributableUtils attrUtils);
}
