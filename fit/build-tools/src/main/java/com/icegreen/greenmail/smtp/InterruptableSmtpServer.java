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
package com.icegreen.greenmail.smtp;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.server.ProtocolHandler;
import com.icegreen.greenmail.util.ServerSetup;
import java.io.IOException;
import java.net.Socket;

public class InterruptableSmtpServer extends SmtpServer {

    private boolean rejectRequests = false;

    public InterruptableSmtpServer(final ServerSetup setup, final Managers managers) {
        super(setup, managers);
    }

    @Override
    protected ProtocolHandler createProtocolHandler(final Socket clientSocket) {
        synchronized (this) {
            if (rejectRequests) {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    // ignore
                }
            }

            return super.createProtocolHandler(clientSocket);
        }
    }

    public void setRejectRequests(final boolean rejectRequests) {
        this.rejectRequests = rejectRequests;
    }
}
