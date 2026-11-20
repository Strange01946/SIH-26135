#!/usr/bin/env python3
"""
Phase 29.10: Automated Postman Collection and Environment Generator

Reads the verified OpenAPI 3.1 specifications in docs/openapi/:
- openapi-0-all-apis.json (Full production API surface: 386 operations across 250 paths)
- openapi-1-core-domain.json (151 operations)
- openapi-2-workflows.json (86 operations)
- openapi-3-analytics-exports.json (78 operations)
- openapi-4-operations-governance.json (57 operations)
- openapi-5-external-communication.json (14 operations)

Generates:
1. docs/integration/sih26135-postman-collection.json (Postman Collection v2.1.0)
2. docs/integration/sih26135-local.postman_environment.json (Local Environment)
"""

import json
import os
import re
from pathlib import Path

SCRIPT_DIR = Path(__file__).resolve().parent
INTEGRATION_DIR = SCRIPT_DIR
PROJECT_ROOT = SCRIPT_DIR.parent.parent
OPENAPI_DIR = PROJECT_ROOT / "docs" / "openapi"

GROUPS = [
    ("1-core-domain", "1. Core Domain & Master Data", "Geographies, Users, Roles, Permissions, Auth, Trainees, Consents, Courses, Programs, Schemes, Sectors, Centers, Providers, Batches, Enrollments, Assessments, Certifications, Placements, Employers, Surveys"),
    ("2-workflows", "2. Workflows & Lifecycle Engines", "Candidate Lifecycle, Placement-to-Employment, Longitudinal Verification, Follow-up Campaigns, Skill Gap & Remediation, Training Outcomes"),
    ("3-analytics-exports", "3. Analytics, Dashboards & Streaming Exports", "Geographic, Executive, Data Integrity, Economic, Retention, Skill Gap dashboards; Fact/Summary read-models; memory-bounded CSV/JSON streaming exports with k=5 SDC"),
    ("4-operations-governance", "4. Operations, Audit & Data Quality", "Audit Logs, System Event Logs, Data Quality Rules & Issues, Bulk Staging & Ingestion Batches, Quarantine Records"),
    ("5-external-communication", "5. External Communication & Webhooks", "Communication Dispatch Queue, Delivery Logs, Templates, Inbound Provider Webhooks (Twilio SMS/WhatsApp, SendGrid)")
]

def load_json(filepath):
    with open(filepath, "r", encoding="utf-8") as f:
        return json.load(f)

