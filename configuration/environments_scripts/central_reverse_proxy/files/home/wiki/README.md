# Internal Wiki

We use a Ruby based tool called Gollum for our wiki (one wiki to rule them all 🙄).
It provides a nice UI, indexing and versions all changes in Git.

We have added some authentication and access control as a sort of middleware.
This is done via the GitHub OAuth process and some APIs.

## The Process
Users navigate to `/login` (or are automatically redirected here if they are logged out and they try to access a path for modifying a resource) and are redirected to GitHub, starting the OAuth process. They have to authenticate with their chosen GitHub credentials and are then redirected back to the wiki.
If a user tries to read a resource or its history then they can access anything, but they must have push access to the Sailing Analytics repository to modify a resource (e.g. rename, create, modify).

## Installation
Run `bundle install` in the wiki home directory as the user who will start the wiki and make sure the Gemfile is present.

## Files
* The `templates` dir is necessary for adding the logout button. A custom dir can be specified by modifying the `config.ru`.
* The `app.rb` handles the auth and provides an allow list for user paths
* `config.ru` handles configuration and setup as you might guess.
* `Gemfile` all packages and versions.
* `server.sh` launches the wiki via Bundle.

## Running
First ensure the secrets specified at the start of app.rb (`ENV[<name>]`) are exported in a secrets file in the same directory. Then, as the user who owns the git repository that backs the Gollum wiki, run the `serve.sh` script.