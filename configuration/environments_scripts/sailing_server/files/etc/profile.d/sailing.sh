# Script to be linked from /etc/profile.d
# Appends to PATH, sets DISPLAY for VNC running on :2, exports JAVA_HOME and Amazon EC2 variables

if [[ "$USER" == "root" ]]; then
    ulimit -n 100000
fi
ulimit -u 40000

# SAP JVM
export JAVA_HOME=/opt/sapjvm_8
# JDK 11.0.1:
#export JAVA_HOME=/opt/jdk-11.0.1+13
#export JAVA_HOME=/opt/jdk1.8.0_45
export ANDROID_HOME=/opt/android-sdk-linux
export PATH=$PATH:$JAVA_HOME/bin
export DISPLAY=:2.0
