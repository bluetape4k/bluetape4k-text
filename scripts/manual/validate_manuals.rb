#!/usr/bin/env ruby

require_relative "manual_contract"

root = File.expand_path("../..", __dir__)
manifest = File.join(root, "docs/manual/manifest.yaml")

ManualContract.new(root: root, manifest: manifest).validate!
puts "Text manual contract validated"
