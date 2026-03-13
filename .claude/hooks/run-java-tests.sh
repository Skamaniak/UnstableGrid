#!/bin/bash
INPUT=$(cat)
FILE_PATH=$(echo "$INPUT" | jq -r '.tool_input.file_path // empty')

# Only run tests if the edited file is a Java source file (not test files)
if [[ "$FILE_PATH" == *.java ]] && [[ "$FILE_PATH" != *src/test/* ]]; then
  cd "$CLAUDE_PROJECT_DIR" && ./gradlew core:test --quiet 2>&1
fi

exit 0
