#ifndef CONFIG_H
#define CONFIG_H

#include <string>
#include <map>
#include <memory>

class Config {
public:
    Config();
    ~Config();
    
    // 문자열 설정
    void setString(const std::string& key, const std::string& value);
    std::string getString(const std::string& key, const std::string& defaultValue = "") const;
    
    // 정수 설정
    void setInt(const std::string& key, int value);
    int getInt(const std::string& key, int defaultValue = 0) const;
    
    // 부동소수점 설정
    void setFloat(const std::string& key, float value);
    float getFloat(const std::string& key, float defaultValue = 0.0f) const;
    
    // 불린 설정
    void setBool(const std::string& key, bool value);
    bool getBool(const std::string& key, bool defaultValue = false) const;
    
    // 설정 저장/로드
    bool saveToFile(const std::string& filename) const;
    bool loadFromFile(const std::string& filename);
    
    // 설정 존재 여부 확인
    bool hasKey(const std::string& key) const;
    
    // 모든 키 가져오기
    std::vector<std::string> getAllKeys() const;
    
    // 설정 초기화
    void clear();

private:
    std::map<std::string, std::string> config_map_;
    
    // 내부 헬퍼 함수들
    std::string serializeValue(const std::string& value) const;
    std::string deserializeValue(const std::string& serialized) const;
};

#endif // CONFIG_H
