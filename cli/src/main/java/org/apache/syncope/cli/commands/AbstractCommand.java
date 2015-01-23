package org.apache.syncope.cli.commands;

import com.beust.jcommander.Parameter;

public abstract class AbstractCommand {

    @Parameter(names = {"-h", "--help"})
    protected boolean help = false;
    
    public abstract void execute();
}
