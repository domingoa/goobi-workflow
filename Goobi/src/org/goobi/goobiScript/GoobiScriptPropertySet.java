package org.goobi.goobiScript;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.goobi.beans.Process;
import org.goobi.beans.Processproperty;
import org.goobi.production.enums.GoobiScriptResultType;

import com.google.common.collect.ImmutableList;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.persistence.managers.ProcessManager;
import de.sub.goobi.persistence.managers.PropertyManager;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class GoobiScriptPropertySet extends AbstractIGoobiScript implements IGoobiScript {

    @Override
    public String getAction() {
        return "propertySet";
    }
    
    @Override
    public String getSampleCall() {
        StringBuilder sb = new StringBuilder();
        addNewAction(sb, "This GoobiScript allows to set a process property to a specific value.");
        addParameter(sb, "name", "Opening angle", "Name of the property to be changed. If the property does not exist already it is created here.");
        addParameter(sb, "value", "90°", "Value that the property shall be set to");
        return sb.toString();
    }
    
    @Override
    public boolean prepare(List<Integer> processes, String command, HashMap<String, String> parameters) {
        super.prepare(processes, command, parameters);

        if (StringUtils.isBlank(parameters.get("name"))) {
            Helper.setFehlerMeldung("goobiScriptfield", "Missing parameter: ", "name");
            return false;
        }

        if (StringUtils.isBlank(parameters.get("value"))) {
            Helper.setFehlerMeldung("goobiScriptfield", "Missing parameter: ", "value");
            return false;
        }
        // add all valid commands to list
        ImmutableList.Builder<GoobiScriptResult> newList = ImmutableList.<GoobiScriptResult> builder().addAll(gsm.getGoobiScriptResults());
        for (Integer i : processes) {
            GoobiScriptResult gsr = new GoobiScriptResult(i, command, username, starttime);
            newList.add(gsr);
        }
        gsm.setGoobiScriptResults(newList.build());

        return true;
    }

    @Override
    public void execute() {
        TEMPLATEThread et = new TEMPLATEThread();
        et.start();
    }

    class TEMPLATEThread extends Thread {
        @Override
        public void run() {
            // wait until there is no earlier script to be executed first
            while (gsm.getAreEarlierScriptsWaiting(starttime)) {
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    log.error("Problem while waiting for running GoobiScripts", e);
                }
            }
            String propertyName = parameters.get("name");
            String value = parameters.get("value");

            // execute all jobs that are still in waiting state
            for (GoobiScriptResult gsr : gsm.getGoobiScriptResults()) {
                if (gsm.getAreScriptsWaiting(command) && gsr.getResultType() == GoobiScriptResultType.WAITING && gsr.getCommand().equals(command)) {
                    Process p = ProcessManager.getProcessById(gsr.getProcessId());
                    gsr.setProcessTitle(p.getTitel());
                    gsr.setResultType(GoobiScriptResultType.RUNNING);
                    gsr.updateTimestamp();
                    boolean matched = false;
                    for (Processproperty pp : p.getEigenschaften()) {
                        if (pp.getTitel().equals(propertyName)) {
                            pp.setWert(value);
                            PropertyManager.saveProcessProperty(pp);
                            gsr.setResultMessage("Property updated.");
                            gsr.setResultType(GoobiScriptResultType.OK);
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) {
                        Processproperty pp = new Processproperty();
                        pp.setTitel(propertyName);
                        pp.setWert(value);
                        pp.setProzess(p);
                        PropertyManager.saveProcessProperty(pp);
                        gsr.setResultMessage("Property created.");
                        gsr.setResultType(GoobiScriptResultType.OK);
                    }
                    if (gsr.getResultType().equals(GoobiScriptResultType.RUNNING)) {
                        gsr.setResultType(GoobiScriptResultType.OK);
                    }
                    gsr.updateTimestamp();
                }
            }
        }
    }
}
