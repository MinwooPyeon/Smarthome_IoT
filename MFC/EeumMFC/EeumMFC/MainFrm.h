
// MainFrm.h: CMainFrame 클래스의 인터페이스
//

#pragma once
#include "DevicePane.h"
#include "LogPane.h"
#include "FactorPane.h"

class CMainFrame : public CFrameWndEx
{
	
protected: // serialization에서만 만들어집니다.
	CMainFrame() noexcept;
	DECLARE_DYNCREATE(CMainFrame)

// 특성입니다.
public:

// 작업입니다.
public:
	void Log(const CString& level, const CString& msg);
	BOOL CreateDockingPanes();

// 재정의입니다.
public:
	virtual BOOL PreCreateWindow(CREATESTRUCT& cs);
	
// 구현입니다.
public:
	virtual ~CMainFrame();
#ifdef _DEBUG
	virtual void AssertValid() const;
	virtual void Dump(CDumpContext& dc) const;
#endif

protected:  // 컨트롤 모음이 포함된 멤버입니다.
	CToolBar        m_wndToolBar;
	CStatusBar      m_wndStatusBar;
	CDevicePane		m_wndDevicePane;
	CLogPane		m_wndLogPane;
	CFactorPane		m_wndFactorPane;
// 생성된 메시지 맵 함수
protected:
	afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct);
	afx_msg LRESULT OnDataReady(WPARAM, LPARAM);
	afx_msg LRESULT OnSelectHub(WPARAM, LPARAM);
	afx_msg LRESULT OnAppLog(WPARAM, LPARAM);
	DECLARE_MESSAGE_MAP()

};