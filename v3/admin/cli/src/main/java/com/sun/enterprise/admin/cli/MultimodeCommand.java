/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.admin.cli;

import java.io.*;
import java.util.*;
import org.jvnet.hk2.annotations.*;
import org.jvnet.hk2.component.*;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.CommandModel.ParamModel;
import com.sun.enterprise.admin.util.*;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;

/**
 * A scaled-down implementation of multi-mode command.
 *
 * @author केदार(km@dev.java.net)
 * @author Bill Shannon
 */
@Service(name = "multimode")
@Scoped(PerLookup.class)
public class MultimodeCommand extends CLICommand {
    @Inject
    private Habitat habitat;

    @Param(optional = true, shortName = "f")
    private File file;

    @Param(name = "printprompt", optional = true)
    private Boolean printPromptOpt;
    private boolean printPrompt;

    @Param(optional = true)
    private String encoding;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(MultimodeCommand.class);

    /**
     * The validate method validates that the type and quantity of
     * parameters and operands matches the requirements for this
     * command.  The validate method supplies missing options from
     * the environment.
     */
    @Override
    protected void validate()
            throws CommandException, CommandValidationException {
        if (printPromptOpt != null)
            printPrompt = printPromptOpt.booleanValue();
        else
            printPrompt = programOpts.isInteractive();
    }

    /**
     * In the usage message modify the --printprompt option to have a
     * default based on the --interactive option.
     */
    protected Collection<ParamModel> usageOptions() {
        Collection<ParamModel> opts = commandModel.getParameters();
        Set<ParamModel> uopts = new LinkedHashSet<ParamModel>();
	ParamModel p = new CommandModelData.ParamModelData("printprompt",
	    boolean.class, true, Boolean.toString(programOpts.isInteractive()));
	for (ParamModel pm : opts) {
	    if (pm.getName().equals("printprompt"))
                uopts.add(p);
            else
                uopts.add(pm);
	}
        return uopts;
    }

    @Override
    protected int executeCommand()
            throws CommandException, CommandValidationException {
        BufferedReader reader = null;
        try {
            if (file == null) {
                System.out.println(strings.get("multimodeIntro"));
                if (encoding != null)
                    reader = new BufferedReader(
                                new InputStreamReader(System.in, encoding));
                    reader = new BufferedReader(
                                new InputStreamReader(System.in));
            } else {
                printPrompt = false;
                if (!file.canRead()) {
                    throw new CommandException("File: " + file +
                                                " can not be read");
                }
                if (encoding != null)
                    reader = new BufferedReader(new InputStreamReader(
                                    new FileInputStream(file), encoding));
                else
                    reader = new BufferedReader(new FileReader(file));
            }
            return executeCommands(reader);
        } catch(IOException e) {
            throw new CommandException(e);
        } finally {
            try {
                if (file != null && reader != null)
                    reader.close();
            } catch (IOException e) {
                // ignore it
            }
        }
    }

    /**
     * Read commands from the specified BufferedReader
     * and execute them.  If printPrompt is set, prompt first.
     *
     * @return the exit code of the last command executed
     */
    private int executeCommands(BufferedReader reader)
            throws CommandException, CommandValidationException, IOException {
        String line = null;
        int rc = 0;

        /*
         * Any program options we start with are copied to the environment
         * to serve as defaults for commands we run, and then we give each
         * command an empty program options.
         */
        programOpts.toEnvironment(env);
        for (;;) {
            if (printPrompt) {
                System.out.print("asadmin> ");
                System.out.flush();
            }
            if ((line = reader.readLine()) == null) {
                if (printPrompt)
                    System.out.println();
                break;
            }

            if (line.trim().startsWith("#"))   // ignore comment lines
                continue;

            String[] args = null;
            try {
                args = getArgs(line);
            } catch (ArgumentTokenizer.ArgumentException ex) {
                logger.printMessage(ex.getMessage());
                continue;
            }

            if (args.length == 0)
                continue;

            String command = args[0];
            if (command.length() == 0)
                continue;

            // handle built-in exit and quit commands
            // XXX - care about their arguments?
            if (command.equals("exit") || command.equals("quit"))
                break;

            CLICommand cmd = null;
            ProgramOptions po = null;
            try {
                /*
                 * Every command gets its own copy of program options
                 * so that any program options specified in its
                 * command line options don't effect other commands.
                 * But all commands share the same environment.
                 */
                po = new ProgramOptions(env);
                // copy over AsadminMain info
                po.setProgramArguments(programOpts.getProgramArguments());
                po.setClassPath(programOpts.getClassPath());
                po.setClassName(programOpts.getClassName());
                // remote the old one and replace it
                habitat.remove(
                    habitat.getInhabitantByType(ProgramOptions.class));
                habitat.addComponent("program-options", po);
                cmd = CLICommand.getCommand(habitat, command);
                rc = cmd.execute(args);
            } catch (CommandValidationException cve) {
                logger.printError(cve.getMessage());
                logger.printError(cmd.getUsage());
                rc = ERROR;
            } catch (InvalidCommandException ice) {
                // find closest match with local or remote commands
                logger.printError(ice.getMessage());
                try {
                    CLIUtil.displayClosestMatch(command,
                        CLIUtil.getAllCommands(habitat, po, env),
                       strings.get("ClosestMatchedLocalAndRemoteCommands"));
                } catch (InvalidCommandException e) {
                    // not a big deal if we cannot help
                }
            } catch (CommandException ce) {
                if (ce.getCause() instanceof java.net.ConnectException) {
                    // find closest match with local commands
                    logger.printError(ce.getMessage());
                    try {
                        CLIUtil.displayClosestMatch(command,
                            CLIUtil.getLocalCommands(habitat),
                            strings.get("ClosestMatchedLocalCommands"));
                    } catch (InvalidCommandException e) {
                        logger.printMessage(
                                strings.get("InvalidRemoteCommand", command));
                    }
                } else
                    logger.printError(ce.getMessage());
                rc = ERROR;
            } finally {
                // restore the original program options
                // XXX - is this necessary?
                habitat.remove(
                    habitat.getInhabitantByType(ProgramOptions.class));
                habitat.addComponent("program-options", programOpts);
            }

            // XXX - this duplicates code in AsadminMain, refactor it
            switch (rc) {
            case SUCCESS:
                if (!programOpts.isTerse())
                    logger.printDetailMessage(
                        strings.get("CommandSuccessful", command));
                break;

            case ERROR:
                logger.printDetailMessage(
                    strings.get("CommandUnSuccessful", command));
                break;

            case INVALID_COMMAND_ERROR:
                logger.printDetailMessage(
                    strings.get("CommandUnSuccessful", command));
                break;

            case CONNECTION_ERROR:
                logger.printDetailMessage(
                    strings.get("CommandUnSuccessful", command));
                break;
            }
            CLIUtil.writeCommandToDebugLog(args, rc);
        }
        return rc;
    }

    private String[] getArgs(String line)
                                throws ArgumentTokenizer.ArgumentException {
        List<String> args = new ArrayList<String>();
        ArgumentTokenizer t = new ArgumentTokenizer(line);
        while (t.hasMoreTokens())
            args.add(t.nextToken());
        return args.toArray(new String[args.size()]);
    }
}
