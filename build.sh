#!/bin/bash

# Raspberry Pi 4 IR Remote Control C++ Build Script

set -e

echo "Building IR Remote Control for Raspberry Pi 4..."

# Check if we're on Raspberry Pi 4
if [[ ! -f /proc/device-tree/model ]] || ! grep -q "Raspberry Pi 4" /proc/device-tree/model; then
    echo "Warning: This script is designed for Raspberry Pi 4"
    echo "Continuing anyway..."
fi

# Install dependencies
echo "Installing dependencies..."
sudo apt-get update
sudo apt-get install -y \
    build-essential \
    cmake \
    pkg-config \
    nlohmann-json3-dev \
    libjsoncpp-dev \
    libmosquitto-dev \
    libssl-dev \
    libboost-all-dev

# Install Crow (header-only library)
if [ ! -d "/usr/local/include/crow" ]; then
    echo "Installing Crow framework..."
    sudo git clone https://github.com/CrowCpp/Crow.git /tmp/crow
    sudo cp -r /tmp/crow/include/crow /usr/local/include/
    sudo rm -rf /tmp/crow
fi

# Create build directory
mkdir -p build
cd build

# Configure with CMake
echo "Configuring with CMake..."
cmake .. -DCMAKE_BUILD_TYPE=Release

# Build
echo "Building..."
make -j$(nproc)

# Install
echo "Installing..."
sudo make install

echo "Build completed successfully!"
echo ""
echo "To start the service:"
echo "  sudo systemctl enable irremote.service"
echo "  sudo systemctl start irremote.service"
echo ""
echo "To check status:"
echo "  sudo systemctl status irremote.service"
echo ""
echo "To view logs:"
echo "  sudo journalctl -u irremote.service -f"
