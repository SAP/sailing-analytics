source "https://rubygems.org"

platform :ruby do
  gem 'highline', '> 0'
  gem 'json', '> 0'
  gem 'fastlane', '>= 2.222.0'   # This version and later support jwt 3.x
  gem 'googleauth', '>= 1.11.0'  # Explicitly require version that supports jwt 3.x

  plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
  eval_gemfile(plugins_path) if File.exist?(plugins_path)
end
