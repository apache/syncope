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
package org.apache.syncope.ide.netbeans;

import org.apache.syncope.common.lib.types.MailTemplateFormat;
import org.apache.syncope.common.lib.types.ReportTemplateFormat;

public final class PluginConstants {

    public static final String MAIL_TEMPLATE = "Mail Template";

    public static final String REPORT_XSLTS = "Report XSLTs";

    public static final String[] MAIL_TEMPLATE_FORMATS = {
        MailTemplateFormat.HTML.name(), MailTemplateFormat.TEXT.name() };

    public static final String[] REPORT_TEMPLATE_FORMATS = {
        ReportTemplateFormat.HTML.name(), ReportTemplateFormat.CSV.name(), ReportTemplateFormat.FO.name() };

    public static final String DISPLAY_NAME = "Apache Syncope";

    public static final String TOOL_TIP_TEXT = "This is a Apache Syncope window";

    private PluginConstants() {
    }

}
