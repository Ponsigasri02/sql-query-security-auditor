package com.sqlagent.sqlqueryagent.controller;

import com.sqlagent.sqlqueryagent.model.QueryAnalysis;
import com.sqlagent.sqlqueryagent.repository.QueryAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class HomeController {

    @Autowired
    private QueryAnalysisRepository repository;

    // Fetch MongoDB history documents array sequence layout
    @GetMapping("/history")
    public List<QueryAnalysis> getAllHistory() {
        return repository.findAll();
    }
}