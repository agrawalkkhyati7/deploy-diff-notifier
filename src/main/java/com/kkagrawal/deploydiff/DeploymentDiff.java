package com.kkagrawal.deploydiff;

public record DeploymentDiff(
        boolean firstDeploy,      // true if there was no previous deploy to compare
        String serviceName,
        String environment,
        String previousVersion,   // null on first deploy
        String currentVersion,
        String previousCommit,    // null on first deploy
        String currentCommit,
        Long minutesSinceLastDeploy  // null on first deploy
) {
}
