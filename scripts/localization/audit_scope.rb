#!/usr/bin/env ruby

require_relative "localization_scope_audit"

root = File.expand_path("../..", __dir__)
audit = LocalizationScopeAudit.new(root: root)
audit.validate!
puts JSON.pretty_generate(audit.inventory)
