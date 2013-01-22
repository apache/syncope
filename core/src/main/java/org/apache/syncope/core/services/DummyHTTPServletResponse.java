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
package org.apache.syncope.core.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * Dummy (empty) HttpServletResponse.
 */
public class DummyHTTPServletResponse implements HttpServletResponse {

    @Override
    public void flushBuffer() throws IOException {
        // No action.
    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public String getCharacterEncoding() {
        return null;
    }

    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return null;
    }

    @Override
    public boolean isCommitted() {
        return false;
    }

    @Override
    public void reset() {
        // No action.
    }

    @Override
    public void resetBuffer() {
        // No action.
    }

    @Override
    public void setBufferSize(final int arg0) {
        // No action.
    }

    @Override
    public void setCharacterEncoding(final String arg0) {
        // No action.
    }

    @Override
    public void setContentLength(final int arg0) {
        // No action.
    }

    @Override
    public void setContentType(final String arg0) {
        // No action.
    }

    @Override
    public void setLocale(final Locale arg0) {
        // No action.
    }

    @Override
    public void addCookie(final Cookie arg0) {
        // No action.
    }

    @Override
    public void addDateHeader(final String arg0, final long arg1) {
        // No action.
    }

    @Override
    public void addHeader(final String arg0, final String arg1) {
        // No action.
    }

    @Override
    public void addIntHeader(final String arg0, final int arg1) {
        // No action.
    }

    @Override
    public boolean containsHeader(final String arg0) {
        return false;
    }

    @Override
    public String encodeRedirectURL(final String arg0) {
        return arg0;
    }

    @Override
    @Deprecated
    public String encodeRedirectUrl(final String arg0) {
        return arg0;
    }

    @Override
    public String encodeURL(final String arg0) {
        return arg0;
    }

    @Override
    @Deprecated
    public String encodeUrl(final String arg0) {
        return arg0;
    }

    @Override
    public void sendError(final int arg0) throws IOException {
        // No action.
    }

    @Override
    public void sendError(final int arg0, final String arg1) throws IOException {
        // No action.
    }

    @Override
    public void sendRedirect(final String arg0) throws IOException {
        // No action.
    }

    @Override
    public void setDateHeader(final String arg0, final long arg1) {
        // No action.
    }

    @Override
    public void setHeader(final String arg0, final String arg1) {
        // No action.
    }

    @Override
    public void setIntHeader(final String arg0, final int arg1) {
        // No action.
    }

    @Override
    public void setStatus(final int arg0) {
        // No action.
    }

    @Override
    @Deprecated
    public void setStatus(final int arg0, final String arg1) {
        // No action.
    }
}
