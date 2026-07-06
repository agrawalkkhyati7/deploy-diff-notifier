package com.kkagrawal.deploydiff;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/deployments")
public class DeploymentController {
    private final DeploymentService service;

    public DeploymentController(DeploymentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DeploymentDiff> record(@Valid @RequestBody DeploymentRequest request) {
        DeploymentDiff diff = this.service.recordAndDiff(request);
        return ResponseEntity.ok(diff);
    }

    // GET /api/deployments?service=&env=  → list (with optional filters)
    @GetMapping
    public List<Deployment> list(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String env) {
        return this.service.list(service, env);
    }

    // GET /api/deployments/{id}  → one deploy, or 404
    @GetMapping("/{id}")
    public ResponseEntity<Deployment> getOne(@PathVariable Long id) {
        return this.service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
