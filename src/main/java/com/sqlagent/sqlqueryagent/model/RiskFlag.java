package com.sqlagent.sqlqueryagent.model;

public class RiskFlag {

    private String type;
    private String explanation;

    public RiskFlag() {
    }

    public RiskFlag(String type, String explanation) {
        this.type = type;
        this.explanation = explanation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}