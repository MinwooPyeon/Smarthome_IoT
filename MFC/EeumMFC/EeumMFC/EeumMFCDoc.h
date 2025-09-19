
// EeumMFCDoc.h: CEeumMFCDoc 클래스의 인터페이스
//


#pragma once
#include "pch.h"
#include "Ingestor.h"
#include "Types.h"
#include "MqttClient.h"

class CEeumMFCDoc : public CDocument
{
protected: // serialization에서만 만들어집니다.
	CEeumMFCDoc() noexcept;
	DECLARE_DYNCREATE(CEeumMFCDoc)

// 특성입니다.
public:
	std::unique_ptr<MqttClient> mqtt_;
	Ingestor ingestor_;
	
	std::mutex mtx_;
	Metrics latestMet_;
	std::vector<EnvSample> latestEnv_;
	std::vector<IrEvent> latestIr_;
private:
	std::string selectedHub_;
	std::string lastOrderedHub_;
// 작업입니다.
public:
	void SetSelectedHub(const CString& hub);
	const Metrics& GetMetrics()   const { return latestMet_; }
	const std::vector<EnvSample>& GetEnvBatch()  const { return latestEnv_; }
	const std::vector<IrEvent>& GetIrBatch()   const { return latestIr_; }
// 재정의입니다.
public:
	virtual BOOL OnNewDocument() override;
	virtual void OnCloseDocument() override;
	virtual void Serialize(CArchive& ar);
#ifdef SHARED_HANDLERS
	virtual void InitializeSearchContent();
	virtual void OnDrawThumbnail(CDC& dc, LPRECT lprcBounds);
#endif // SHARED_HANDLERS

// 구현입니다.
public:
	virtual ~CEeumMFCDoc();
#ifdef _DEBUG
	virtual void AssertValid() const;
	virtual void Dump(CDumpContext& dc) const;
#endif

protected:

// 생성된 메시지 맵 함수
protected:
	DECLARE_MESSAGE_MAP()

#ifdef SHARED_HANDLERS
	// 검색 처리기에 대한 검색 콘텐츠를 설정하는 도우미 함수
	void SetSearchContent(const CString& value);
#endif // SHARED_HANDLERS
};
