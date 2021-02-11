package com.redhat.labs.lodestar.utils;

import com.google.common.collect.Lists;
import com.redhat.labs.lodestar.model.Commit;
import com.redhat.labs.lodestar.model.Commit.CommitBuilder;
import com.redhat.labs.lodestar.model.Engagement;
import com.redhat.labs.lodestar.model.EngagementUser;
import com.redhat.labs.lodestar.model.GitlabProject;
import com.redhat.labs.lodestar.model.Hook;
import com.redhat.labs.lodestar.model.HostingEnvironment;
import com.redhat.labs.lodestar.model.Status;

public class MockUtils {

    private MockUtils() {
        throw new IllegalStateException("Utility Class");
    }

    public static Engagement mockMinimumEngagement(String customerName, String projectName, String uuid) {
        return Engagement.builder().customerName(customerName).projectName(projectName).uuid(uuid).build();
    }

    public static HostingEnvironment mockHostingEnvironment(String environmentName, String ocpSubdomain) {
        return HostingEnvironment.builder().environmentName(environmentName).ocpCloudProviderName("provider1")
                .ocpClusterSize("small").ocpPersistentStorageSize("none").ocpSubDomain(ocpSubdomain).ocpVersion("4.x.x")
                .build();
    }

    public static EngagementUser mockEngagementUser(String email, String firstName, String lastName, String role,
            String uuid, boolean reset) {
        return EngagementUser.builder().email(email).firstName(firstName).lastName(lastName).role(role).uuid(uuid)
                .reset(true).build();
    }

    public static Hook mockHook(String pathWithNamespace, String nameWithNamespace, boolean fileChanged, String fileName) {
        return Hook.builder().project(mockGitLabProject(pathWithNamespace, nameWithNamespace))
                .commits(Lists.newArrayList(mockCommit(fileName, fileChanged))).build();
    }

    public static GitlabProject mockGitLabProject(String pathWithNamespace, String nameWithNamspace) {
        return GitlabProject.builder().pathWithNamespace(pathWithNamespace).nameWithNamespace(nameWithNamspace).build();
    }

    public static Commit mockCommit(String fileName, boolean hasChanged) {
        CommitBuilder builder = Commit.builder();
        if (hasChanged) {
            builder.added(Lists.newArrayList("status.json"));
        }
        return builder.build();
    }
    
    public static Status mockStatus(String status) {
        return Status.builder().status(status).build();
    }

}