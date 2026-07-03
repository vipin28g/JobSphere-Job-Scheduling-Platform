package com.job.scheduler.controller;

import com.job.scheduler.dto.WorkerDto;
import com.job.scheduler.service.WorkerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {

    @Autowired
    private WorkerService workerService;

    @GetMapping
    public ResponseEntity<List<WorkerDto>> getAllWorkers() {
        return ResponseEntity.ok(workerService.getAllWorkers());
    }
}
