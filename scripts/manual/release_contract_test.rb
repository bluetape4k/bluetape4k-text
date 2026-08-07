require "minitest/autorun"
require "tmpdir"
require "yaml"

require_relative "release_contract"

class ReleaseContractTest < Minitest::Test
  RELEASE_SHA = "aead213d2d25307d7d3684226943a5f95c7411f2"

  def test_accepts_annotated_tag_at_expected_commit
    runner = git_runner(type: "tag\n", sha: "#{RELEASE_SHA}\n")
    assert ReleaseContract.new(root: Dir.pwd, tag: "0.3.0", expected_sha: RELEASE_SHA, git_runner: runner).validate!
  end

  def test_rejects_lightweight_tag
    runner = git_runner(type: "commit\n", sha: "#{RELEASE_SHA}\n")
    error = assert_raises(ReleaseContract::Violation) do
      ReleaseContract.new(root: Dir.pwd, tag: "0.3.0", expected_sha: RELEASE_SHA, git_runner: runner).validate!
    end
    assert_includes error.message, "must be annotated"
  end

  def test_rejects_release_commit_that_does_not_match_tag
    runner = git_runner(type: "tag\n", sha: "#{'0' * 40}\n")
    error = assert_raises(ReleaseContract::Violation) do
      ReleaseContract.new(root: Dir.pwd, tag: "0.3.0", expected_sha: RELEASE_SHA, git_runner: runner).validate!
    end
    assert_includes error.message, "releaseCommit"
  end

  private

  def git_runner(type:, sha:)
    lambda do |arguments|
      case arguments.first
      when "cat-file" then [type, true]
      when "rev-parse" then [sha, true]
      else ["", false]
      end
    end
  end
end
