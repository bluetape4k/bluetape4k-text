#!/usr/bin/env ruby

require "net/http"
require "optparse"
require "rexml/document"
require "rexml/xpath"
require "timeout"
require "uri"

class MavenMetadataCheck
  GROUP = "io.github.bluetape4k.text"
  ARTIFACTS = %w[
    tokenizer-core
    tokenizer-japanese
    tokenizer-korean
    lingua
    text-search
    bluetape4k-text-bom
  ].freeze
  DEFAULT_BASE_URL = "https://repo.maven.apache.org/maven2"
  HTTP_TIMEOUT_SECONDS = 10

  class Violation < StandardError; end
  class FetchError < StandardError; end

  def initialize(root:, version: nil, base_url: DEFAULT_BASE_URL, metadata_fetcher: nil)
    @root = File.expand_path(root)
    @project_version = project_version
    @expected_version = version || @project_version
    @base_url = base_url.sub(%r{/+\z}, "")
    @metadata_fetcher = metadata_fetcher || method(:fetch_metadata)
  end

  def validate!
    reports = ARTIFACTS.map { |artifact| validate_artifact(artifact) }
    errors = reports.each_with_object([]) do |report, collected|
      error = report["error"]
      collected << error if error
    end
    raise Violation, errors.join("\n") unless errors.empty?

    {
      "group" => GROUP,
      "project_version" => @project_version,
      "expected_version" => @expected_version,
      "artifacts" => reports,
    }
  end

  private

  def validate_artifact(artifact)
    coordinate = "#{GROUP}:#{artifact}:#{@expected_version}"

    begin
      body = @metadata_fetcher.call(artifact, metadata_url(artifact))
      versions = parse_versions(body)
    rescue FetchError => error
      return {
        "artifact" => artifact,
        "status" => "missing",
        "error" => "metadata missing: #{coordinate} (#{error.message})",
      }
    rescue REXML::ParseException => error
      return {
        "artifact" => artifact,
        "status" => "invalid",
        "error" => "metadata invalid: #{coordinate} (#{error.message})",
      }
    end

    if versions.include?(@expected_version)
      {
        "artifact" => artifact,
        "status" => "available",
        "versions" => versions,
      }
    else
      {
        "artifact" => artifact,
        "status" => "stale",
        "versions" => versions,
        "error" => "metadata stale: #{coordinate} (available: #{versions.join(", ")})",
      }
    end
  end

  def project_version
    properties_path = File.join(@root, "gradle.properties")
    properties = File.foreach(properties_path).each_with_object({}) do |line, values|
      key, value = line.strip.split("=", 2)
      next if key.nil? || key.empty? || key.start_with?("#")

      values[key] = value.to_s.strip
    end

    base_version = properties.fetch("baseVersion") do
      raise Violation, "gradle.properties is missing baseVersion"
    end
    "#{base_version}#{properties.fetch("snapshotVersion", "")}"
  rescue Errno::ENOENT
    raise Violation, "gradle.properties not found: #{properties_path}"
  end

  def metadata_url(artifact)
    "#{@base_url}/#{GROUP.tr(".", "/")}/#{artifact}/maven-metadata.xml"
  end

  def parse_versions(body)
    document = REXML::Document.new(body)
    REXML::XPath.match(document, "/metadata/versioning/versions/version")
      .map { |version| version.text.to_s.strip }
      .reject(&:empty?)
      .uniq
  end

  def fetch_metadata(artifact, url)
    uri = URI(url)
    response = Net::HTTP.start(
      uri.host,
      uri.port,
      use_ssl: uri.scheme == "https",
      open_timeout: HTTP_TIMEOUT_SECONDS,
      read_timeout: HTTP_TIMEOUT_SECONDS,
    ) { |http| http.get(uri.request_uri) }

    unless response.is_a?(Net::HTTPSuccess)
      raise FetchError, "HTTP #{response.code} for #{artifact}"
    end

    response.body
  rescue Timeout::Error, SocketError, SystemCallError => error
    raise FetchError, error.message
  end
end

if $PROGRAM_NAME == __FILE__
  options = {
    base_url: MavenMetadataCheck::DEFAULT_BASE_URL,
    root: Dir.pwd,
  }
  parser = OptionParser.new do |arguments|
    arguments.banner = "Usage: ruby scripts/manual/maven_metadata_check.rb [options]"
    arguments.on("--version VERSION", "Published version to verify (defaults to gradle.properties)") do |version|
      options[:version] = version
    end
    arguments.on("--repo-root PATH", "Repository root (defaults to the current directory)") do |root|
      options[:root] = root
    end
    arguments.on("--base-url URL", "Maven repository base URL") do |base_url|
      options[:base_url] = base_url
    end
    arguments.on("-h", "--help", "Show this help") do
      puts arguments
      exit
    end
  end

  begin
    parser.parse!
    result = MavenMetadataCheck.new(**options).validate!
    puts "Maven metadata valid: #{result.fetch("artifacts").length} artifacts at #{result.fetch("expected_version")}."
  rescue OptionParser::ParseError, MavenMetadataCheck::Violation => error
    warn "Maven metadata check failed: #{error.message}"
    exit 1
  end
end
