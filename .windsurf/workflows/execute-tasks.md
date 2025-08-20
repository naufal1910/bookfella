---
description: Execute all tasks for the Super-Sale Mini Booking API spec with Big Travel Company-scale perf checks and CI stubs
auto_execution_mode: 3
---

# 🔧 Workflow Name: execute-tasks
# 📌 Purpose: Execute all parent tasks in a spec by chaining `execute-task`, validate with full test + perf suite, and prepare PR.

steps:
  - id: select_spec
    title: "📄 Select Spec to Execute"
    type: form
    fields:
      - id: spec_folder
        label: "Spec Folder (YYYY-MM-DD-feature-name)"
        type: short_text
        placeholder: "e.g., 2025-08-20-super-sale-booking"

  - id: confirm_execution
    title: "🚀 Ready to Execute All Tasks?"
    type: message
    content: |
      This workflow will:
      - Load `tasks.md` from the selected spec folder
      - Identify all incomplete parent tasks
      - Execute each task via `execute-task` cascade
      - Run full unit + integration + perf suite at the end
      - Push code and create a PR

  - id: branch_setup
    title: "🌿 Git Branch Setup"
    type: action
    action: |
      Use the spec name (excluding the date) to create or switch to a Git branch.
      Example:
      - Folder: `2025-08-20-super-sale-booking`
      - Branch: `super-sale-booking`

  - id: task_loop
    title: "🔁 Loop Through All Tasks"
    type: loop
    source: |
      Parse `tasks.md` from `.document/specs/{{spec_folder}}/`
      Identify all uncompleted parent tasks (e.g., 1, 2, 3)
    steps:
      - use_workflow: execute-task
        with:
          spec_folder: "{{spec_folder}}"
          task_number: "{{current_task_number}}"

  - id: run_all_tests
    title: "🧪 Run Full Test & Perf Suite"
    type: action
    action: |
      Run the full suite after all tasks:
      - Unit tests: `mvn test`
      - Integration tests: Redis (TTL=60s), H2 Oracle mode DB, Kafka outbox
      - Perf tests: `k6 run perf/search.js` (p95 <200ms @ 800 rps), `k6 run perf/reservations.js` (p95 <350ms @ 150 rps)
      - Observability: confirm Prometheus `/actuator/prometheus` scrape and Grafana p95/p99 panels visible
      All must pass before proceeding.

  - id: create_pr
    title: "📦 Create Pull Request"
    type: action
    action: |
      - Commit changes from spec branch
      - Push to GitHub
      - Open a PR to `main`
      - Commit message format: `feat({{spec_folder}}): complete feature`
      - PR description should include:
        - Key endpoints implemented
        - Perf thresholds achieved
        - Evidence: Grafana screenshot or k6 summary

  - id: check_roadmap_update
    title: "🗺️ Check If Roadmap Should Be Updated"
    type: choice
    options:
      - label: "Yes, this spec completes a roadmap feature"
        value: update_roadmap
      - label: "No, this spec is internal/support-only"
        value: skip_roadmap

  - id: update_roadmap
    if: check_roadmap_update == "update_roadmap"
    title: "📌 Mark Feature as Completed in Roadmap"
    type: action
    action: |
      In `docs/roadmap.md`, locate the feature implemented by this spec and:
      - Mark as complete `[x]`
      - Add notes on scale/perf outcomes (e.g., "Validated 800 rps search with p95 <200ms")

  - id: summary
    title: "✅ Execution Summary"
    type: message
    content: |
      🎉 All tasks for `{{spec_folder}}` have been executed.

      🔍 Tests: ✅ Unit + integration + perf passed  
      📊 Perf: ✅ Search <200ms p95 @800rps, Reservations <350ms p95 @150rps  
      🔃 Git: Branch pushed & PR created  
      🗺️ Roadmap: {{check_roadmap_update == "update_roadmap" ? '✔ Updated' : '❌ Skipped'}}

      You may now continue to the next spec or start another cycle!
