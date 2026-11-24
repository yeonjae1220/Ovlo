package me.yeonjae.ovlo.shared.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest(properties = {
        "management.endpoint.health.group.readiness.include=readinessState",
        "management.endpoints.web.exposure.include=health,metrics,prometheus"
})
@ActiveProfiles("test")
class HealthProbeSecurityTest {
    @Autowired WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
        AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
        AvailabilityChangeEvent.publish(context, LivenessState.CORRECT);
    }

    @Test
    void anonymousProbesReturnOnlyStatus() throws Exception {
        for (String group : new String[]{"readiness", "liveness"}) {
            mvc.perform(get("/actuator/health/" + group))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.components").doesNotExist())
                    .andExpect(jsonPath("$.details").doesNotExist());
            mvc.perform(get("/actuator/health/" + group).with(user("operator").authorities(() -> "ADMIN")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.components").doesNotExist());
        }
    }

    @Test
    void unavailableApplicationReturns503InsteadOfAuthenticationError() throws Exception {
        AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value("OUT_OF_SERVICE"));
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        AvailabilityChangeEvent.publish(context, LivenessState.BROKEN);
        mvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value("DOWN"));
    }

    @Test
    void onlyExactReadOnlyProbePathsArePublic() throws Exception {
        for (String path : new String[]{"/actuator/metrics", "/actuator/prometheus", "/actuator/env",
                "/actuator/health/readiness/db", "/actuator/health/readinessState"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
            mvc.perform(get(path).with(user("member").authorities(() -> "MEMBER")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(post("/actuator/health/readiness")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/members/me")).andExpect(status().isUnauthorized());
    }
}
