#!/usr/bin/env ruby
#require 'rubygems'
#require 'gollum/app'
## Define list of authorized users.
## Each user must have a username, password, name and email.
##
## Instead of a password you can also define a password_digest, which is the
## SHA-256 hash of a password.
##
## Example:
#users = YAML.load %q{
#---
#- username: rick
#  password: asdf754&1129-@lUZw
#  name: Rick Sanchez
#  email: rick@example.com
#- username: morty
#  password_digest: 5994471abb01112afcc18159f6cc74b4f511b99806da59b3caf5a9c173cacfc5
#  name: Morty Smith
#  email: morty@example.com
#}
#
## Allow unauthenticated users to read the wiki (disabled by default).
#options = { allow_unauthenticated_readonly: true }
#
## Allow only authenticated users to change the wiki.
## (NOTE: This must be loaded *before* Precious::App!)
#use Gollum::Auth, users, options
#
## That's it. The rest is for gollum only.
#gollum_path = "/home/wiki/gitwiki"
#wiki_options = {:universal_toc => false}
#Precious::App.set(:gollum_path, gollum_path)
#Precious::App.set(:wiki_options, wiki_options)
#run Precious::App

require 'rubygems'
require 'gollum/app'
require_relative 'app'
#require 'ruby-prof'
#require 'gollum/auth' # Don't forget to load the gem!
Gollum::Page.send :remove_const, :FORMAT_NAMES if defined? Gollum::Page::FORMAT_NAMES
#Gollum::Markup.formats.clear
#Gollum::Markup.formats[:markdown] = {
# :name => "MarkDown",
# :extensions => "md",
# :regexp => /md|mkdn?|mdown|markdown/
#}

gollum_path = "/home/wiki/gitwiki"
wiki_options = {universal_toc: false, ref: 'main'}
Precious::App.set(:gollum_path, gollum_path)
Precious::App.set(:wiki_options, wiki_options)
Precious::App.set(:authorized_users, YAML.load_file(File.expand_path('users.yml', File.expand_path(File.dirname(__FILE__)))))
Precious::App.set(:loggedInUser, "anonymous");
Precious::App.set(:loggedInUserEmail, "wiki@sapsailing.com");
App.set(:default_markup, :markdown) # set your favorite markup language
run App

#require 'rubygems'
#require 'gollum/app'
#
#gollum_path = "/home/wiki/gitwiki"
#wiki_options = {:universal_toc => false}
#Precious::App.set(:gollum_path, gollum_path)
#Precious::App.set(:default_markup, :markdown) # set your favorite markup language
#Precious::App.set(:wiki_options, wiki_options)
#run Precious::App
