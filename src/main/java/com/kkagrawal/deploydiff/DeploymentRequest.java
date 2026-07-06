package com.kkagrawal.deploydiff;

import jakarta.validation.constraints.NotBlank;

public class DeploymentRequest {
     @NotBlank(message = "serviceName is required")
    private String serviceName;

    @NotBlank(message = "version is required")
    private String version;

    @NotBlank(message = "gitCommit is required")
    private String gitCommit;

    @NotBlank(message = "environment is required")
    private String environment;

    // Jackson (JSON library) needs a no-args constructor + getters/setters
    // to build this object from the incoming request body.
    public DeploymentRequest() {
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGitCommit() {
        return gitCommit;
    }

    public void setGitCommit(String gitCommit) {
        this.gitCommit = gitCommit;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }
}
