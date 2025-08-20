---
description: Create a detailed feature spec for Super-Sale Mini Booking API based on roadmap or idea
auto_execution_mode: 3
---

# 🔧 Workflow Name: create-spec
# 📌 Purpose: Produce a complete feature specification (functional + technical) aligned with Super-Sale Mini Booking API PRD

steps:
  - id: spec_source
    title: "🧩 Where is this spec coming from?"
    type: choice
    options:
      - label: "Use next item from the roadmap (e.g., Search API, Reservation API)"
        value: from_roadmap
      - label: "I have a specific idea"
        value: custom

  - id: fetch_from_roadmap
    if: spec_source == "from_roadmap"
    title: "📋 Select roadmap item"
    type: form
    fields:
      - id: roadmap_feature
        label: "Enter the next uncompleted roadmap feature"
        type: short_text
        placeholder: "Example: Implement Reservation API with Kafka outbox"

  - id: enter_custom_spec
    if: spec_source == "custom"
    title: "💡 Describe your feature idea"
    type: form
    fields:
      - id: spec_title
        label: "Feature Title"
        type: short_text
        placeholder: "e.g., Add Idempotency to Reservations"
      - id: spec_idea
        label: "Describe what this feature should do"
        type: paragraph
        placeholder: "e.g., Ensure reservation POST requests are idempotent via Redis SET NX 600s"

  - id: clarify_scope
    if: true
    title: "🔍 Clarify Scope & Details"
    type: form
    fields:
      - id: feature_goals
        label: "What is the goal of this feature?"
        type: paragraph
      - id: in_scope
        label: "What is IN scope?"
        type: list
        placeholder: "e.g., Implement /api/search with Redis TTL 60s cache-aside"
      - id: out_of_scope
        label: "What is OUT of scope? (optional)"
        type: list
        placeholder: "e.g., Payment, Cancellations"
      - id: integration_points
        label: "What needs to be integrated?"
        type: list
        placeholder: "e.g., Oracle RDBMS, Kafka topic reservations.created, Elasticsearch"

  - id: write_user_stories
    title: "🧑‍💻 Define User Stories"
    type: repeatable
    item_label: "User Story"
    fields:
      - id: actor
        label: "Who is the user?"
        type: short_text
      - id: action
        label: "What do they want to do?"
        type: short_text
      - id: benefit
        label: "What is the benefit?"
        type: short_text
      - id: flow_description
        label: "Workflow / Notes"
        type: paragraph
        placeholder: "Describe flow: cache → ES → response OR tx → DB → Kafka"

  - id: generate_spec_files
    title: "📁 Generate Spec Files"
    type: action
    action: |
      Create the following files under `.document/specs/YYYY-MM-DD-{{spec_title_kebab}}/`:
      - `spec.md` → full feature spec (Problem, Goals, APIs, Data Models, Perf targets, Observability, Acceptance Criteria)
      - `spec-lite.md` → 1-pager recruiter/executive summary
      - `sub-specs/technical-spec.md` → deep dive (API contracts, DDL, ES mappings, Nginx configs, Prometheus scrape, k6 plan)
      Conditionally:
      - `database-schema.md` if DB changes needed (include Oracle/H2 DDL with indexes)
      - `api-spec.md` if new endpoints are introduced (use OpenAPI-like YAML)

  - id: confirm_tasks
    title: "✅ Confirm Feature Scope"
    type: summary
    content: |
      You're about to create a feature spec with:
      - Title: {{spec_title || roadmap_feature}}
      - Goal: {{feature_goals}}
      - Scope: {{in_scope.join(', ')}}
      - Out of Scope: {{out_of_scope.join(', ') || 'N/A'}}
      - Integrations: {{integration_points.join(', ')}}
      - Stories: {{user_stories.length}} user story(ies)

  - id: next_step_suggestion
    title: "🚀 Ready to break into tasks?"
    type: message
    content: |
      Your spec is ready. Run the `create-tasks` workflow to break this into an actionable list of tasks with DoD.