package org.apache.syncope.installer.processes;

import com.izforge.izpack.panels.process.AbstractUIProcessHandler;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public abstract class AbstractProcess {

    protected void exec(final String cmd, final AbstractUIProcessHandler handler, final String path) {
        try {
            final Process process;
            if (path == null || path.isEmpty()) {
                process = Runtime.getRuntime().exec(cmd);
            } else {
                process = Runtime.getRuntime().exec(cmd, null, new File(path));
            }
            readResponse(process.getInputStream(), handler);
        } catch (IOException ex) {
        }
    }

    protected void readResponse(final InputStream inputStream, final AbstractUIProcessHandler handler) throws
            IOException {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line = reader.readLine();
        while (line != null) {
            line = reader.readLine();
            handler.logOutput(line == null ? "" : line, false);
        }
        inputStream.close();
    }

    protected void writeToFile(final File orm, final String content) {
        try {
            final FileWriter fw = new FileWriter(orm.getAbsoluteFile());
            final BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();
        } catch (IOException ex) {
        }
    }

}
