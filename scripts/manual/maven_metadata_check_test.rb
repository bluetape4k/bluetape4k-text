require "minitest/autorun"
require "tmpdir"

require_relative "maven_metadata_check"

class MavenMetadataCheckTest < Minitest::Test
  EXPECTED_VERSION = "0.4.0"

  def test_accepts_all_published_artifacts_at_the_expected_version
    with_project_root do |root|
      result = MavenMetadataCheck.new(
        root: root,
        metadata_fetcher: metadata_fetcher,
      ).validate!

      assert_equal EXPECTED_VERSION, result.fetch("project_version")
      assert_equal EXPECTED_VERSION, result.fetch("expected_version")
      assert_equal 6, result.fetch("artifacts").length
      assert_equal MavenMetadataCheck::ARTIFACTS, result.fetch("artifacts").map { |entry| entry.fetch("artifact") }
      assert result.fetch("artifacts").all? { |entry| entry.fetch("status") == "available" }
    end
  end

  def test_allows_an_explicit_release_version_from_a_current_checkout
    with_project_root do |root|
      result = MavenMetadataCheck.new(
        root: root,
        version: "0.3.0",
        metadata_fetcher: metadata_fetcher(version: "0.3.0"),
      ).validate!

      assert_equal EXPECTED_VERSION, result.fetch("project_version")
      assert_equal "0.3.0", result.fetch("expected_version")
    end
  end

  def test_reports_missing_and_stale_metadata
    with_project_root do |root|
      error = assert_raises(MavenMetadataCheck::Violation) do
        MavenMetadataCheck.new(
          root: root,
          metadata_fetcher: metadata_fetcher(
            missing: ["tokenizer-core"],
            stale: ["bluetape4k-text-bom"],
          ),
        ).validate!
      end

      assert_includes error.message, "metadata missing: io.github.bluetape4k.text:tokenizer-core:#{EXPECTED_VERSION}"
      assert_includes error.message, "metadata stale: io.github.bluetape4k.text:bluetape4k-text-bom:#{EXPECTED_VERSION}"
    end
  end

  private

  def with_project_root
    Dir.mktmpdir do |root|
      File.write(
        File.join(root, "gradle.properties"),
        "projectGroup=#{MavenMetadataCheck::GROUP}\nbaseVersion=#{EXPECTED_VERSION}\nsnapshotVersion=\n",
      )
      yield root
    end
  end

  def metadata_fetcher(version: EXPECTED_VERSION, missing: [], stale: [])
    lambda do |artifact, _url|
      raise MavenMetadataCheck::FetchError, "HTTP 404" if missing.include?(artifact)

      metadata_version = stale.include?(artifact) ? "0.3.0" : version
      <<~XML
        <metadata>
          <groupId>#{MavenMetadataCheck::GROUP}</groupId>
          <artifactId>#{artifact}</artifactId>
          <versioning>
            <versions>
              <version>#{metadata_version}</version>
            </versions>
          </versioning>
        </metadata>
      XML
    end
  end
end
