#!/usr/bin/env ruby

require "fileutils"
require "json"
require "yaml"

class ManifestExporter
  def initialize(source:, output:)
    @source = source
    @output = output
  end

  def write
    FileUtils.mkdir_p(File.dirname(@output))
    File.binwrite(@output, rendered)
  end

  def current?
    File.file?(@output) && File.binread(@output) == rendered.b
  end

  private

  def rendered
    manifest = YAML.safe_load(File.read(@source))
    raise ArgumentError, "manual manifest must be a mapping" unless manifest.is_a?(Hash)

    normalized = manifest.dup
    if normalized["modules"].is_a?(Array)
      normalized["modules"] = normalized["modules"].sort_by { |entry| entry.fetch("id") }
    end
    if normalized["evidence"].is_a?(Array)
      normalized["evidence"] = normalized["evidence"].sort_by { |entry| entry.fetch("id") }
    end
    JSON.pretty_generate(sort_keys(normalized)) + "\n"
  end

  def sort_keys(value)
    case value
    when Hash
      value.keys.sort.each_with_object({}) { |key, result| result[key] = sort_keys(value[key]) }
    when Array
      value.map { |entry| sort_keys(entry) }
    else
      value
    end
  end
end

if $PROGRAM_NAME == __FILE__
  check = ARGV.delete("--check")
  exporter = ManifestExporter.new(
    source: ARGV.fetch(0, "docs/manual/manifest.yaml"),
    output: ARGV.fetch(1, "docs/manual/generated/manifest.json"),
  )
  if check
    abort("Manual manifest snapshot is out of date.") unless exporter.current?
    puts "Manual manifest snapshot is current."
  else
    exporter.write
    puts "Manual manifest snapshot written."
  end
end
