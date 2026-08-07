#!/usr/bin/env ruby

require_relative "manual_contract"
require_relative "release_contract"
require_relative "release_inventory"

root = File.expand_path("../..", __dir__)
manifest = File.join(root, "docs/manual/manifest.yaml")
tag = ARGV.fetch(0, ReleaseInventory::DEFAULT_REF)
sha = ARGV.fetch(1, ReleaseInventory::DEFAULT_SHA)

ReleaseContract.new(root: root, tag: tag, expected_sha: sha).validate!
inventory = ReleaseInventory.new(root: root, ref: tag, expected_sha: sha).validate!
ManualContract.new(root: root, manifest: manifest, expected_release: { "ref" => tag, "commit" => sha }).validate!

puts "Strict release manual contract valid: annotated tag #{tag} -> #{sha}; " \
     "#{inventory.fetch('modules').length} modules, #{inventory.fetch('examples').length} examples, " \
     "#{inventory.fetch('evidence').length} evidence files."
