require "minitest/autorun"

require_relative "release_inventory"

class ReleaseInventoryTest < Minitest::Test
  RELEASE_SHA = "aead213d2d25307d7d3684226943a5f95c7411f2"

  def test_accepts_the_pinned_release_tree
    inventory = ReleaseInventory.new(
      root: Dir.pwd,
      ref: "0.3.0",
      expected_sha: RELEASE_SHA,
      git_runner: git_runner(REQUIRED_RELEASE_PATHS),
    ).validate!

    assert_equal 6, inventory.fetch("modules").length
    assert_equal 3, inventory.fetch("examples").length
    assert_equal 2, inventory.fetch("evidence").length
  end

  def test_rejects_a_missing_example
    paths = REQUIRED_RELEASE_PATHS.reject { |path| path.start_with?("examples/lingua-examples/") }
    error = assert_raises(ReleaseInventory::Violation) do
      ReleaseInventory.new(
        root: Dir.pwd,
        ref: "0.3.0",
        expected_sha: RELEASE_SHA,
        git_runner: git_runner(paths),
      ).validate!
    end
    assert_includes error.message, "examples/lingua-examples"
  end

  private

  REQUIRED_RELEASE_PATHS = [
    "bom/build.gradle.kts",
    "tokenizer-core/build.gradle.kts",
    "tokenizer-korean/build.gradle.kts",
    "tokenizer-japanese/build.gradle.kts",
    "lingua/build.gradle.kts",
    "text-search/build.gradle.kts",
    "examples/text-search-examples/build.gradle.kts",
    "examples/lingua-examples/build.gradle.kts",
    "examples/tokenizer-safety-examples/build.gradle.kts",
    "docs/superpowers/research/2026-05-27-issue-86-quality-report.md",
    "docs/benchmark/2026-06-04-issue-97-ahocorasick-baselines.json",
  ].freeze

  def git_runner(paths)
    lambda do |arguments|
      case arguments.first
      when "rev-parse" then ["#{RELEASE_SHA}\n", true]
      when "ls-tree" then ["#{paths.join("\n")}\n", true]
      else ["", false]
      end
    end
  end
end
