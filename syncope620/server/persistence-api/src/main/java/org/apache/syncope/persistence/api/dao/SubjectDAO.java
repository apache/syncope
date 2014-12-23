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
package org.apache.syncope.persistence.api.dao;

import java.util.List;
import org.apache.syncope.persistence.api.entity.AttributableUtil;
import org.apache.syncope.persistence.api.entity.DerAttr;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.NormAttr;
import org.apache.syncope.persistence.api.entity.NormAttrValue;
import org.apache.syncope.persistence.api.entity.Subject;
import org.apache.syncope.persistence.api.entity.VirAttr;

public interface SubjectDAO<N extends NormAttr, D extends DerAttr<N>, V extends VirAttr> extends DAO<Subject<N, D, V>> {

    <T extends Subject<N, D, V>> List<T> findByAttrValue(String schemaName,
            NormAttrValue attrValue, AttributableUtil attrUtil);

    <T extends Subject<N, D, V>> T findByAttrUniqueValue(String schemaName,
            NormAttrValue attrUniqueValue, AttributableUtil attrUtil);

    <T extends Subject<N, D, V>> List<T> findByDerAttrValue(
            String schemaName, String value, AttributableUtil attrUtil);

    <T extends Subject<N, D, V>> List<T> findByResource(ExternalResource resource, Class<T> reference);
}
