package com.kkagrawal.deploydiff;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;

@Entity
public class Deployment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceName;   // e.g. "checkout-api"
    private String version;       // e.g. "1.4.2"
    private String gitCommit;     // e.g. "a1b2c3d"
    private String environment;   // e.g. "prod", "staging"
    private Instant deployedAt;

    protected Deployment() {
    }

    public Deployment(String serviceName, String version, String gitCommit, String environment, Instant deployedAt) {
        this.serviceName = serviceName;
        this.version = version;
        this.gitCommit = gitCommit;
        this.environment = environment;
        this.deployedAt = deployedAt;
    }

    public Long getId() {
        return id;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getVersion() {
        return version;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public String getEnvironment() {
        return environment;
    }

    public Instant getDeployedAt() {
        return deployedAt;
    }
}
