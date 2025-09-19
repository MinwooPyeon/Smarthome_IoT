
// MainFrm.cpp: CMainFrame 클래스의 구현
//

#include "pch.h"


#include "framework.h"
#include "EeumMFC.h"
#include "EeumMFCDoc.h"

#include "MainFrm.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

// CMainFrame

IMPLEMENT_DYNCREATE(CMainFrame, CFrameWndEx)

BEGIN_MESSAGE_MAP(CMainFrame, CFrameWndEx)
	ON_WM_CREATE()
	ON_MESSAGE(WM_APP_DATAREADY, &CMainFrame::OnDataReady)
	ON_MESSAGE(WM_APP_SELECT_HUB, &CMainFrame::OnAppLog)
	ON_MESSAGE(WM_APP_LOG, &CMainFrame::OnAppLog)
END_MESSAGE_MAP()

static UINT indicators[] =
{
	ID_SEPARATOR,           // 상태 줄 표시기
	ID_INDICATOR_CAPS,
	ID_INDICATOR_NUM,
	ID_INDICATOR_SCRL,
};

// CMainFrame 생성/소멸

CMainFrame::CMainFrame() noexcept
{
	// TODO: 여기에 멤버 초기화 코드를 추가합니다.
}

CMainFrame::~CMainFrame()
{
}

int CMainFrame::OnCreate(LPCREATESTRUCT lpCreateStruct)
{
	if (CFrameWndEx::OnCreate(lpCreateStruct) == -1)
		return -1;

	if (!m_wndToolBar.CreateEx(this, TBSTYLE_FLAT, WS_CHILD | WS_VISIBLE | CBRS_TOP | CBRS_GRIPPER | CBRS_TOOLTIPS | CBRS_FLYBY | CBRS_SIZE_DYNAMIC) ||
		!m_wndToolBar.LoadToolBar(IDR_MAINFRAME))
	{
		TRACE0("도구 모음을 만들지 못했습니다.\n");
		return -1;      // 만들지 못했습니다.
	}

	if (!m_wndStatusBar.Create(this))
	{
		TRACE0("상태 표시줄을 만들지 못했습니다.\n");
		return -1;      // 만들지 못했습니다.
	}
	m_wndStatusBar.SetIndicators(indicators, sizeof(indicators)/sizeof(UINT));

	// TODO: 도구 모음을 도킹할 수 없게 하려면 이 세 줄을 삭제하십시오.
	m_wndToolBar.EnableDocking(CBRS_ALIGN_ANY);
	
	CDockingManager::SetDockingMode(DT_SMART);
	CMFCVisualManager::SetDefaultManager(RUNTIME_CLASS(CMFCVisualManagerWindows));
	EnableDocking(CBRS_ALIGN_ANY);
	EnableAutoHidePanes(CBRS_ALIGN_ANY);

	if (!CreateDockingPanes())
		return -1;

	return 0;
}

LRESULT CMainFrame::OnDataReady(WPARAM, LPARAM)
{
	CEeumMFCDoc* doc = dynamic_cast<CEeumMFCDoc*>(GetActiveDocument());

	if (doc) {
		doc->UpdateAllViews(nullptr);
	}

	return 0;
}	

LRESULT CMainFrame::OnSelectHub(WPARAM, LPARAM lp) {
	std::unique_ptr<CString> sp((CString*)lp);
	CString sel = *sp;

	CString norm = sel;
	if (norm.Left(4).CompareNoCase(L"hub/") != 0) norm = L"hub/" + norm;

	if (auto* doc = dynamic_cast<CEeumMFCDoc*>(GetActiveDocument()))
		doc->SetSelectedHub(norm);

	Log(L"INFO", L"Select Hub: " + norm);
	return 0;
}

LRESULT CMainFrame::OnAppLog(WPARAM, LPARAM lp)
{
	std::unique_ptr<CString> sp((CString*)lp);
	Log(L"LOG", *sp);
	return 0;
}

void CMainFrame::Log(const CString& level, const CString& msg)
{
	if (m_wndLogPane.GetSafeHwnd())
		m_wndLogPane.Append(level, msg);
}

BOOL CMainFrame::CreateDockingPanes()
{
	// 좌: Device/Topic Tree
	if (!m_wndDevicePane.Create(L"Device / Topic", this, CRect(0, 0, 250, 500),
		TRUE, 5001, WS_CHILD | WS_VISIBLE | CBRS_LEFT))
		return FALSE;
	m_wndDevicePane.EnableDocking(CBRS_ALIGN_LEFT);
	DockPane(&m_wndDevicePane, AFX_IDW_DOCKBAR_LEFT);

	// 하: Error/Event Log
	if (!m_wndLogPane.Create(L"Error / Event Log", this, CRect(0, 0, 500, 180),
		TRUE, 5002, WS_CHILD | WS_VISIBLE | CBRS_BOTTOM))
		return FALSE;
	m_wndLogPane.EnableDocking(CBRS_ALIGN_BOTTOM);
	DockPane(&m_wndLogPane, AFX_IDW_DOCKBAR_BOTTOM);

	// 우: Factor/Option
	if (!m_wndFactorPane.Create(L"Factors / Options", this, CRect(0, 0, 260, 500),
		TRUE, 5003, WS_CHILD | WS_VISIBLE | CBRS_RIGHT))
		return FALSE;
	m_wndFactorPane.EnableDocking(CBRS_ALIGN_RIGHT);
	DockPane(&m_wndFactorPane, AFX_IDW_DOCKBAR_RIGHT);

	// 자동 숨김 버튼 표시
	m_wndDevicePane.ShowPane(TRUE, FALSE, TRUE);
	m_wndLogPane.ShowPane(TRUE, FALSE, TRUE);
	m_wndFactorPane.ShowPane(TRUE, FALSE, TRUE);

	return TRUE;
}


BOOL CMainFrame::PreCreateWindow(CREATESTRUCT& cs)
{
	if( !CFrameWndEx::PreCreateWindow(cs) )
		return FALSE;
	// TODO: CREATESTRUCT cs를 수정하여 여기에서
	//  Window 클래스 또는 스타일을 수정합니다.

	return TRUE;
}

// CMainFrame 진단

#ifdef _DEBUG
void CMainFrame::AssertValid() const
{
	CFrameWndEx::AssertValid();
}

void CMainFrame::Dump(CDumpContext& dc) const
{
	CFrameWndEx::Dump(dc);
}
#endif //_DEBUG


// CMainFrame 메시지 처리기

