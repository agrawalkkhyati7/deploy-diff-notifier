package com.kkagrawal.deploydiff;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class DeployDiffNotifierApplicationTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void recordsAValidDeploymentAsFirstDeploy() throws Exception {
        // Use a unique service+env so this is guaranteed to be the first deploy for it.
        String body = "{\"serviceName\":\"first-deploy-svc\",\"version\":\"1.0.0\","
                + "\"gitCommit\":\"a1b2c3d\",\"environment\":\"first-env\"}";

        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstDeploy").value(true))
                .andExpect(jsonPath("$.serviceName").value("first-deploy-svc"))
                .andExpect(jsonPath("$.currentVersion").value("1.0.0"))
                .andExpect(jsonPath("$.previousVersion").doesNotExist());
    }

    @Test
    void computesDiffAgainstPreviousDeploy() throws Exception {
        // First deploy of this unique service+env
        postDeployment("diff-svc", "1.0.0", "aaa111", "diff-env");
        // Second deploy → should diff against the first
        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"serviceName\":\"diff-svc\",\"version\":\"1.1.0\","
                                + "\"gitCommit\":\"bbb222\",\"environment\":\"diff-env\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstDeploy").value(false))
                .andExpect(jsonPath("$.previousVersion").value("1.0.0"))
                .andExpect(jsonPath("$.currentVersion").value("1.1.0"))
                .andExpect(jsonPath("$.previousCommit").value("aaa111"))
                .andExpect(jsonPath("$.currentCommit").value("bbb222"))
                .andExpect(jsonPath("$.minutesSinceLastDeploy").exists());
    }

    @Test
    void rejectsDeploymentMissingRequiredField() throws Exception {
        // Missing "version" → should be rejected with 400
        String body = "{\"serviceName\":\"checkout-api\","
                + "\"gitCommit\":\"a1b2c3d\",\"environment\":\"prod\"}";

        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsAndFiltersDeployments() throws Exception {
        postDeployment("checkout-api", "1.0.0", "aaa111", "prod");
        postDeployment("user-api", "2.0.0", "ccc333", "prod");

        // Filter by service → every result's serviceName should contain "checkout"
        mockMvc.perform(get("/api/deployments").param("service", "checkout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].serviceName", org.hamcrest.Matchers.everyItem(
                        org.hamcrest.Matchers.containsString("checkout"))));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        mockMvc.perform(get("/api/deployments/999999"))
                .andExpect(status().isNotFound());
    }

    // Small helper to store a deployment in tests (named to avoid clashing with MockMvc's post()).
    private void postDeployment(String service, String version, String commit, String env) throws Exception {
        String body = "{\"serviceName\":\"" + service + "\",\"version\":\"" + version
                + "\",\"gitCommit\":\"" + commit + "\",\"environment\":\"" + env + "\"}";
        mockMvc.perform(post("/api/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

        @Test
    void slackNotifierPostsMessageToWebhook() {
        // Build a RestClient bound to a mock server (no real network).
        org.springframework.web.client.RestClient.Builder builder =
                org.springframework.web.client.RestClient.builder();
        org.springframework.test.web.client.MockRestServiceServer mockServer =
                org.springframework.test.web.client.MockRestServiceServer.bindTo(builder).build();

        mockServer.expect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .requestTo("http://slack.test/webhook"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers
                        .method(org.springframework.http.HttpMethod.POST))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators
                        .withSuccess());

        org.springframework.web.client.RestClient mockClient = builder.build();

        // Create the notifier with a non-blank webhook URL so it actually sends.
        SlackNotifier notifier = new SlackNotifier(mockClient, "http://slack.test/webhook");

        DeploymentDiff diff = new DeploymentDiff(
                true, "checkout-api", "prod",
                null, "1.0.0", null, "aaa111", null);

        notifier.notifyDeployment(diff);

        // Verify the expected POST actually happened.
        mockServer.verify();
    }

    @Test
    void slackNotifierDoesNothingWhenNoWebhookConfigured() {
        // Empty URL → should NOT attempt any HTTP call (no mock expectations set).
        org.springframework.web.client.RestClient client =
                org.springframework.web.client.RestClient.builder().build();
        SlackNotifier notifier = new SlackNotifier(client, "");

        DeploymentDiff diff = new DeploymentDiff(
                true, "checkout-api", "prod",
                null, "1.0.0", null, "aaa111", null);

        // Should simply return without error (nothing to assert beyond "no exception").
        notifier.notifyDeployment(diff);
    }

}
