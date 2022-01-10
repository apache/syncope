/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.springframework.cloud.gateway.config;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import org.springframework.util.unit.DataSize;

// Keep until
// https://lists.apache.org/thread/km7c4ojrlw5q5j42cbw7nht6b0f4z5r2
// is clear.
public class DataSizeMaxValidator implements ConstraintValidator<DataSizeMax, DataSize> {

    private DataSize dataSizeMax;

    @Override
    public void initialize(final DataSizeMax dataSizeMax) {
        this.dataSizeMax = DataSize.of(dataSizeMax.value(), dataSizeMax.unit());
    }

    @Override
    public boolean isValid(final DataSize value, final ConstraintValidatorContext context) {
        return value == null || dataSizeMax.compareTo(value) >= 0;
    }
}
