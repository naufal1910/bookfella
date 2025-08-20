---
description: Execute a single Super-Sale Mini Booking API task with TDD, Big Travel Company-scale perf checks, and observability validation
auto_execution_mode: 3
---

🔧 Workflow Name: execute-task
# 📌 Purpose: Execute one major task (and subtasks) for the Super-Sale Mini Booking API using TDD and perf validation

steps:
  - id: select_task
    title: "📋 Select Task to Execute"
    type: form
    fields:
      - id: spec_folder
        label: "Spec Folder (YYYY-MM-DD-task-name)"
        type: short_text
      - id: task_number
        label: "Which parent task number are you working on?"
        type: number
        placeholder: "e.g., 1"

  - id: load_task_and_spec
    title: "📂 Load Task & Technical Spec"
    type: action
    action: |
      Load from:
      - `tasks.md` → task {{task_number}} and subtasks
      - `technical-spec.md` (Super-Sale Mini Booking API) → only sections relevant to this task
      - `.windsurf/rules/best-practices.md`, `code-style.md`, `tech-stack.md` → enforce dev standards

  - id: summarize_requirements
    title: "📌 Confirm What You’re Building"
    type: summary
    content: |
      You're about to implement:
      - Task: {{task_number}} from `tasks.md`
      - Subtasks: parsed from e.g., 1.1, 1.2, 1.3
      - Scale anchors:
        - Search SLO: p95 <200ms at 800 rps (Redis TTL=60s → ES)
        - Reservation SLO: p95 <350ms at 150 rps (RDBMS + Kafka outbox)
        - Gateway: Nginx `limit_req 200r/s burst=100`
        - Metrics: /actuator/prometheus scrape, Grafana p95/p99 panels

  - id: write_tests
    title: "🧪 Write Tests First"
    type: action
    action: |
      Implement the first subtask: **Write failing tests** for this task.
      Examples:
      - For `/api/search`: write JUnit slice tests for cache-miss and cache-hit (Redis key: `search:q:{query}` TTL=60s)
      - For `/api/reservations`: write JUnit tests verifying idempotency (Redis SET NX 600s), DB persistence, Kafka event published
      Use conventions from `code-style.md` (naming, DTO separation, structured logging).
    completion_message: "✅ Tests written and failing as expected?"

  - id: implement_logic
    title: "🔨 Implement Feature"
    type: repeatable
    item_label: "Implementation Subtask"
    fields:
      - id: step_description
        label: "Describe sub-implementation step"
        type: paragraph
        placeholder: "E.g., Implement Redis cache-aside for /api/search"
    instructions: |
      For each subtask:
      - Write minimal code to make failing test pass
      - Use DB schema, ES mapping, and Kafka config from spec
      - Ensure idempotency, transaction boundaries, and observability are covered

  - id: verify_tests
    title: "✅ Run Task-Specific Tests"
    type: action
    action: |
      Run only the tests related to this task (e.g., `mvn test -Dtest=SearchControllerTest`).
      Confirm:
      - Unit tests pass
      - Integration tests (DB, Redis, Kafka) pass
      - No regressions

  - id: perf_validation
    title: "📊 Run Performance Validation"
    type: action
    action: |
      Use k6 scripts provided in `perf/`:
      - For /api/search: validate p95 <200ms @ 800 rps
      - For /api/reservations: validate p95 <350ms @ 150 rps
      Check Grafana panels: latency, error rate <1%, cache hit ratio
      Fix logic or config if thresholds fail.

  - id: update_task_status
    title: "📒 Update Task Status"
    type: choice
    options:
      - label: "Mark task as complete"
        value: complete
      - label: "Task is blocked"
        value: blocked
    instructions: |
      If blocked, document what you tried, including failing tests, perf evidence, or config gaps.

  - id: log_blocking_issue
    if: update_task_status == "blocked"
    title: "⚠️ Log Blocking Issue"
    type: form
    fields:
      - id: blocking_description
        label: "What is blocking you?"
        type: paragraph
        placeholder: "e.g., Redis TTL not respected, Kafka topic missing"
      - id: attempts
        label: "How many approaches did you try?"
        type: number
    instructions: |
      After ≥3 attempts, mark this as BLOCKED in `tasks.md` with ⚠️

  - id: done
    title: "🎉 Task Execution Complete"
    type: message
    content: |
      Finished executing Task {{task_number}}.
      ✅ Tests written and verified  
      ✅ Logic implemented  
      ✅ Perf thresholds validated  
      {{update_task_status == "blocked" ? "⚠️ Task marked as BLOCKED" : "✅ Task marked as COMPLETE"}}

      Next: run `execute-task` again for another task, or `execute-tasks` for batch execution.