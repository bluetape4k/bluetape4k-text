#!/usr/bin/env python3
"""CI path-filter policy regression checks for shared Gradle inputs."""

from __future__ import annotations

import re
import sys
from fnmatch import fnmatchcase
from pathlib import Path


WORKFLOW = Path(__file__).parents[1] / "workflows" / "ci.yml"
NIGHTLY_WORKFLOW = Path(__file__).parents[1] / "workflows" / "nightly-tests.yml"
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
MODULE_DEPENDENCIES = {
    "tokenizer-core": (),
    "tokenizer-japanese": ("tokenizer-core",),
    "tokenizer-korean": ("tokenizer-core",),
    "lingua": (),
    "text-search": (),
    "examples": (
        "tokenizer-core",
        "tokenizer-japanese",
        "tokenizer-korean",
        "lingua",
        "text-search",
    ),
}
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


def filter_patterns(workflow: str, filter_name: str) -> tuple[str, ...]:
    block = filter_block(workflow, filter_name)
    return tuple(
        match.group(1)
        for match in re.finditer(
            r"^\s+-\s+['\"]([^'\"]+)['\"]\s*$", block, re.MULTILINE
        )
    )


def path_matches(pattern: str, path: str) -> bool:
    normalized_pattern = pattern.removeprefix("./")
    normalized_path = path.removeprefix("./")
    if normalized_pattern.endswith("/**"):
        prefix = normalized_pattern[:-3].rstrip("/")
        return normalized_path == prefix or normalized_path.startswith(f"{prefix}/")
    return fnmatchcase(normalized_path, normalized_pattern)


def selected_filters(workflow: str, changed_paths: list[str]) -> tuple[str, ...]:
    selected: list[str] = []
    for filter_name in FILTERS:
        patterns = filter_patterns(workflow, filter_name)
        if any(
            path_matches(pattern, path)
            for pattern in patterns
            for path in changed_paths
        ):
            selected.append(filter_name)
    return tuple(selected)


def nightly_test_jobs(workflow: str) -> set[str]:
    return set(re.findall(r"^  (test-[A-Za-z0-9_-]+):\n", workflow, re.MULTILINE))


def validate_workflow(workflow: str, nightly_workflow: str | None = None) -> list[str]:
    errors: list[str] = []

    for filter_name in FILTERS:
        try:
            patterns = filter_patterns(workflow, filter_name)
        except ValueError as error:
            errors.append(str(error))
            continue

        for shared_path in SHARED_PATHS:
            if shared_path not in patterns:
                errors.append(f"{filter_name} is missing shared path: {shared_path}")

        for dependency in MODULE_DEPENDENCIES[filter_name]:
            dependency_path = f"{dependency}/**"
            if dependency_path not in patterns:
                errors.append(
                    f"{filter_name} is missing dependency path: {dependency_path}"
                )

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

    if nightly_workflow is not None:
        nightly_jobs = nightly_test_jobs(nightly_workflow)
        for filter_name in FILTERS:
            job_name = f"test-{filter_name}"
            if job_name not in nightly_jobs:
                errors.append(f"Nightly is missing module test job: {job_name}")

    return errors


def main() -> int:
    workflow = WORKFLOW.read_text(encoding="utf-8")
    nightly_workflow = NIGHTLY_WORKFLOW.read_text(encoding="utf-8")
    errors = validate_workflow(workflow, nightly_workflow)

    if errors:
        for error in errors:
            print(f"::error::{error}")
        return 1

    print(
        f"Validated {len(FILTERS)} filters against {len(SHARED_PATHS)} shared Gradle paths "
        f"and {sum(len(dependencies) for dependencies in MODULE_DEPENDENCIES.values())} "
        "dependency edges; expected-job skip guard and Nightly module coverage are present."
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
