#ifndef IR_DATABASE_H
#define IR_DATABASE_H

#include <string>
#include <vector>
#include <map>
#include <memory>

struct IRDatabaseEntry {
    std::string brand;
    std::string model;
    std::string device_type;
    std::string command;
    std::string ir_code;
    std::string protocol;
    int frequency;
    int bits;
    std::string description;
    double confidence;
};

class IRDatabase {
public:
    IRDatabase();
    ~IRDatabase();

    bool initialize();
    bool loadFromFile(const std::string& filename);
    bool saveToFile(const std::string& filename) const;

    std::vector<IRDatabaseEntry> searchByBrand(const std::string& brand) const;
    std::vector<IRDatabaseEntry> searchByModel(const std::string& brand, const std::string& model) const;
    std::vector<IRDatabaseEntry> searchByCommand(const std::string& command) const;
    std::vector<IRDatabaseEntry> searchByIRCode(const std::string& ir_code) const;

    std::vector<IRDatabaseEntry> autoMatch(const std::string& ir_code,
                                          const std::string& device_type = "") const;

    bool addEntry(const IRDatabaseEntry& entry);
    bool updateEntry(const std::string& id, const IRDatabaseEntry& entry);
    bool deleteEntry(const std::string& id);

    int getTotalEntries() const;
    std::vector<std::string> getSupportedBrands() const;
    std::vector<std::string> getSupportedDevices() const;

    bool syncWithOnlineDatabase();
    bool uploadLearnedCodes(const std::vector<IRDatabaseEntry>& codes);

private:
    std::map<std::string, IRDatabaseEntry> entries_;
    std::map<std::string, std::vector<std::string>> brand_index_;
    std::map<std::string, std::vector<std::string>> command_index_;
    std::map<std::string, std::vector<std::string>> code_index_;

    mutable std::mutex database_mutex_;

    void rebuildIndexes();
    void addToIndexes(const std::string& id, const IRDatabaseEntry& entry);
    void removeFromIndexes(const std::string& id);

    double calculateSimilarity(const std::string& code1, const std::string& code2) const;
    std::string normalizeIRCode(const std::string& code) const;

    std::string generateEntryId(const IRDatabaseEntry& entry) const;
    bool validateEntry(const IRDatabaseEntry& entry) const;
};

#endif
