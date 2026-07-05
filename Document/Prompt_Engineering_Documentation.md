# Prompt Engineering Documentation
### SQL Query Agent — Query Safety & Privacy Risk Analyzer

This document explains the prompt design used inside `PromptBuilder.java`, why each part of the prompt exists, and how it connects to the rest of the system. This is meant to be usable directly in your project report and viva.

---

## 1. Where the Prompt Fits in the System

```
User input (schema + request)
        ↓
PromptBuilder.buildPrompt()  →  builds the instruction text below
        ↓
GeminiClient.generateContent()  →  sends it to Gemini, gets raw text back
        ↓
QueryAnalysisAgent  →  parses the text as JSON into a QueryAnalysis object
        ↓
QueryAnalysisRepository  →  saves it to MongoDB
```

The prompt is the single most important part of this pipeline — everything downstream (parsing, storage, the frontend display) only works because the prompt forces Gemini into a predictable, structured response.

## 2. The Full Prompt Template

```
You are a database security expert reviewing SQL queries for safety and privacy risk.

You will be given:
1. A database schema description
2. A natural language request from a user

Your job has two parts:
PART 1: Generate a valid SQL query that fulfills the user's request, using ONLY
the tables and columns mentioned in the schema below. Do not invent columns that
are not listed.

PART 2: Audit the SQL query you just generated for these specific risks:
- MISSING_WHERE_CLAUSE: the query has no WHERE clause and may scan/return an
  entire table
- SELECT_STAR: the query uses SELECT * instead of naming specific columns
- SENSITIVE_COLUMN: the query selects or filters on a column that looks
  sensitive, such as: password, ssn, salary, email, credit_card, or similar
  personal/financial data
- SQL_INJECTION_RISK: the query is structured in a way that would be dangerous
  if built by directly concatenating raw user input

For each risk you find, explain it in one plain, simple English sentence a
non-technical person could understand. If a risk category does not apply, do
not include it in the output at all.

Then assign an overall riskLevel:
- "LOW" if no risks were found
- "MEDIUM" if exactly one risk was found
- "HIGH" if two or more risks were found, or if a SENSITIVE_COLUMN risk was
  found at all

Respond with ONLY valid JSON in exactly this shape, with no extra text before
or after it, and no markdown code fences:
{
  "generatedSql": "the SQL query as a single string",
  "riskLevel": "LOW or MEDIUM or HIGH",
  "riskFlags": [
    { "type": "RISK_TYPE_HERE", "explanation": "plain english explanation here" }
  ]
}

Database schema:
{schemaDescription}

User request:
{naturalLanguageRequest}
```

## 3. Prompt Engineering Techniques Used (and why)

| Technique | Where it appears in the prompt | Why it matters |
|---|---|---|
| **Role prompting** | "You are a database security expert reviewing SQL queries for safety and privacy risk." | Anchors Gemini's tone and judgment toward a security-analyst mindset from the very first sentence, rather than a generic assistant tone. This measurably changes the quality and seriousness of the risk explanations it generates. |
| **Task decomposition** | "PART 1" and "PART 2" explicitly separated | Splitting "generate the SQL" from "audit the SQL" into two clearly labeled sub-tasks makes the model reason through both jobs deliberately in sequence, instead of trying to do both at once and doing a shallower job of each. |
| **Grounding constraint** | "using ONLY the tables and columns mentioned in the schema... Do not invent columns" | Directly prevents hallucination — without this line, the model might reference plausible-sounding columns (like `department_id`) that don't actually exist in the user's schema, producing SQL that would fail to run in a real database. |
| **Fixed, named categories (closed-set classification)** | The 4 explicitly named risk types (`MISSING_WHERE_CLAUSE`, `SELECT_STAR`, `SENSITIVE_COLUMN`, `SQL_INJECTION_RISK`) | Instead of open-endedly asking "find any risks" (which produces inconsistent, differently-worded results every time), giving Gemini a fixed vocabulary means every response uses the same category names. This consistency is exactly what lets `QueryAnalysisAgent.java` reliably parse the response into Java objects every time. |
| **Deterministic scoring rule** | The explicit LOW/MEDIUM/HIGH rule based on risk count | Removes subjective judgment from the risk-level scoring. Without this rule, the same underlying risks might be scored differently across different runs; the rule makes scoring reproducible and defensible if a faculty member asks "why is this HIGH and not MEDIUM?" |
| **Forced structured output (JSON-only)** | "Respond with ONLY valid JSON... no markdown code fences" | This is what makes the whole pipeline automatable. Without this constraint, Gemini would return conversational prose ("Sure! Here's the SQL query you asked for...") that no code could reliably parse. |
| **Explicit output schema** | The literal JSON shape shown in the prompt | Gemini is far more likely to produce a parseable result when shown the *exact* field names and structure expected, rather than being told only in words what fields to include. |
| **Grounded, plain-language explanations** | "explain it in one plain, simple English sentence a non-technical person could understand" | Ensures the output is usable directly in a demo/UI without further processing — a non-technical faculty member or user can read a risk explanation and understand it immediately. |

## 4. The Self-Check / Retry Step (Agentic Behavior)

The prompt alone does not guarantee valid JSON every time — LLMs are probabilistic, and occasionally produce malformed output (a missing comma, or accidental markdown fencing despite instructions). Rather than assuming success, `QueryAnalysisAgent.java` treats the first response as a hypothesis to verify:

```
1. Send the prompt, get a response
2. Try to parse it as JSON
3. If parsing fails:
     → Re-send the same prompt with an added instruction:
       "Your previous response was not valid JSON. Return ONLY the JSON
       object described above, with no extra text, no explanation, and
       no markdown code fences."
     → Try parsing again
4. If it fails a second time, raise a clear error rather than silently
   returning broken data
```

This is a genuine example of autonomous, self-correcting agent behavior — the system checks its own output against a requirement (valid, parseable JSON) and takes a corrective action on its own before involving the user, rather than behaving as a simple one-shot prompt-response tool. This is worth explicitly calling out in your viva as the answer to "what makes this an agent, not just an API call wrapper?"

## 5. Example Input/Output Pair

**Input:**
- Schema: `users(id, name, email, salary, password_hash)`
- Request: `"Show me everyone's email and salary"`

**Expected Gemini output (structure, wording will vary slightly per run):**
```json
{
  "generatedSql": "SELECT email, salary FROM users",
  "riskLevel": "HIGH",
  "riskFlags": [
    {
      "type": "SENSITIVE_COLUMN",
      "explanation": "This query selects the 'salary' column, which contains sensitive financial information about employees."
    }
  ]
}
```

**Why HIGH:** per the deterministic scoring rule, any `SENSITIVE_COLUMN` flag forces `riskLevel` to `HIGH` regardless of how many other flags exist — reflecting that exposing sensitive personal/financial data is treated as the most serious category of risk this system checks for.

## 6. Design Decisions Deliberately Made for the 3-Day Scope

- **Single-call design, no chunking or retrieval:** the full schema + request fits comfortably within one prompt, so there was no need for document splitting or a vector database — this keeps the architecture simple without sacrificing correctness for realistic use.
- **Fixed 4-category risk taxonomy instead of open-ended risk discovery:** trades some theoretical flexibility (catching arbitrary novel risk types) for reliability and explainability, which matters more for a beginner-built, demoable system.
- **One retry, not an unbounded retry loop:** keeps latency and API usage predictable during a live demo, while still meaningfully reducing failure rate compared to no retry at all.
