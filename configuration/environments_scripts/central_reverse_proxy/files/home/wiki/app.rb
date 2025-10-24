require 'gollum/app'
require 'digest/sha1'


#__DIR__ = File.expand_path(File.dirname(__FILE__))
#$: << __DIR__
class App < Precious::App
  User = Struct.new(:name, :email, :password_hash, :can_write)
  before { authenticate! }
  before /edit/ do   authorize_write! ; end
    before do
        session['gollum.author'] = {
            :name => "%s" % settings.loggedInUser,
            :email => "%s" % settings.loggedInUserEmail,
        }
    end

  helpers do
    def authenticate!
      public_urls=IO.readlines 'public.txt'
      public_urls.each {|url|
        if self.env['PATH_INFO'] == url.slice(0, url.length-1)
          puts "Allowing " + url
          return
        end

        if self.env['PATH_INFO'].start_with?('/wiki/images') ||
            self.env['PATH_INFO'].start_with?('/favicon.ico')
          return
        end
      }
      if self.env['PATH_INFO'].split('/')[1] == 'gollum' && self.env['PATH_INFO'].split('/')[2] == 'assets'
        return
      end
      @_auth =  Rack::Auth::Basic::Request.new(request.env)
      if self.env['PATH_INFO'].split('/').length >=2 &&
        (self.env['PATH_INFO'].split('/')[1] != 'wiki' &&
        self.env['PATH_INFO'].split('/')[2] != 'wiki' &&
        self.env['PATH_INFO'].split('/')[1] != 'home' &&
        self.env['PATH_INFO'].split('/')[2] != 'home' &&
        self.env['PATH_INFO'].split('/')[1] != 'Home' &&
        self.env['PATH_INFO'].split('/')[2] != 'Home' &&
        self.env['PATH_INFO'].split('/')[1] != 'Home.md' &&
        self.env['PATH_INFO'].split('/')[1] != 'search' &&
        self.env['PATH_INFO'].split('/')[2] != 'search' &&
        self.env['PATH_INFO'].split('/')[1] != 'edit' &&
        self.env['PATH_INFO'].split('/')[2] != 'edit' &&
        self.env['PATH_INFO'].split('/')[1] != 'preview' &&
        self.env['PATH_INFO'].split('/')[2] != 'preview' &&
        (self.env['PATH_INFO'].split('/')[1] != 'gollum' ||
          (self.env['PATH_INFO'].split('/')[2] != 'create' &&
             (self.env['PATH_INFO'].split('/')[2] != 'overview' || self.env['PATH_INFO'].split('/')[3] != 'wiki'))))
        throw(:halt, [403, 'Forbidden - You can not access anything outside wiki/ path.'])
      end
      if @_auth.provided?
      end
      if @_auth.provided? && @_auth.basic? && @_auth.credentials && @user = detected_user(@_auth.credentials)
        Precious::App.set(:loggedInUser, @user.name)
        Precious::App.set(:loggedInUserEmail, @user.email)
        return @user
      else
        response['WWW-Authenticate'] = %(Basic realm="Gollum Wiki")
        throw(:halt, [401, "Not authorized\n"])
      end
    end

    def authorize_write!
      throw(:halt, [403, "Forbidden\n"]) unless @user.can_write
    end

    def users
      @_users ||= settings.authorized_users.map {|u| User.new(*u) }
    end

    def detected_user(credentials)
      users.detect do |u|
        [u.email, u.password_hash] ==
        [credentials[0], Digest::SHA1.hexdigest(credentials[1])]
      end
    end
  end

  def commit_
    {
      :message => params[:message],
  #    :name => @user.name,
      :email => @user.email
    }
  end
end
##set author
#class Precious::App
#    before do
#        session['gollum.author'] = {
#            :name => "%s" % settings.loggedInUser,
#            :email => "%s@example.com" % settings.loggedInUser,
#        }
#    end
#End
