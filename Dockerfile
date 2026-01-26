# Claude Code Development Container
FROM node:20-bookworm

# Install common development tools and Java 17 for Kotlin/Gradle
RUN apt-get update && apt-get install -y \
    git \
    curl \
    vim \
    python3 \
    python3-pip \
    sudo \
    openjdk-17-jdk \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Set Java environment variables
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Install Claude Code CLI and claude-flow v3 globally
RUN npm install -g @anthropic-ai/claude-code claude-flow@v3alpha

# Create non-root user (required for --dangerously-skip-permissions)
ARG USERNAME=claude
ARG USER_UID=1000
ARG USER_GID=$USER_UID

RUN groupadd --gid $USER_GID $USERNAME \
    && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME \
    && echo $USERNAME ALL=\(root\) NOPASSWD:ALL > /etc/sudoers.d/$USERNAME \
    && chmod 0440 /etc/sudoers.d/$USERNAME

# Set working directory
WORKDIR /workspace

# Switch to non-root user
USER $USERNAME

# Default command - interactive shell
CMD ["bash"]