def save_json(filepath, data):
    with open(filepath, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
    print(f"  + Generated: {filepath} ({os.path.getsize(filepath):,} bytes)")

def resolve_schema(schema, components, depth=0):
    """Recursively generate realistic synthetic sample from OpenAPI schema."""
    if depth > 5 or not schema:
        return {}

    if "$ref" in schema:
        ref_name = schema["$ref"].split("/")[-1]
        target_schema = components.get("schemas", {}).get(ref_name, {})
        return resolve_schema(target_schema, components, depth + 1)

    schema_type = schema.get("type")
    
    # Check if example is directly provided
    if "example" in schema:
        return schema["example"]

    # Enum support
    if "enum" in schema and schema["enum"]:
        return schema["enum"][0]

    if schema_type == "string":
        format_type = schema.get("format", "")
        desc = schema.get("description", "").lower()
        if format_type == "date-time":
            return "2026-11-20T10:00:00Z"
        if format_type == "date":
            return "2026-01-15"
        if "email" in desc or format_type == "email":
            return "trainee@example.org"
        if "phone" in desc or "mobile" in desc:
            return "9876543210"
        if "code" in desc:
            return "BATCH-2026-A1"
        if "uuid" in format_type or "correlation" in desc:
            return "c8f9b2a1-0000-4000-a000-000000000001"
        return "sample_text"

    if schema_type == "integer":
        if "page" in schema.get("description", "").lower():
            return 0
        return schema.get("minimum", 1)

    if schema_type == "number":
        return 100.0

    if schema_type == "boolean":
        return True

    if schema_type == "array":
        items_schema = schema.get("items", {})
        item_sample = resolve_schema(items_schema, components, depth + 1)
        return [item_sample] if item_sample else []

    if schema_type == "object" or "properties" in schema:
        obj = {}
        props = schema.get("properties", {})
        for prop_name, prop_schema in props.items():
            obj[prop_name] = resolve_schema(prop_schema, components, depth + 1)
        return obj

    return {}

def format_url(path, query_params):
    """Convert /api/v1/trainees/{id} to {{baseUrl}}/api/v1/trainees/:id and return url object."""
    clean_path = path.strip("/")
    segments = clean_path.split("/")
    
    postman_path = []
    path_variables = []
    
    for seg in segments:
        if seg.startswith("{") and seg.endswith("}"):
            var_name = seg[1:-1]
            postman_path.append(f":{var_name}")
            
            # Smart default value for variable
            val = "1"
            if "trainee" in var_name.lower():
                val = "{{traineeId}}"
            elif "batch" in var_name.lower():
                val = "{{batchId}}"
            elif "course" in var_name.lower():
                val = "{{courseId}}"
            elif "center" in var_name.lower():
                val = "{{centerId}}"
            elif "provider" in var_name.lower():
                val = "{{providerId}}"
            elif "employer" in var_name.lower():
                val = "{{employerId}}"
            elif "job" in var_name.lower():
                val = "{{jobId}}"
            elif "state" in var_name.lower():
                val = "{{stateId}}"
            elif "district" in var_name.lower():
                val = "{{districtId}}"
            elif "rule" in var_name.lower():
                val = "{{ruleId}}"
            elif "issue" in var_name.lower():
                val = "{{issueId}}"
            elif "id" == var_name.lower():
                val = "1"

            path_variables.append({
                "key": var_name,
                "value": str(val),
                "description": f"Target {var_name} identifier"
            })
        else:
            postman_path.append(seg)

    url_obj = {
        "raw": "{{baseUrl}}/" + "/".join(postman_path),
        "host": ["{{baseUrl}}"],
        "path": postman_path
    }

    if path_variables:
        url_obj["variable"] = path_variables

    if query_params:
        url_obj["query"] = query_params

    return url_obj

def generate_collection():
    print("=" * 80)
    print("Phase 29.10: Generating Enterprise Postman Collection & Environment")
    print("=" * 80)

    # 1. Load full production spec for components and reference
    full_spec_path = os.path.join(OPENAPI_DIR, "openapi-0-all-apis.json")
    full_spec = load_json(full_spec_path)
    components = full_spec.get("components", {})

    # Collection Structure
    collection = {
        "info": {
            "_postman_id": "sih26135-backend-production-api-collection",
            "name": "SIH-26135 Analytics Backend API",
            "description": (
                "### SIH-26135 Unified Trainee Tracking, Verification, Analytics & Governance Engine\n\n"
                "Comprehensive Postman collection covering 100% of the production API operations (386 operations across 250 paths).\n\n"
                "**Organization**:\n"
                "- `1-core-domain`: Master entities, Trainees, Centers, Providers, Batches, Consents, Auth (151 ops)\n"
                "- `2-workflows`: Candidate Lifecycle, Placement-to-Employment, Verification, Skill Gap, Follow-up (86 ops)\n"
                "- `3-analytics-exports`: Dashboards (Q01–Q22), Fact/Summary read-models, streaming CSV/JSON exports with k=5 SDC (78 ops)\n"
                "- `4-operations-governance`: Immutable Audit Trails, Data Quality Rules & Issues, Bulk CSV Ingestion (57 ops)\n"
                "- `5-external-communication`: Durable Dispatch Queue, Delivery Logs, Templates, Inbound Webhooks (14 ops)\n\n"
                "**Authentication**:\n"
                "Stateless Bearer JWT. Execute `1. Core Domain -> Authentication -> Login` to automatically populate `{{accessToken}}` and `{{refreshToken}}`."
            ),
            "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
        },
        "auth": {
            "type": "bearer",
            "bearer": [
                {
                    "key": "token",
                    "value": "{{accessToken}}",
                    "type": "string"
                }
            ]
        },
        "event": [
            {
                "listen": "test",
                "script": {
                    "type": "text/javascript",
                    "exec": [
                        "// ===========================================================================",
                        "// Global Contract-Aware Post-Response Validation (Phase 29.10)",
                        "// ===========================================================================",
                        "",
                        "// 1. Validate response code exists",
                        "pm.test(\"Status code is defined\", function () {",
                        "    pm.response.to.have.status(pm.response.code);",
                        "});",
                        "",
                        "// 2. Identify response media type",
                        "const contentType = pm.response.headers.get(\"Content-Type\") || \"\";",
                        "",
                        "// 3. Handle Streaming Exports & File Downloads",
                        "if (contentType.includes(\"text/csv\") || contentType.includes(\"application/octet-stream\")) {",
                        "    pm.test(\"Streaming export returned valid content-type\", function () {",
                        "        pm.expect(contentType).to.be.oneOf([\"text/csv;charset=UTF-8\", \"text/csv\", \"application/octet-stream\"]);",
                        "    });",
                        "    pm.test(\"Content-Disposition header present for attachment download\", function () {",
                        "        pm.expect(pm.response.headers.has(\"Content-Disposition\")).to.be.true;",
                        "    });",
                        "}",
                        "// 4. Handle JSON Responses",
                        "else if (contentType.includes(\"application/json\")) {",
                        "    const json = pm.response.json();",
                        "",
                        "    // Successful Standard REST API Responses (2xx)",
                        "    if (pm.response.code >= 200 && pm.response.code < 300) {",
                        "        // Standard ApiResponse<T> Envelope Validation",
                        "        if (json.hasOwnProperty(\"success\")) {",
                        "            pm.test(\"ApiResponse envelope conforms to contract (success, timestamp, correlationId)\", function () {",
                        "                pm.expect(json.success).to.be.a('boolean');",
                        "                pm.expect(json.timestamp).to.be.a('string');",
                        "                pm.expect(json.correlationId).to.be.a('string');",
                        "            });",
                        "",
                        "            // PagedResponse<T> Validation (when data contains pagination metadata)",
                        "            if (json.data && typeof json.data === 'object' && Array.isArray(json.data.content)) {",
                        "                pm.test(\"PagedResponse metadata conforms to contract (page, size, totalElements, totalPages)\", function () {",
                        "                    pm.expect(json.data.page).to.be.a('number');",
                        "                    pm.expect(json.data.size).to.be.a('number');",
                        "                    pm.expect(json.data.totalElements).to.be.a('number');",
                        "                    pm.expect(json.data.totalPages).to.be.a('number');",
                        "                    pm.expect(json.data.first).to.be.a('boolean');",
                        "                    pm.expect(json.data.last).to.be.a('boolean');",
                        "                });",
                        "            }",
                        "        }",
                        "    }",
                        "",
                        "    // Sanitized Error Responses (4xx, 5xx)",
                        "    if (pm.response.code >= 400) {",
                        "        pm.test(\"ErrorResponse envelope conforms to contract (status, error, code, message, timestamp)\", function () {",
                        "            pm.expect(json.status).to.eql(pm.response.code);",
                        "            pm.expect(json.error).to.be.a('string');",
                        "            pm.expect(json.code).to.be.a('string');",
                        "            pm.expect(json.message).to.be.a('string');",
                        "            pm.expect(json.timestamp).to.be.a('string');",
                        "            pm.expect(json.correlationId).to.be.a('string');",
                        "        });",
                        "        pm.test(\"Zero stack trace or internal SQL leakage in error response\", function () {",
                        "            const str = JSON.stringify(json);",
                        "            pm.expect(str).to.not.include(\".java:\");",
                        "            pm.expect(str).to.not.include(\"Exception\");",
                        "            pm.expect(str).to.not.include(\"SQLException\");",
                        "        });",
                        "    }",
                        "}"
                    ]
                }
            }
        ],
        "item": []
    }

    total_operations_generated = 0

    # 2. Iterate over the 5 partitioned groups
    for group_key, group_title, group_desc in GROUPS:
        group_spec_path = OPENAPI_DIR / f"openapi-{group_key}.json"
        group_spec = load_json(str(group_spec_path))
        group_paths = group_spec.get("paths", {})

        group_folder = {
            "name": group_title,
            "description": group_desc,
            "item": []
        }

        # Subfolders organized by tag
        subfolder_map = {}

        for path, path_item in group_paths.items():
            for method in ["get", "post", "put", "delete", "patch"]:
                if method not in path_item:
                    continue

                op = path_item[method]
                tags = op.get("tags", ["General"])
                tag_name = tags[0] if tags else "General"

                # Clean up tag display name
                clean_tag = tag_name.replace("-controller", "").replace("-", " ").title()
                if clean_tag.startswith("Workflows - "):
                    clean_tag = clean_tag.replace("Workflows - ", "")
                elif clean_tag.startswith("Operations - "):
                    clean_tag = clean_tag.replace("Operations - ", "")

                if clean_tag not in subfolder_map:
                    subfolder_map[clean_tag] = {
                        "name": clean_tag,
                        "item": []
                    }

                # Construct request item
                summary = op.get("summary") or op.get("operationId") or f"{method.upper()} {path}"
                description = op.get("description", summary)

                # Query parameters
                query_list = []
                for p in op.get("parameters", []):
                    if p.get("in") == "query":
                        p_name = p.get("name")
                        p_schema = p.get("schema", {})
                        p_def = "0" if p_name == "page" else ("20" if p_name == "size" else "")
                        if "example" in p:
                            p_def = str(p["example"])
                        elif "example" in p_schema:
                            p_def = str(p_schema["example"])
                        elif p_name == "entityType":
                            p_def = "Trainee"
                        elif p_name == "sourceSystem":
                            p_def = "STATE_PORTAL"

                        query_list.append({
                            "key": p_name,
                            "value": p_def,
                            "description": p.get("description", ""),
                            "disabled": not p.get("required", False)
                        })

                url_obj = format_url(path, query_list)

                # Request Headers
                headers = [
                    {
                        "key": "Accept",
                        "value": "application/json",
                        "type": "text"
                    }
                ]

                # Body Handling: Prioritize multipart/form-data for file upload endpoints
                body_obj = None
                request_body = op.get("requestBody", {})
                content_types = request_body.get("content", {})

                if "multipart/form-data" in content_types:
                    multipart_schema = content_types["multipart/form-data"].get("schema", {})
                    formdata_entries = [
                        {
                            "key": "file",
                            "type": "file",
                            "src": "",
                            "description": "Target CSV, JSON, or NDJSON file for bulk staging & ingestion"
                        }
                    ]
                    props = multipart_schema.get("properties", {})
                    for prop_name, prop_spec in props.items():
                        if prop_name != "file":
                            formdata_entries.append({
                                "key": prop_name,
                                "value": str(resolve_schema(prop_spec, components)),
                                "type": "text",
                                "description": prop_spec.get("description", "")
                            })
                    body_obj = {
                        "mode": "formdata",
                        "formdata": formdata_entries
                    }
                    if "application/json" in content_types:
                        description += "\n\n**Note**: This endpoint also accepts raw JSON payloads (`CreateImportBatchRequest`) for programmatic batch descriptor registration."
                elif "application/json" in content_types:
                    json_schema = content_types["application/json"].get("schema", {})
                    sample_payload = resolve_schema(json_schema, components)

                    # Custom payload overrides for special endpoints
                    if path == "/api/v1/auth/login":
                        sample_payload = {
                            "usernameOrEmail": "{{username}}",
                            "password": "{{password}}"
                        }
                    elif path == "/api/v1/auth/refresh":
                        sample_payload = {
                            "refreshToken": "{{refreshToken}}"
                        }

                    headers.append({
                        "key": "Content-Type",
                        "value": "application/json",
                        "type": "text"
                    })
                    body_obj = {
                        "mode": "raw",
                        "raw": json.dumps(sample_payload, indent=2),
                        "options": {
                            "raw": {
                                "language": "json"
                            }
                        }
                    }

                # Authentication overrides for public endpoints
                auth_override = None
                is_public = False
                if path in ["/api/v1/auth/login", "/api/v1/auth/refresh"] or "/api/v1/communication/webhooks/" in path:
                    is_public = True
                    auth_override = {
                        "type": "noauth"
                    }

                # Custom operation-level test scripts
                op_event = []
                if path == "/api/v1/auth/login":
                    op_event = [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "// Extract JWT access and refresh tokens from login response",
                                    "pm.test(\"Login successful and tokens captured\", function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const json = pm.response.json();",
                                    "    pm.expect(json.success).to.be.true;",
                                    "    pm.expect(json.data.accessToken).to.be.a('string');",
                                    "    pm.environment.set(\"accessToken\", json.data.accessToken);",
                                    "    if (json.data.refreshToken) {",
                                    "        pm.environment.set(\"refreshToken\", json.data.refreshToken);",
                                    "    }",
                                    "    if (json.data.userId) {",
                                    "        pm.environment.set(\"userId\", json.data.userId);",
                                    "    }",
                                    "});"
                                ]
                            }
                        }
                    ]
                elif path == "/api/v1/auth/refresh":
                    op_event = [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "// Update access token from token refresh response",
                                    "pm.test(\"Token refresh successful and access token rotated\", function () {",
                                    "    pm.response.to.have.status(200);",
                                    "    const json = pm.response.json();",
                                    "    pm.expect(json.success).to.be.true;",
                                    "    pm.expect(json.data.accessToken).to.be.a('string');",
                                    "    pm.environment.set(\"accessToken\", json.data.accessToken);",
                                    "    if (json.data.refreshToken) {",
                                    "        pm.environment.set(\"refreshToken\", json.data.refreshToken);",
                                    "    }",
                                    "});"
                                ]
                            }
                        }
                    ]
                elif "export" in path:
                    op_event = [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test(\"Streaming export response status code is 200\", function () {",
                                    "    pm.response.to.have.status(200);",
                                    "});",
                                    "pm.test(\"Streaming export Content-Type header present\", function () {",
                                    "    const ct = pm.response.headers.get('Content-Type') || '';",
                                    "    pm.expect(ct).to.be.oneOf(['text/csv;charset=UTF-8', 'text/csv', 'application/octet-stream', 'application/json']);",
                                    "});"
                                ]
                            }
                        }
                    ]
                else:
                    op_event = [
                        {
                            "listen": "test",
                            "script": {
                                "type": "text/javascript",
                                "exec": [
                                    "pm.test(\"Status code is successful\", function () {",
                                    "    pm.expect(pm.response.code).to.be.oneOf([200, 201, 202, 204]);",
                                    "});",
                                    "pm.test(\"Response conforms to ApiResponse contract\", function () {",
                                    "    const ct = pm.response.headers.get('Content-Type') || '';",
                                    "    if (ct.includes('application/json')) {",
                                    "        const json = pm.response.json();",
                                    "        pm.expect(json).to.be.an('object');",
                                    "        if (json.hasOwnProperty('success')) {",
                                    "            pm.expect(json.success).to.be.a('boolean');",
                                    "        }",
                                    "    }",
                                    "});"
                                ]
                            }
                        }
                    ]

                request_item = {
                    "name": summary,
                    "request": {
                        "method": method.upper(),
                        "header": headers,
                        "url": url_obj,
                        "description": description
                    }
                }

                if body_obj:
                    request_item["request"]["body"] = body_obj

                if auth_override:
                    request_item["request"]["auth"] = auth_override

                if op_event:
                    request_item["event"] = op_event

                subfolder_map[clean_tag]["item"].append(request_item)
                total_operations_generated += 1

        # Add subfolders to group folder sorted by name
        for sub_name in sorted(subfolder_map.keys()):
            group_folder["item"].append(subfolder_map[sub_name])

        collection["item"].append(group_folder)

    # Save collection
    collection_path = INTEGRATION_DIR / "sih26135-postman-collection.json"
    save_json(str(collection_path), collection)

    print(f"Total operations generated in Postman collection: {total_operations_generated}")

    # 3. Generate Local Environment Configuration
    environment = {
        "id": "sih26135-local-environment",
        "name": "SIH-26135 Local Environment",
        "values": [
            {
                "key": "baseUrl",
                "value": "http://localhost:8080",
                "type": "default",
                "enabled": True
            },
            {
                "key": "apiVersion",
                "value": "v1.0.0-PROD",
                "type": "default",
                "enabled": True
            },
            {
                "key": "adminUsername",
                "value": "admin",
                "type": "default",
                "enabled": True
            },
            {
                "key": "adminPassword",
                "value": "",
                "type": "secret",
                "enabled": True
            },
            {
                "key": "traineeUsername",
                "value": "trainee@example.org",
                "type": "default",
                "enabled": True
            },
            {
                "key": "traineePassword",
                "value": "",
                "type": "secret",
                "enabled": True
            },
            {
                "key": "username",
                "value": "admin",
                "type": "default",
                "enabled": True
            },
            {
                "key": "password",
                "value": "",
                "type": "secret",
                "enabled": True
            },
            {
                "key": "accessToken",
                "value": "",
                "type": "secret",
                "enabled": True
            },
            {
                "key": "refreshToken",
                "value": "",
                "type": "secret",
                "enabled": True
            },
            {
                "key": "sampleTraineeId",
                "value": "1005143",
                "type": "default",
                "enabled": True
            },
            {
                "key": "traineeId",
                "value": "1005143",
                "type": "default",
                "enabled": True
            },
            {
                "key": "sampleBatchId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "batchId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "sampleCourseId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "courseId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "sampleSchemeId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "schemeId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "sampleCenterId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "centerId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "sampleProviderId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "providerId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "sampleAgencyId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "agencyId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "sampleAssessorId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "assessorId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "sampleAttendanceId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "attendanceId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "sampleEmployerId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "employerId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "stateId",
                "value": "10",
                "type": "default",
                "enabled": True
            },
            {
                "key": "districtId",
                "value": "101",
                "type": "default",
                "enabled": True
            },
            {
                "key": "ruleId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "issueId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "importBatchId",
                "value": "1",
                "type": "default",
                "enabled": True
            },
            {
                "key": "jobId",
                "value": "1",
                "type": "default",
                "enabled": True
            }
        ],
        "_postman_variable_scope": "environment"
    }

    env_path = INTEGRATION_DIR / "sih26135-local.postman_environment.json"
    save_json(str(env_path), environment)
    print("=" * 80)
    print("Generation Completed Successfully!")
    print("=" * 80)

if __name__ == "__main__":
    generate_collection()
