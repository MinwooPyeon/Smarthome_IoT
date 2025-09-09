
// EeumMFCDoc.h: CEeumMFCDoc 클래스의 인터페이스
//


#pragma once
#include "pch.h"
#include "Ingestor.h"
#include "Types.h"


#ifndef WM_APP_DATAREADY
#define WM_APP_DATAREADY (WM_APP + 100)
#endif

class CEeumMFCDoc : public CDocument
{
protected: // serialization에서만 만들어집니다.
	CEeumMFCDoc() noexcept;
	DECLARE_DYNCREATE(CEeumMFCDoc)

// 특성입니다.
public:
	Ingestor m_ing;
	Metrics m_lastMetrics;
	std::vector<EnvSample> m_lastEnvBatch;
	std::vector<IrEvent> m_lastIrBatch;

// 작업입니다.
public:
	const Metrics& GetMetrics()   const { return m_lastMetrics; }
	const std::vector<EnvSample>& GetEnvBatch()  const { return m_lastEnvBatch; }
	const std::vector<IrEvent>& GetIrBatch()   const { return m_lastIrBatch; }
// 재정의입니다.
public:
	virtual BOOL OnNewDocument();
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
