# How to work with this Wiki

### General Information
This Wiki is being stored into a GIT repository on branch master that is tracking remote branch master of origin at `/home/trac/git`. The physical location is `/home/gollum/wiki/gitwiki`. The changes here are pushed to origin on a more or less regular base (every week) by one of the admins.

The script `/home/gollum/wiki/serve.sh` can be executed to either start or restart the process being responsible for this wiki. The process is a Ruby webserver (Rack) that uses the handler defined in `/home/gollum/wiki/app.rb`. This handler embeds Gollum wiki software into Rack and also makes sure that only authenticated access is possible (see `/home/gollum/wiki/users.yml`).

### Making websites public
Accessing this wiki is normally only allowed for authenticated users. If you want to share your work with others not having an account here then you can just request making a given url publicly available. On the server this is done by simply putting a new line to `/home/gollum/wiki/public.txt`.

### Creating a New Account
A new account can be created by adding a new entry to `/home/gollum/wiki/users.yml`. The password there is encrypted using sha1sum.

### Editing
You can edit information here freely but do not leave any nonsense here.

The syntax of the markup is relatively easy and is documented here http://daringfireball.net/projects/markdown/dingus. When you create a new Page then make sure to put some thinking into the right tree structure and then just extend the URL above. It is also possible to use plain HTML for tables or image refs but use with care. If there is a markdown command then it is always to be preferred.

* Make sure to always have _wiki/_ as the base path so that contents is not cluttered around. So if you want to create `server/SystemBoot` then the correct URL is `wiki/server/SystemBoot`. Make sure to add your Page to the Table of Contents of [/home] by editing the document there. The contents of java/ is protected so that one can neither write nor read.

* Password can be found in the encrypted page [[Passwords|wiki/passwords]]. You can decrypt it by using `gpg -d < passwords.md` after having entered the password. If you want to edit, first decrypt and then encrypt again using `gpg -a --symmetric passwords.md`. Make sure to never save the unencrypted version! The encryption is a symmetric one (not key based) based on a password that is known to the admins.

If you have any questions then either contact Axel Uhl, Jens Rommel, Frank Mittag or Simon Pamiés.