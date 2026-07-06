package com.kkagrawal.deploydiff;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class DeploymentService {
    private final DeploymentRepository repository;
    private final SlackNotifier slackNotifier;

    public DeploymentService(DeploymentRepository repository, SlackNotifier slackNotifier) {
        this.repository = repository;
        this.slackNotifier = slackNotifier;
    }

        public Deployment record(DeploymentRequest request) {
        Deployment deployment = new Deployment(
                request.getServiceName(),
                request.getVersion(),
                request.getGitCommit(),
                request.getEnvironment(),
                Instant.now());
        return repository.save(deployment);
    }

    public DeploymentDiff recordAndDiff(DeploymentRequest request) {
        // 1. Look up the previous deploy BEFORE saving the new one.
        Optional<Deployment> previous = repository
                .findFirstByServiceNameAndEnvironmentOrderByDeployedAtDesc(
                        request.getServiceName(), request.getEnvironment());

        // 2. Save the new deploy.
        Deployment current = record(request);

        // 3. Build the diff.
        DeploymentDiff diff;
        if (previous.isEmpty()) {
            diff = new DeploymentDiff(
                    true,
                    current.getServiceName(),
                    current.getEnvironment(),
                    null, current.getVersion(),
                    null, current.getGitCommit(),
                    null);
        } else {
            Deployment prev = previous.get();
            long minutes = Duration.between(prev.getDeployedAt(), current.getDeployedAt()).toMinutes();
            diff = new DeploymentDiff(
                    false,
                    current.getServiceName(),
                    current.getEnvironment(),
                    prev.getVersion(), current.getVersion(),
                    prev.getGitCommit(), current.getGitCommit(),
                    minutes);
        }

        // 4. Send the notification (no-op if no webhook is configured).
        slackNotifier.notifyDeployment(diff);

        return diff;
    }


    
    public List<Deployment> list(String service, String env) {
        return repository.search(blankToNull(service), blankToNull(env));
    }

    public Optional<Deployment> findById(Long id) {
        return repository.findById(id);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    
}
