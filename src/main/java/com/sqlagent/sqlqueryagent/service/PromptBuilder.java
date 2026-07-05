package com.sqlagent.sqlqueryagent.service;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildPrompt(String schemaDescription, String userRequest) {

        return """
                You are a database security expert reviewing SQL queries for safety and privacy risk.

                You will be given:
                1. A database schema description
                2. A natural language request from a user

                Your job has two parts:
                PART 1: Generate a valid SQL query that fulfills the user's request, using ONLY the tables and columns mentioned in the schema below. Do not invent columns that are not listed.

                PART 2: Audit the SQL query you just generated for these specific risks:
                - MISSING_WHERE_CLAUSE: the query has no WHERE clause and may scan/return an entire table
                - SELECT_STAR: the query uses SELECT * instead of naming specific columns
                - SENSITIVE_COLUMN: the query selects or filters on a column that looks sensitive, such as: password, ssn, salary, email, credit_card, or similar personal/financial data
                - SQL_INJECTION_RISK: the query is structured in a way that would be dangerous if built by directly concatenating raw user input (e.g. no parameterization possible for a value used in a WHERE clause)

                For each risk you find, explain it in one plain, simple English sentence a non-technical person could understand. If a risk category does not apply, do not include it in the output at all.

                Then assign an overall riskLevel:
                - "LOW" if no risks were found
                - "MEDIUM" if exactly one risk was found
                - "HIGH" if two or more risks were found, or if a SENSITIVE_COLUMN risk was found at all

                Respond with ONLY valid JSON in exactly this shape, with no extra text before or after it, and no markdown code fences:
                {
                  "generatedSql": "the SQL query as a single string",
                  "riskLevel": "LOW or MEDIUM or HIGH",
                  "riskFlags": [
                    { "type": "RISK_TYPE_HERE", "explanation": "plain english explanation here" }
                  ]
                }

                Database schema:
                %s

                User request:
                %s
                """.formatted(schemaDescription, userRequest);
    }
}