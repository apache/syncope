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
package org.apache.syncope.common.lib.to;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import org.apache.syncope.common.lib.AbstractBaseBean;

@XmlRootElement(name = "pagedConnObjectResult")
@XmlType
public class PagedConnObjectTOResult extends AbstractBaseBean {

    private static final long serialVersionUID = -2832908019064402976L;

    private URI next;

    private final List<ConnObjectTO> result = new ArrayList<>();

    private String pagedResultsCookie;

    private int remainingPagedResults = -1;

    private boolean allResultsReturned = true;

    public URI getNext() {
        return next;
    }

    public void setNext(final URI next) {
        this.next = next;
    }

    /**
     * Returns the opaque cookie which should be used with the next paged results search request.
     *
     * @return The opaque cookie which should be used with the next paged results search request, or {@code null} if
     * paged results were not requested, or if there are not more pages to be returned.
     */
    public String getPagedResultsCookie() {
        return pagedResultsCookie;
    }

    /**
     * @param pagedResultsCookie The opaque cookie which should be used with the next paged results search request, or
     * {@code null} if paged results were not requested, or if there are not more pages to be returned.
     */
    public void setPagedResultsCookie(final String pagedResultsCookie) {
        this.pagedResultsCookie = pagedResultsCookie;
    }

    /**
     * Returns an estimate of the total number of remaining results to be returned in subsequent paged results search
     * requests.
     *
     * @return An estimate of the total number of remaining results to be returned in subsequent paged results search
     * requests, or {@code -1} if paged results were not requested, or if the total number of remaining results is
     * unknown.
     */
    public int getRemainingPagedResults() {
        return remainingPagedResults;
    }

    /**
     * @param remainingPagedResults An estimate of the total number of remaining results to be returned in subsequent
     * paged results search requests, or {@code -1} if paged results were not requested, or if the total number of
     * remaining results is unknown.
     */
    public void setRemainingPagedResults(final int remainingPagedResults) {
        this.remainingPagedResults = remainingPagedResults;
    }

    /**
     * Returns a flag indicating whether all the results that match a search query were returned.
     *
     * @return true if the search returned all the results that match the query, false if the returned
     * result is not complete, e.g. if the server returned only part of the results due to server limits, errors, etc.
     */
    public boolean isAllResultsReturned() {
        return allResultsReturned;
    }

    /**
     * @param allResultsReturned Set to true if the search returned all the results that match the query. Set to false
     * if the returned result is not complete, e.g. if the server returned only part of the results due to server
     * limits, errors, etc.
     */
    public void setAllResultsReturned(final boolean allResultsReturned) {
        this.allResultsReturned = allResultsReturned;
    }

    @XmlElementWrapper(name = "result")
    @XmlElement(name = "item")
    @JsonProperty("result")
    public List<ConnObjectTO> getResult() {
        return result;
    }

}
