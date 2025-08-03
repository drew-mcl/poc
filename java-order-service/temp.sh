
export JAVA_HOME="/opt/homebrew/opt/openjdk@17"        # or $(/usr/libexec/java_home -v17)
export PATH="$JAVA_HOME/bin:$PATH"

# 2️⃣  (Optional but recommended) Make the JDK discoverable to GUI apps & /usr/libexec/java_home
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk \
             /Library/Java/JavaVirtualMachines/openjdk-17.jdk