require "gollum/app"
require "digest/sha1"
require "logger"
require "rest-client"
require "base64"

class App < Precious::App
  use Rack::Session::Cookie,
    secret: ENV["SESSION_SECRET"],
    expire_after: 3600,
    httponly: true,
    same_site: :lax

#   use Rack::Session::Redis,
#       :redis_server => "redis://127.0.0.1:6379/0",
#       :expires_in => 3600

  LOGGER = Logger.new("/home/wiki/wiki_log.txt")
  CLIENT_ID = ENV["CLIENT_ID"]
  CLIENT_SECRET = ENV["CLIENT_SECRET"]
  REDIRECT_URL = "https://wiki.sapsailing.com/callback"
  ACCESS_TOKEN = ENV["ACCESS_TOKEN"]

  REPO_OWNER = "SAP"
  REPO_NAME = "sailing-analytics"
  LOGIN_LINK = "<a href='/login'>Login (will redirect to GitHub)</a>".freeze
  before { check! }
  before "/gollum/(edit|create|rename|delete|history)/*" do
    authorize_write
    session["gollum.author"] = {
      :name => session[:name],
      :email => session[:email],
    }
  end
  before "/gollum/(overview|preview|latest_changes)" do
    if !session[:logged_in]
      halt 401, LOGIN_LINK
    end
  end

  PUBLIC_STARTS = ["/Home", "/wiki", "/favicon.ico","/login","/callback","/logout"].freeze
  NON_PAGE_PATTERNS = [%r{\A/gollum/(assets|commit)}, %r{\A/gollum/(last_commit_info|search)\z}].freeze
  AUTH_PATHS = [%r{\A/gollum/(edit|create|rename|history|delete|latest_changes)}, %r{\A/gollum/(overview|preview)\z}].freeze # Require logged in.
  LOGIN_PATHS = %r{\A/(login|callback|logout)}.freeze

  START_TIME=Time.now.freeze
  get '/robots.txt' do
    content_type 'text/plain'
    "User-agent: *\nDisallow: /"
  end

  get "/logout" do
    session.clear()
    "Logged out"
  end

  get "/login" do
    session[:oauth_state] = {
      :state => SecureRandom.hex(16),
      :expiry => Time.now + (60 * 5),
    }
    params = {
      client_id: CLIENT_ID,
      scope: "user:email",
      state: session[:oauth_state][:state],
      redirect_uri: REDIRECT_URL,
    }
    uri = URI::HTTPS.build(
      host: "github.com",
      path: "/login/oauth/authorize",
      query: URI.encode_www_form(params),
    )
    redirect uri.to_s()
  end

  get "/callback" do
    if session[:logged_in]
      # Ensures cancel works in editor after login.
      if session[:prev]
        prev = session[:prev].dup
        stripped_prev = prev.sub("/gollum/edit", "")
        redirect stripped_prev
      end
    end
    halt 400, "error" if params[:error]
    halt 400, "Missing code" unless params[:code]
    halt 403, "Old state" unless session[:oauth_state] && session[:oauth_state][:expiry] > Time.now
    halt 403, "Invalid OAuth state" unless session[:oauth_state] && session[:oauth_state][:state] == params[:state]

    session.delete(:oauth_state)

    result = JSON.parse(RestClient.post("https://github.com/login/oauth/access_token",
                                        {
      :client_id => CLIENT_ID,
      :client_secret => CLIENT_SECRET,
      :code => params[:code],
      :redirect_uri => REDIRECT_URL,
    },
                                        :accept => :json))
    scopes = result["scope"].split(",")
    if scopes.include?("user:email") && result["access_token"]
      access_token = result["access_token"]
      fetch_and_set_user_email(access_token)
      fetch_and_set_user_name_and_id(access_token)
      session[:logged_in] = true
      revoke_access_token(access_token)
      session.delete(:access_token)
    end

    if session[:prev]
      target = session.delete(:prev)
      redirect target.sub(/\/gollum\/(rename|delete)/, "")
    end

    redirect "/"
  end

  helpers do
    def public_path?(path)
      if path == "/"
        return true
      end
      PUBLIC_STARTS.any? { |link| path.start_with?(link) }
    end

    def asset_path?(path)
      NON_PAGE_PATTERNS.any? {|link| link.match?(path)}
    end

    def auth_path?(path)
      AUTH_PATHS.any?{ |link| link.match?(path) }
    end

    def login_path?(path)
      LOGIN_PATHS.match?(path)
    end

    def check!
      path = env["PATH_INFO"]
      return if login_path?(path)
      return if path == "/robots.txt"
      return if public_path?(path)
      return if asset_path?(path)
      return if auth_path?(path)
      halt 403, "You cannot access anything outside wiki/ path."
    end

    def authorize_write
      LOGGER.debug("Checking auth before writing")

      if !session[:logged_in]
        if env["PATH_INFO"].start_with?("/gollum/delete")
          halt 401, "Unauthorized"
        end
        session[:prev] = env["PATH_INFO"]
        halt 401, LOGIN_LINK
      end
      halt 403, "Forbidden" unless user_can_write()
    end

    def user_can_write()
      return false unless session[:logged_in] && session[:name]
      response = github_api_get("/repos/#{REPO_OWNER}/#{REPO_NAME}/collaborators/#{session[:name]}/permission",
                                ACCESS_TOKEN)
      return false unless response
      LOGGER.debug("response received")
      result = JSON.parse(response)
      LOGGER.debug("checking permission")
      result.dig("user", "permissions", "push") == true
    end

    def fetch_and_set_user_email(access_token)
      response = github_api_get("/user/emails", access_token)
      return unless response
      emails = JSON.parse(response)
      if emails[0] && emails[0]["email"]
        session[:email] = emails[0]["email"]
        LOGGER.debug("email is #{session[:email]}")
      end
    end

    def fetch_and_set_user_name_and_id(access_token)
      response = github_api_get("/user", access_token)
      return unless response
      user_details = JSON.parse(response)
      if user_details["login"] && user_details["id"]
        session[:user_id] = user_details["id"]
        session[:name] = user_details["login"]
        LOGGER.debug("user #{session[:email]} logged in")
      end
    end

    def github_api_get(path, access_token)
      uri = URI::HTTPS.build(
        host: "api.github.com",
        path: path,
      )
      RestClient.get(uri.to_s(),
                     {
        :Authorization => "Bearer #{access_token}",
      })
    rescue RestClient::Unauthorized, RestClient::Forbidden
      LOGGER.warn("GitHub auth failed for #{path}")
      nil
    rescue RestClient::ExceptionWithResponse => e
      LOGGER.error("GitHub API error #{e.response.code} for #{path}")
      nil
    rescue StandardError => e
      LOGGER.error("GitHub request failed: #{e.message}")
      nil
    end

    def revoke_access_token(token)
      uri = URI::HTTPS.build(
        host: "api.github.com",
        path: "/applications/#{CLIENT_ID}/token",
      )
      basicAuth = Base64.strict_encode64("#{CLIENT_ID}:#{CLIENT_SECRET}")
      RestClient::Request.execute(
        method: :delete,
        url: uri.to_s(),
        payload: {
          :access_token => token,
        }.to_json(),
        headers: {
          :Authorization => "Basic #{basicAuth}",
          :accept => "application/vnd.github.v3+json",
        },
        timeout: 5,
        open_timeout: 3,
      )
    end
  end
end
