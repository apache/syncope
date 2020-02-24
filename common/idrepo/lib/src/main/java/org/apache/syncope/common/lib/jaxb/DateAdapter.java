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
package org.apache.syncope.common.lib.jaxb;

import java.util.Date;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.apache.commons.lang3.time.FastDateFormat;

public class DateAdapter extends XmlAdapter<String, Date> {

    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    @Override
    public Date unmarshal(final String value) throws Exception {
        return FastDateFormat.getInstance(DATE_FORMAT).parse(value);
    }

    @Override
    public String marshal(final Date value) throws Exception {
        return FastDateFormat.getInstance(DATE_FORMAT).format(value);
    }
}
