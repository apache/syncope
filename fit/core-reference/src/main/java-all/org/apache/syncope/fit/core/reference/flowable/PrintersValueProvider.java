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
package org.apache.syncope.fit.core.reference.flowable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.flowable.api.DropdownValueProvider;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

public class PrintersValueProvider implements DropdownValueProvider {

    private static final SearchCond PRINTER_COND;

    private static final List<Sort.Order> ORDER_BY;

    static {
        AnyTypeCond anyTypeCond = new AnyTypeCond();
        anyTypeCond.setAnyTypeKey("PRINTER");
        PRINTER_COND = SearchCond.of(anyTypeCond);

        Sort.Order orderByNameAsc = new Sort.Order(Sort.Direction.ASC, "name");
        ORDER_BY = List.of(orderByNameAsc);
    }

    private final AnySearchDAO anySearchDAO;

    public PrintersValueProvider(final AnySearchDAO anySearchDAO) {
        this.anySearchDAO = anySearchDAO;
    }

    @Transactional(readOnly = true)
    @Override
    public Map<String, String> getValues() {
        return anySearchDAO.<AnyObject>search(PRINTER_COND, ORDER_BY, AnyTypeKind.ANY_OBJECT).stream().
                collect(Collectors.toMap(
                        AnyObject::getKey,
                        AnyObject::getName,
                        (u, v) -> {
                            throw new IllegalStateException(String.format("Duplicate key %s", u));
                        },
                        LinkedHashMap::new));
    }
}
