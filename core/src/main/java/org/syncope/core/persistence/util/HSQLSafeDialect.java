/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.syncope.core.persistence.util;

import java.sql.Types;
import org.hibernate.dialect.HSQLDialect;

/**
 * Try to fix issues that should have been already solved by
 * HSQLDB 2.1.0, unfortunately *not* yet on maven repositories.
 *
 * Basically, disable BLOB and CLOB supported introduced as new in
 * HSQLDB 2.0.0, reverting to 1.8.X style.
 */
public class HSQLSafeDialect extends HSQLDialect {

    public HSQLSafeDialect() {
        super();

        registerColumnType(Types.BLOB, "longvarbinary");
        registerColumnType(Types.CLOB, "longvarchar");
    }
}
