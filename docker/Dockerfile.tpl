FROM openjdk:11
LABEL maintainer=axel.uhl@sap.com
# Download and extract the release
WORKDIR /home/sailing/servers/server
RUN apt-get update \
 && apt-get install -y wget \
 && wget -O /tmp/RELEASE.tar.gz http://releases.sapsailing.com/RELEASE/RELEASE.tar.gz \
 && tar xzvpf /tmp/RELEASE.tar.gz \
 && rm /tmp/RELEASE.tar.gz
RUN apt-get install -y vim
COPY vimrc /root/.vimrc
RUN apt-get install -y telnet dnsutils net-tools
COPY env.sh .
COPY start .
COPY JavaSE-11.profile .
EXPOSE 8888 14888 8000
CMD [ "/home/sailing/servers/server/start", "fg" ]
