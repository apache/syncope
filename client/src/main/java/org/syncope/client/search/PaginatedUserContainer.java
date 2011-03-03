/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.client.search;

import java.util.List;
import org.syncope.client.AbstractBaseBean;
import org.syncope.client.to.UserTO;

public class PaginatedUserContainer extends AbstractBaseBean {

    private List<UserTO> records;

    private int totalRecords;

    private int recordsInPage;

    private int pageNumber;

    private int pageSize;

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<UserTO> getRecords() {
        return records;
    }

    public void setRecords(List<UserTO> records) {
        this.records = records;
    }

    public int getRecordsInPage() {
        return recordsInPage;
    }

    public void setRecordsInPage(int recordsInPage) {
        this.recordsInPage = recordsInPage;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }
}
