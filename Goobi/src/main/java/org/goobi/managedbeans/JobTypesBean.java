package org.goobi.managedbeans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.deltaspike.core.api.scope.WindowScoped;
import org.goobi.beans.JobType;
import org.goobi.beans.Step;

import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.ScriptThreadWithoutHibernate;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.persistence.managers.StepManager;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Log4j2
@WindowScoped
@Named("JobTypesBean")
@Data
public class JobTypesBean implements Serializable {
    private static final long serialVersionUID = 4451013586976652181L;

    @Inject
    private JobTypesCache jobTypesCache;
    private List<String> stepTitles;
    private List<String> availableStepTitles;
    private List<JobType> jobTypes;
    private JobType currentJobType;

    @PostConstruct
    public void init() {
        this.stepTitles = StepManager.getDistinctStepTitles("schritte.titel", "schritte.typAutomatisch=1");
        try {
            this.jobTypes = StepManager.getExternalQueueJobTypes();
        } catch (DAOException e) {
            log.error(e);
        }
        if (!jobTypes.isEmpty()) {
            this.currentJobType = jobTypes.get(0);
        }
    }

    public void addStepToCurrentJobType(String stepTitle) {
        if (this.currentJobType != null) {
            this.currentJobType.getStepNames().add(stepTitle);
            this.availableStepTitles.remove(stepTitle);
        }
    }

    public void removeStepFromCurrentJobType(String stepTitle) {
        if (this.currentJobType != null) {
            this.currentJobType.getStepNames().remove(stepTitle);
            this.availableStepTitles.add(stepTitle);
        }
    }

    public void removeStepFromJobType(String stepTitle, JobType jobType) {
        jobType.getStepNames().remove(stepTitle);
    }

    public String addNewJobType() {
        this.currentJobType = new JobType();
        this.availableStepTitles = new ArrayList<>(this.stepTitles);
        return "admin_jobtypes_edit.xhtml";
    }

    public String editJobType(JobType jobType) {
        this.currentJobType = jobType.clone();
        this.availableStepTitles = new ArrayList<>(this.stepTitles);
        this.availableStepTitles.removeAll(this.currentJobType.getStepNameList());
        return "admin_jobtypes_edit.xhtml";
    }

    public String saveCurrentJobType() {
        int[] indexes = IntStream.range(0, this.jobTypes.size())
                .filter(i -> this.jobTypes.get(i).getId().equals(this.currentJobType.getId()))
                .toArray();
        if (indexes.length == 0) {
            this.jobTypes.add(this.currentJobType);
        } else {
            JobType oldJobType = this.jobTypes.get(indexes[0]);
            if (oldJobType.isPaused() && !this.currentJobType.isPaused()) {
                //job was paused, but now isn't anymore => restart work
                //TODO re-run paused jobs for this jobType
            }
            this.jobTypes.set(indexes[0], this.currentJobType);
        }
        this.apply();
        return "admin_jobtypes_all.xhtml";
    }

    public String cancelJobTypeEdit() {
        this.currentJobType = new JobType();
        return "admin_jobtypes_all.xhtml";
    }

    public void pauseJobType(JobType jobType) {
        jobType.setPaused(true);
        this.apply();
    }

    public void unPauseJobType(JobType jobType) {
        jobType.setPaused(false);
        this.apply();
    }

    public boolean isCurrentJobTypeNew() {
        return !jobTypesCache.getJobTypes()
                .stream()
                .anyMatch(jt -> jt.getId().equals(this.currentJobType.getId()));
    }

    public String deleteCurrentJobType() {
        this.jobTypes = this.jobTypes
                .stream()
                .filter(jt -> !jt.getId().equals(this.currentJobType.getId()))
                .collect(Collectors.toList());
        this.apply();
        return "admin_jobtypes_all.xhtml";
    }

    public void apply() {
        try {
            List<String> restartStepnames = this.jobTypesCache.applyAndPersist(jobTypes);
            // Restart the steps.
            if (!restartStepnames.isEmpty()) {
                List<Step> restartSteps = StepManager.getPausedSteps(restartStepnames);
                for (Step step : restartSteps) {
                    ScriptThreadWithoutHibernate scriptThread = new ScriptThreadWithoutHibernate(step);
                    scriptThread.startOrPutToQueue();
                    StepManager.setStepPaused(step.getId(), false);
                }
            }
        } catch (DAOException e) {
            log.error("error persisting jobTypes", e);
            Helper.setFehlerMeldung(Helper.getTranslation("errorPersistingJobTypes"));
        }
    }
}
