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
package org.apache.syncope.server.persistence.jpa.entity.membership;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.apache.syncope.server.persistence.api.entity.membership.MVirSchema;
import org.apache.syncope.server.persistence.jpa.entity.AbstractVirSchema;

@Entity
@Table(name = JPAMVirSchema.TABLE)
@Cacheable
public class JPAMVirSchema extends AbstractVirSchema implements MVirSchema {

    private static final long serialVersionUID = 6255905733563668766L;

    public static final String TABLE = "MVirSchema";

}
