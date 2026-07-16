require "open3"

class ReleaseContract
  class Violation < StandardError; end

  def initialize(root:, tag:, expected_sha:, git_runner: nil)
    @root = File.expand_path(root)
    @tag = tag
    @expected_sha = expected_sha
    @git_runner = git_runner || method(:run_git)
  end

  def validate!
    type, found = @git_runner.call(["cat-file", "-t", "refs/tags/#{@tag}"])
    raise Violation, "release tag not found: #{@tag}" unless found
    raise Violation, "release tag must be annotated: #{@tag}" unless type.strip == "tag"

    sha, found = @git_runner.call(["rev-parse", "--verify", "refs/tags/#{@tag}^{commit}"])
    raise Violation, "release tag not found: #{@tag}" unless found
    unless sha.strip.casecmp?(@expected_sha)
      raise Violation, "releaseCommit #{sha.strip} does not match #{@expected_sha}"
    end

    true
  end

  private

  def run_git(arguments)
    stdout, _stderr, status = Open3.capture3("git", "-C", @root, *arguments)
    [stdout, status.success?]
  end
end
