package in.gov.sih.sih26135.security.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.Collections;
import java.util.List;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Enterprise OpenAPI 3.0 / Swagger documentation configuration for the SIH-26135 Spring Boot backend.
 *
 * <p>Provides:
 * <ul>
 *   <li>Comprehensive API metadata, contact, and server environment definitions</li>
 *   <li>HTTP Bearer JWT security scheme definition ({@code bearerAuth}) with role and tenancy scoping documentation</li>
 *   <li>Dynamic operation-level security customization stripping security from genuinely public endpoints</li>
 *   <li>Five mutually disjoint logical API groups plus an all-inclusive default group ({@code 0-all-apis})</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

  public static final String SECURITY_SCHEME_NAME = "bearerAuth";

  @Value("${server.port:8080}")
  private String serverPort;

  @Value("${server.servlet.context-path:}")
  private String contextPath;

  @Bean
  public OpenAPI customOpenAPI() {
    String serverUrl = "http://localhost:" + serverPort + (contextPath.isEmpty() ? "/" : contextPath);

    return new OpenAPI()
        .info(new Info()
            .title("SIH-26135 Analytics Backend API")
            .version("v1.0.0")
            .description("""
                ### SIH-26135 Unified Trainee Tracking, Verification, Analytics & Governance Engine
                
                Enterprise REST API providing end-to-end vocational skill training monitoring, longitudinal employment verification, statutory DPDP Act 2023 compliance, automated data quality assurance, multi-channel communication dispatch, and high-throughput analytical exports.
                
                ---
                
                #### 1. Core Domain Capabilities
                - **Master Entities**: Geographies (States, Districts), Users, Roles, Permissions, Trainees, Courses, Programs, Schemes, Sectors, Training Centers, Training Providers, Training Batches, and Enrollments.
                - **Lifecycle Records**: Assessments, Certifications, Placements, Employment Records, Salary Histories, Exit Events, Unemployment Records, and Longitudinal Surveys.
                
                #### 2. Multi-Step Workflow Engines
                - **Candidate Lifecycle**: End-to-end status tracking from enrollment to certified, placed, verified employment, or dropout/exit.
                - **Placement & Employment**: Transition placement records to verified employment with compensation history logging.
                - **Longitudinal Verification**: Multi-channel verification attempts (SMS, WhatsApp, IVR, physical employer confirmation), evidence capture, and SLA escalation.
                - **Skill Gap & Remediation**: Anomaly identification, personalized diagnostic recommendations, and automated remediation triggers.
                
                #### 3. Advanced Analytics & MIS Reporting
                - **Projected Read Models**: Q01-Q22 analytical fact aggregations and summaries.
                - **Dashboards**: Real-time Geographic, Executive, Data Integrity, Economic Outcome, Retention/Attrition, Skill Gap, and Operational oversight.
                - **Streaming Exports**: Memory-bounded CSV and JSON export streams with sliding-window concurrency limits and formula injection defense (CWE-1236).
                
                #### 4. Data Quality & Operations Governance
                - **Anomaly Detection Engine**: Real-time cross-field validation, spike alarms, and automated issue tracking.
                - **Bulk Staging & Ingestion**: High-volume multipart CSV ingestion with chunked staging, quarantine isolation, and transactional commit.
                - **Audit Logging**: Immutable actor attribution, entity correlation tracking, and tamper-evident event streaming.
                
                #### 5. External Communication
                - **Durable Dispatch Queue**: Multi-worker lease management, exponential backoff retries, and dead-letter queue containment.
                - **Provider Integrations**: Twilio SMS, WhatsApp Business API, Twilio SendGrid email, and fallback SMTP delivery.
                - **Webhook Receipts**: Asynchronous webhook ingestion for real-time delivery status synchronization.
                
                #### 6. Security, Tenancy & DPDP Compliance
                - **Authentication**: Stateless HMAC-SHA256 / RSA Bearer JWT tokens.
                - **Role-Based Authorization**: `ROLE_super_admin`, `ROLE_national_admin`, `ROLE_state_admin`, `ROLE_district_admin`, `ROLE_training_provider`, `ROLE_employer`, `ROLE_trainee`, `ROLE_auditor`.
                - **Tenancy Scoping**: Territorial bounding (State/District), Institutional bounding (Provider/Employer), and Trainee self-service scoping.
                - **Statistical Disclosure Control (SDC)**: $k=5$ minimum cell suppression, complementary secondary suppression, partition anti-differencing, and zero canary credential leakage.
                """)
            .contact(new Contact()
                .name("SIH-26135 National Technical Administration")
                .email("support@sih26135.gov.in"))
            .license(new License()
                .name("Government of India / SIH-26135 Proprietary")
                .url("https://sih26135.gov.in/license")))
        .servers(List.of(
            new Server()
                .url(serverUrl)
                .description("Local Development & Demonstration Server")
        ))
        .components(new Components()
            .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("""
                    Standard HTTP Bearer JWT token header.
                    
                    Format: `Authorization: Bearer <access_token>`
                    
                    Authentication is stateless. Tokens are minted via `POST /api/v1/auth/login` and refreshed via `POST /api/v1/auth/refresh`.
                    Permissions are enforced via Spring Security `@PreAuthorize` method security and `WorkflowSecurityScopeValidator` multi-tenant boundaries.
                    """)))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
  }

  /**
   * Operation customizer ensuring that genuinely public endpoints do NOT falsely declare a mandatory JWT requirement.
   */
  @Bean
  public OperationCustomizer publicEndpointSecurityCustomizer() {
    return (operation, handlerMethod) -> {
      String className = handlerMethod.getBeanType().getSimpleName();
      String methodName = handlerMethod.getMethod().getName();

      boolean isPublic = false;

      // 1. Authentication endpoints (login, refresh)
      if ("AuthController".equals(className) && ("login".equals(methodName) || "refresh".equals(methodName) || "refreshToken".equals(methodName))) {
        isPublic = true;
      }
      // 2. Public inbound webhooks (Twilio SMS/WhatsApp, SendGrid)
      else if ("CommunicationWebhookController".equals(className)) {
        isPublic = true;
      }

      if (isPublic) {
        operation.setSecurity(Collections.emptyList());
      } else {
        // Ensure protected operations include the bearerAuth scheme
        if (operation.getSecurity() == null || operation.getSecurity().isEmpty()) {
          operation.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
        }
      }

      return operation;
    };
  }

  // =========================================================================
  // OpenAPI Group Definitions
  // =========================================================================

  /**
   * Group 0: Default all-inclusive API specification covering all application-owned endpoints.
   */
  @Bean
  public GroupedOpenApi allApisGroup() {
    return GroupedOpenApi.builder()
        .group("0-all-apis")
        .pathsToMatch("/api/v1/**")
        .build();
  }

  /**
   * Group 1: Core Domain Management (Geography, Auth, Trainees, Courses, Centers, Providers, Batches, Enrollments, Employers).
   */
  @Bean
  public GroupedOpenApi coreDomainGroup() {
    return GroupedOpenApi.builder()
        .group("1-core-domain")
        .pathsToMatch(
            "/api/v1/auth/**",
            "/api/v1/users/**",
            "/api/v1/roles/**",
            "/api/v1/permissions/**",
            "/api/v1/trainees/**",
            "/api/v1/trainee-assessments/**",
            "/api/v1/trainee-unemployment-events/**",
            "/api/v1/consents/**",
            "/api/v1/courses/**",
            "/api/v1/programs/**",
            "/api/v1/schemes/**",
            "/api/v1/sectors/**",
            "/api/v1/states/**",
            "/api/v1/districts/**",
            "/api/v1/training-centers/**",
            "/api/v1/training-providers/**",
            "/api/v1/training-batches/**",
            "/api/v1/training-enrollments/**",
            "/api/v1/assessments/**",
            "/api/v1/assessment-results/**",
            "/api/v1/attendance-records/**",
            "/api/v1/certifications/**",
            "/api/v1/employers/**",
            "/api/v1/employment-records/**",
            "/api/v1/employment-exit-events/**",
            "/api/v1/job-applications/**",
            "/api/v1/job-postings/**",
            "/api/v1/placement-records/**",
            "/api/v1/salary-history/**",
            "/api/v1/survey-questions/**",
            "/api/v1/survey-responses/**",
            "/api/v1/survey-response-answers/**"
        )
        .build();
  }

  /**
   * Group 2: Workflows & Multi-Step Lifecycle Operations (Candidate Lifecycle, Placement, Verification, Skill Gap).
   */
  @Bean
  public GroupedOpenApi workflowsGroup() {
    return GroupedOpenApi.builder()
        .group("2-workflows")
        .pathsToMatch(
            "/api/v1/workflows/**",
            "/api/v1/followup-campaigns/**",
            "/api/v1/followup-tasks/**",
            "/api/v1/skill-gaps/**",
            "/api/v1/skill-gap-assessments/**",
            "/api/v1/skill-gap-observations/**",
            "/api/v1/skill-gap-recommendations/**",
            "/api/v1/employment-verifications/**",
            "/api/v1/employment-verification-attempts/**",
            "/api/v1/employment-verification-evidence/**",
            "/api/v1/employment-verification-requests/**"
        )
        .build();
  }

  /**
   * Group 3: Analytics, Dashboards & Streaming Exports (Q01-Q22 Analytics, Dashboards, MIS Exports).
   */
  @Bean
  public GroupedOpenApi analyticsExportsGroup() {
    return GroupedOpenApi.builder()
        .group("3-analytics-exports")
        .pathsToMatch(
            "/api/v1/analytics/**",
            "/api/v1/dashboards/**",
            "/api/v1/operational/**"
        )
        .build();
  }

  /**
   * Group 4: Operations & Governance (Audit Logs, System Event Logs, Data Quality, Bulk Staging & Quarantine).
   */
  @Bean
  public GroupedOpenApi operationsGovernanceGroup() {
    return GroupedOpenApi.builder()
        .group("4-operations-governance")
        .pathsToMatch(
            "/api/v1/audit-logs/**",
            "/api/v1/system-event-logs/**",
            "/api/v1/data-quality/**",
            "/api/v1/data-quality-rules/**",
            "/api/v1/data-quality-issues/**",
            "/api/v1/data-quality-issue-events/**",
            "/api/v1/import-batches/**",
            "/api/v1/import/batches/**",
            "/api/v1/import-records/**"
        )
        .build();
  }

  /**
   * Group 5: External Communication (Dispatch Queue, Delivery Logs, Templates, Inbound Webhooks).
   */
  @Bean
  public GroupedOpenApi externalCommunicationGroup() {
    return GroupedOpenApi.builder()
        .group("5-external-communication")
        .pathsToMatch(
            "/api/v1/communication/**",
            "/api/v1/communication-logs/**",
            "/api/v1/notifications/**"
        )
        .build();
  }
}