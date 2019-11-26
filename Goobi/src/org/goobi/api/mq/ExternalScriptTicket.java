package org.goobi.api.mq;

import java.util.List;

import lombok.Data;

@Data
public class ExternalScriptTicket {
    private Integer processId;
    private String processName;

    private Integer stepId;
    private String stepName;

    private String jwt;

    private List<List<String>> scripts;
}
