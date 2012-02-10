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
package org.syncope.client.report;

import org.syncope.client.AbstractBaseBean;

public abstract class AbstractReportlet extends AbstractBaseBean
        implements Reportlet {

    private static final long serialVersionUID = 2261593176065528113L;

    private String name;

    public AbstractReportlet() {
        this.name = this.getClass().getSimpleName();
    }

    public AbstractReportlet(final String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
