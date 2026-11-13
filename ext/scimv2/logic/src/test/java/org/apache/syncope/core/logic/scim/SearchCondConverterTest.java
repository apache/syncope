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
package org.apache.syncope.core.logic.scim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.syncope.common.lib.scim.SCIMConf;
import org.apache.syncope.common.lib.scim.SCIMUserConf;
import org.apache.syncope.common.lib.scim.SCIMUserNameConf;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SearchCondConverterTest {

    private static SearchCondVisitor VISITOR;

    @BeforeAll
    static void setup() {
        SCIMConf conf = new SCIMConf();
        conf.setUserConf(new SCIMUserConf());
        conf.getUserConf().setName(new SCIMUserNameConf());
        conf.getUserConf().getName().setFamilyName("surname");

        VISITOR = new SearchCondVisitor(Resource.User.name(), conf);
    }

    @Test
    void invalidSyntax() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> SearchCondConverter.convert(VISITOR, "userName eq"));
        assertEquals(ErrorType.invalidFilter, ex.getErrorType());
    }

    @Test
    void unknownAttribute() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> SearchCondConverter.convert(VISITOR, "unknownAttribute eq \"value\""));
        assertEquals(ErrorType.invalidFilter, ex.getErrorType());
    }
}
