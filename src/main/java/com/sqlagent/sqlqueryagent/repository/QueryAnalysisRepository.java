package com.sqlagent.sqlqueryagent.repository;

import com.sqlagent.sqlqueryagent.model.QueryAnalysis;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QueryAnalysisRepository extends MongoRepository<QueryAnalysis, String> {
}
