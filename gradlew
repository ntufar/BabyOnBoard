#!/bin/bash
# This is a simplified gradle wrapper for CI purposes.
# In a real production environment, use 'gradle wrapper' to generate the full wrapper.

set -e

# Determine Java home
if [ -x "$(command -v java)" ]; then
    export JAVA_HOME=$(java -Xmx512m -version 2>&1 | awk -F '"' '/\"java\"/ {print $2}')
else
    echo "Java not found. Please ensure Java is installed."
    exit 1
fi

# Path to the gradle binary. 
# We check for system-installed gradle first, then fall back to a dummy if needed.
if command -v gradle >/dev/null 2>&1; then
    GRADLE_BIN="gradle"
else
    echo "Warning: Gradle not found in PATH. This wrapper is designed to proxy to a system-installed gradle."
    # If we really had the jar, we would point to it here.
    # For now, we let the CI fail gracefully or use a dummy path.
    exit 1
fi

# Execute gradle with passed arguments
exec "$GRADLE_BIN" "$@"
