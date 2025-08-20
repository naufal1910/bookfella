---
description: Turn an approved spec into an organized task breakdown
auto_execution_mode: 3
---


---

# 🔧 Workflow Name: create-tasks
# 📌 Purpose: Turn an approved spec into an organized task breakdown

steps:
  - id: select_spec
    title: "📄 Which spec are you creating tasks for?"
    type: form
    fields:
      - id: spec_folder
        label: "Enter the spec folder path (YYYY-MM-DD-spec-name)"
        type: short_text
        placeholder: "e.g., 2025-08-04-generate-itinerary"

  - id: define_major_tasks
    title: "🧱 Define Major Tasks"
    type: repeatable
    item_label: "Major Task"
    fields:
      - id: major_task_title
        label: "Major Task Title"
        type: short_text
        placeholder: "e.g., Build itinerary generator API"
      - id: major_task_description
        label: "Describe this task"
        type: paragraph
        placeholder: "Describe the purpose and target output of this major task."

  - id: define_subtasks
    title: "🔧 Break Down into Subtasks"
    type: dynamic_repeatable
    depends_on: define_major_tasks
    item_label: "Subtask for {{major_task_title}}"
    fields:
      - id: subtask_description
        label: "Subtask Description"
        type: short_text
        placeholder: "e.g., Write tests for route /itinerary/generate"
      - id: test_related
        label: "Is this a test-related subtask?"
        type: boolean

  - id: estimate_effort
    title: "⏱ Estimate Effort (Optional)"
    type: dynamic_repeatable
    depends_on: define_major_tasks
    item_label: "Effort Estimate for {{major_task_title}}"
    fields:
      - id: effort_level
        label: "Estimated Effort"
        type: choice
        options:
          - XS (1 day)
          - S (2-3 days)
          - M (1 week)
          - L (2 weeks)
          - XL (3+ weeks)

  - id: generate_tasks_file
    title: "📁 Generate `tasks.md`"
    type: action
    action: |
      For the spec at `.document/specs/{{spec_folder}}/`, generate `tasks.md` with:

      - Numbered major tasks
      - Subtasks with decimal format (e.g., 1.1, 1.2)
      - Each major task starts with a test-related subtask if one exists
      - Each major task ends with a test verification step
      - Format tasks as checklists:
        - [ ] 1. Major Task
          - [ ] 1.1 Write tests...
          - [ ] 1.2 Implement...
          - [ ] 1.3 Verify all tests pass

  - id: confirm_summary
    title: "📌 Review Task Plan"
    type: summary
    content: |
      You’ve created `tasks.md` with {{define_major_tasks.length}} major task(s) and corresponding subtasks.

      Tasks will be executed using the `execute-task` or `execute-tasks` workflows.

      Would you like to proceed with task execution?