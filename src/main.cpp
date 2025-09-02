#include <iostream>
#include <memory>
#include <thread>
#include <signal.h>
#include <fstream>
#include <nlohmann/json.hpp>
#include "remote.h"
#include "config.h"
#include "mqtt_client.h"
#include "web_server.h"

using namespace irremote;

// Global variables for signal handling
std::unique_ptr<WebServer> webServer;
std::unique_ptr<MqttClient> mqttClient;
bool running = true;

void signalHandler(int signum) {
    std::cout << "\nReceived signal " << signum << ". Shutting down..." << std::endl;
    running = false;
}

std::shared_ptr<Remote> loadRemoteFromFile(const std::string& filename) {
    std::ifstream file(filename);
    if (!file.is_open()) {
        std::cerr << "Failed to open remote file: " << filename << std::endl;
        return nullptr;
    }

    std::string content((std::istreambuf_iterator<char>(file)),
                        std::istreambuf_iterator<char>());
    file.close();

    try {
        // Parse JSON directly using nlohmann/json
        auto json_remote = nlohmann::json::parse(content);
        
        // Create Remote object
        auto remote = std::make_shared<Remote>(json_remote["name"]);
        for (const auto& code : json_remote["code"]) {
            remote->addCode(code["name"], code["code"]);
        }
        
        return remote;
    } catch (const std::exception& e) {
        std::cerr << "Failed to parse remote JSON: " << e.what() << std::endl;
        return nullptr;
    }

    return remote;
}

void printUsage(const char* programName) {
    std::cout << "Usage: " << programName << " [options] remote_proto [remote_proto ...]\n\n";
    std::cout << "Options:\n";
    std::cout << "  -c, --config <file>    Configuration file path\n";
    std::cout << "  -m, --mqtt <broker>    MQTT broker address (optional)\n";
    std::cout << "  -p, --port <port>      MQTT broker port (default: 1883)\n";
    std::cout << "  -h, --help             Show this help message\n\n";
    std::cout << "Positional arguments (required):\n";
    std::cout << "  remote_json            Path to JSON-encoded remote file\n\n";
    std::cout << "Example:\n";
    std::cout << "  " << programName << " -c config.json cambridge_cxa.json\n";
}

int main(int argc, char* argv[]) {
    // Set up signal handling
    signal(SIGINT, signalHandler);
    signal(SIGTERM, signalHandler);

    // Parse command line arguments
    std::string configFile;
    std::string mqttBroker;
    int mqttPort = 1883;
    std::vector<std::string> remoteFiles;

    for (int i = 1; i < argc; ++i) {
        std::string arg = argv[i];
        
        if (arg == "-c" || arg == "--config") {
            if (++i < argc) {
                configFile = argv[i];
            } else {
                std::cerr << "Error: --config requires a file path\n";
                return 1;
            }
        } else if (arg == "-m" || arg == "--mqtt") {
            if (++i < argc) {
                mqttBroker = argv[i];
            } else {
                std::cerr << "Error: --mqtt requires a broker address\n";
                return 1;
            }
        } else if (arg == "-p" || arg == "--port") {
            if (++i < argc) {
                mqttPort = std::stoi(argv[i]);
            } else {
                std::cerr << "Error: --port requires a port number\n";
                return 1;
            }
        } else if (arg == "-h" || arg == "--help") {
            printUsage(argv[0]);
            return 0;
        } else if (arg[0] == '-') {
            std::cerr << "Unknown option: " << arg << std::endl;
            printUsage(argv[0]);
            return 1;
        } else {
            remoteFiles.push_back(arg);
        }
    }

    if (remoteFiles.empty()) {
        std::cerr << "Error: At least one remote JSON file must be specified\n";
        printUsage(argv[0]);
        return 1;
    }

    // Load configuration
    std::shared_ptr<Config> config;
    if (!configFile.empty()) {
        config = Config::loadFromFile(configFile);
    } else {
        config = Config::loadDefault();
    }

    // Load remotes
    auto remoteManager = std::make_shared<RemoteManager>();
    for (const auto& remoteFile : remoteFiles) {
        auto remote = loadRemoteFromFile(remoteFile);
        if (remote) {
            remoteManager->addRemote(remote);
            std::cout << "Loaded remote: " << remote->getName() << std::endl;
        }
    }

    if (remoteManager->getAllRemotes().empty()) {
        std::cerr << "Error: No valid remotes loaded\n";
        return 1;
    }

    // Set up MQTT client if broker is specified
    if (!mqttBroker.empty()) {
        mqttClient = std::make_unique<MqttClient>();
        
        // Set up message handler
        mqttClient->setMessageCallback([&remoteManager](const std::string& topic, const irremote::Action& action) {
            std::cout << "Received MQTT command: " << action.remote_id() << " " << action.command() << std::endl;
            
            if (!action.remote_id().empty() && !action.command().empty()) {
                remoteManager->sendCommand(action.remote_id(), action.command());
            }
        });

        // Connect to MQTT broker
        if (!mqttClient->connect(mqttBroker, mqttPort, "", "", "irremote_client")) {
            std::cerr << "Failed to connect to MQTT broker\n";
            return 1;
        }

        // Subscribe to command topic (you can customize this)
        mqttClient->subscribe("irremote/commands");
        
        // Start MQTT loop in a separate thread
        std::thread mqttThread([&mqttClient]() {
            while (running) {
                mqttClient->loop(100);
                std::this_thread::sleep_for(std::chrono::milliseconds(100));
            }
        });
        mqttThread.detach();
    }

    // Create and start web server
    webServer = std::make_unique<WebServer>(remoteManager, config);
    
    std::cout << "Starting IR Remote Control Server for Raspberry Pi 4" << std::endl;
    std::cout << "Web UI will be available at: http://localhost:" << config->getWebUIPort() << std::endl;
    if (!mqttBroker.empty()) {
        std::cout << "MQTT connected to: " << mqttBroker << ":" << mqttPort << std::endl;
    }

    // Start web server in a separate thread
    std::thread webThread([&webServer, &config]() {
        webServer->run(config->getWebUIPort());
    });

    // Wait for shutdown signal
    while (running) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }

    std::cout << "Shutting down..." << std::endl;
    
    // Cleanup
    if (mqttClient) {
        mqttClient->disconnect();
    }

    return 0;
}
