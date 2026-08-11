#!/usr/bin/env python3
"""CI path-filter policy regression checks for shared Gradle inputs."""

from __future__ import annotations

import re
import sys
from pathlib import Path


WORKFLOW = Path(__file__).parents[1] / "workflows" / "ci.yml"
FILTERS = (
    "tokenizer-core",
    "tokenizer-japanese",
    "tokenizer-korean",
    "lingua",
    "text-search",
    "examples",
)
SHARED_PATHS = (
    "settings.gradle.kts",
    "build.gradle.kts",
    "buildSrc/**",
    "gradle/**",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
)
EXPECTED_RESULT_VARIABLES = (
    "CORE_EXPECTED",
    "CORE_RESULT",
    "JAPANESE_EXPECTED",
    "JAPANESE_RESULT",
    "KOREAN_EXPECTED",
    "KOREAN_RESULT",
    "LINGUA_EXPECTED",
    "LINGUA_RESULT",
    "TEXT_SEARCH_EXPECTED",
    "TEXT_SEARCH_RESULT",
    "EXAMPLES_EXPECTED",
    "EXAMPLES_RESULT",
)


def filter_block(workflow: str, filter_name: str) -> str:
    marker = f"            {filter_name}:\n"
    start = workflow.find(marker)
    if start < 0:
        raise ValueError(f"missing path filter: {filter_name}")

    body_start = start + len(marker)
    next_filter = re.search(r"^            [A-Za-z0-9_-]+:\n", workflow[body_start:], re.MULTILINE)
    body_end = body_start + next_filter.start() if next_filter else len(workflow)
    return workflow[body_start:body_end]


def main() -> int:
    workflow = WORKFLOW.read_text(encoding="utf-8")
    errors: list[str] = []

    for filter_name in FILTERS:
        try:
            block = filter_block(workflow, filter_name)
        except ValueError as error:
            errors.append(str(error))
            continue

        for shared_path in SHARED_PATHS:
            if f"'{shared_path}'" not in block and f'"{shared_path}"' not in block:
                errors.append(f"{filter_name} is missing shared path: {shared_path}")

    for variable in EXPECTED_RESULT_VARIABLES:
        if variable not in workflow:
            errors.append(f"ci-status is missing expected-job result variable: {variable}")

    required_guard_tokens = (
        "expected_tests=",
        '[[ "$expected" == "true" && "$result" == "skipped" ]]',
        "Unexpected skipped test",
    )
    for token in required_guard_tokens:
        if token not in workflow:
            errors.append(f"ci-status is missing expected-job guard token: {token}")

    if errors:
        for error in errors:
            print(f"::error::{error}")
        return 1

    print(
        f"Validated {len(FILTERS)} filters against {len(SHARED_PATHS)} shared Gradle paths "
        "and expected-job skip guard."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
