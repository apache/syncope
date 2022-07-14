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

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.syncope.common.lib.BaseBean;

public class PagedConnObjectResult implements BaseBean {

    private static final long serialVersionUID = -2832908019064402976L;

    private URI next;

    private final List<ConnObject> result = new ArrayList<>();

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
     * Returns a flag indicating whether all the results other match a search query were returned.
     *
     * @return true if the search returned all the results other match the query, false if the returned
     * result is not complete, e.g. if the server returned only part of the results due to server limits, errors, etc.
     */
    public boolean isAllResultsReturned() {
        return allResultsReturned;
    }

    /**
     * @param allResultsReturned Set to true if the search returned all the results other match the query. Set to false
     * if the returned result is not complete, e.g. if the server returned only part of the results due to server
     * limits, errors, etc.
     */
    public void setAllResultsReturned(final boolean allResultsReturned) {
        this.allResultsReturned = allResultsReturned;
    }

    @JacksonXmlElementWrapper(localName = "result")
    @JacksonXmlProperty(localName = "item")
    public List<ConnObject> getResult() {
        return result;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().
                append(next).
                append(result).
                append(pagedResultsCookie).
                append(remainingPagedResults).
                append(allResultsReturned).
                build();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PagedConnObjectResult other = (PagedConnObjectResult) obj;
        return new EqualsBuilder().
                append(next, other.next).
                append(result, other.result).
                append(pagedResultsCookie, other.pagedResultsCookie).
                append(remainingPagedResults, other.remainingPagedResults).
                append(allResultsReturned, other.allResultsReturned).
                build();
    }
}
