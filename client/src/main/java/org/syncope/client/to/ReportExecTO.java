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
package org.syncope.client.to;

import org.syncope.types.ReportExecStatus;

public class ReportExecTO extends AbstractExecTO {

    private static final long serialVersionUID = -1025555939724089215L;

    private long report;

    public long getReport() {
        return report;
    }

    public void setReport(long report) {
        this.report = report;
    }

    public void setStatus(ReportExecStatus status) {
        super.setStatus(status.name());
    }
}
