package com.sqlagent.sqlqueryagent.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Document(collection = "queries")
public class QueryAnalysis {

    @Id
    private String id;

    private String schemaDescription;
    private String naturalLanguageRequest;
    private String generatedSql;
    private String riskLevel;
    private List<RiskFlag> riskFlags;
    private Instant createdAt;

    public QueryAnalysis() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSchemaDescription() {
        return schemaDescription;
    }

    public void setSchemaDescription(String schemaDescription) {
        this.schemaDescription = schemaDescription;
    }

    public String getNaturalLanguageRequest() {
        return naturalLanguageRequest;
    }

    public void setNaturalLanguageRequest(String naturalLanguageRequest) {
        this.naturalLanguageRequest = naturalLanguageRequest;
    }

    public String getGeneratedSql() {
        return generatedSql;
    }

    public void setGeneratedSql(String generatedSql) {
        this.generatedSql = generatedSql;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public List<RiskFlag> getRiskFlags() {
        return riskFlags;
    }

    public void setRiskFlags(List<RiskFlag> riskFlags) {
        this.riskFlags = riskFlags;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}