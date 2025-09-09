
// EeumMFCView.h: CEeumMFCView 클래스의 인터페이스
//

#pragma once
#include <wrl.h>
#include <webview2.h>
#include "Types.h"
#include <afxwin.h>

class CEeumMFCView : public CView
{
protected: // serialization에서만 만들어집니다.
	CEeumMFCView() noexcept;
	DECLARE_DYNCREATE(CEeumMFCView)

// 특성입니다.
public:
	CEeumMFCDoc* GetDocument() const;

// 작업입니다.
public:

// 재정의입니다.
public:
	virtual void OnDraw(CDC* pDC);  // 이 뷰를 그리기 위해 재정의되었습니다.
	virtual BOOL PreCreateWindow(CREATESTRUCT& cs);
protected:
	virtual BOOL OnPreparePrinting(CPrintInfo* pInfo);
	virtual void OnBeginPrinting(CDC* pDC, CPrintInfo* pInfo);
	virtual void OnEndPrinting(CDC* pDC, CPrintInfo* pInfo);

// 구현입니다.
public:
	virtual ~CEeumMFCView();
#ifdef _DEBUG
	virtual void AssertValid() const;
	virtual void Dump(CDumpContext& dc) const;
#endif

protected:

// 생성된 메시지 맵 함수
protected:
	afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct);
	afx_msg void OnSize(UINT nType, int cx, int cy);
	afx_msg void OnTimer(UINT_PTR nIDEvent);
	afx_msg BOOL OnEraseBkgnd(CDC* pDC);
	DECLARE_MESSAGE_MAP()

private:
	HWND m_hWebHost = nullptr;
	Microsoft::WRL::ComPtr<ICoreWebView2Environment> m_env;
	Microsoft::WRL::ComPtr<ICoreWebView2Controller> m_controller;
	Microsoft::WRL::ComPtr<ICoreWebView2> m_webview;

private:
	void CreateWebHostWindow();
	void InitWebView();
	void ResizeWebView();
	void LoadChartHtml();

public:
	void PushMetrics(const Metrics& m);
};

#ifndef _DEBUG  // EeumMFCView.cpp의 디버그 버전
inline CEeumMFCDoc* CEeumMFCView::GetDocument() const
   { return reinterpret_cast<CEeumMFCDoc*>(m_pDocument); }
#endif

