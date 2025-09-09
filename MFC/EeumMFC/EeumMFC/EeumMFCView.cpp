
// EeumMFCView.cpp: CEeumMFCView 클래스의 구현
//

#include "pch.h"
#include "framework.h"
// SHARED_HANDLERS는 미리 보기, 축소판 그림 및 검색 필터 처리기를 구현하는 ATL 프로젝트에서 정의할 수 있으며
// 해당 프로젝트와 문서 코드를 공유하도록 해 줍니다.
#ifndef SHARED_HANDLERS
#include "EeumMFC.h"
#endif

#include "EeumMFCDoc.h"
#include "EeumMFCView.h"

#include <wrl.h>
#include <WebView2.h>
#include <filesystem>
#include <Shlwapi.h>

using Microsoft::WRL::ComPtr;
using Microsoft::WRL::Callback;


#ifdef _DEBUG
#define new DEBUG_NEW
#endif

// CEeumMFCView

IMPLEMENT_DYNCREATE(CEeumMFCView, CView)

BEGIN_MESSAGE_MAP(CEeumMFCView, CView)
	// 표준 인쇄 명령입니다.
	ON_COMMAND(ID_FILE_PRINT, &CView::OnFilePrint)
	ON_COMMAND(ID_FILE_PRINT_DIRECT, &CView::OnFilePrint)
	ON_COMMAND(ID_FILE_PRINT_PREVIEW, &CView::OnFilePrintPreview)


	ON_WM_CREATE()
	ON_WM_SHOWWINDOW()
	ON_WM_SIZE()
	ON_WM_TIMER()
	ON_WM_ERASEBKGND()
END_MESSAGE_MAP()


static CString PathToFileUri(const std::wstring& path) {
	wchar_t uri[2048]{}; DWORD len = _countof(uri);
	if (SUCCEEDED(UrlCreateFromPathW(path.c_str(), uri, &len, 0))) {
		CString s(uri); s.Replace(L"\\", L"/");
		return s; // e.g. file:///C:/...
	}
	// fallback: 대충 만든다 (권장X)
	CString s; s.Format(L"file:///%s", path.c_str()); s.Replace(L"\\", L"/");
	return s;
}
// CEeumMFCView 생성/소멸
CEeumMFCView::CEeumMFCView() noexcept
{
	// TODO: 여기에 생성 코드를 추가합니다.

}

CEeumMFCView::~CEeumMFCView()
{
}

BOOL CEeumMFCView::PreCreateWindow(CREATESTRUCT& cs)
{
	// TODO: CREATESTRUCT cs를 수정하여 여기에서
	//  Window 클래스 또는 스타일을 수정합니다.

	return CView::PreCreateWindow(cs);
}

// ====== WebView2: 호스트 자식 창 생성 ======
void CEeumMFCView::CreateWebHostWindow()
{
	if (m_hWebHost) return;

	CRect rc; GetClientRect(&rc);
	m_hWebHost = ::CreateWindowEx(
		0, L"STATIC", L"",
		WS_CHILD | WS_VISIBLE,
		rc.left, rc.top, rc.Width(), rc.Height(),
		this->GetSafeHwnd(), nullptr, AfxGetInstanceHandle(), nullptr);
}

// ====== WebView2 초기화 ======
void CEeumMFCView::InitWebView() {
	if (m_webview) return;

	CreateCoreWebView2EnvironmentWithOptions(
		nullptr, nullptr, nullptr,
		Callback<ICoreWebView2CreateCoreWebView2EnvironmentCompletedHandler>(
			[this](HRESULT result, ICoreWebView2Environment* env)->HRESULT {
				if (FAILED(result) || !env) { AfxMessageBox(L"WebView2 env 실패"); return result; }
				m_env = env;

				env->CreateCoreWebView2Controller(
					m_hWebHost,
					Callback<ICoreWebView2CreateCoreWebView2ControllerCompletedHandler>(
						[this](HRESULT result2, ICoreWebView2Controller* controller)->HRESULT {
							if (FAILED(result2) || !controller) { AfxMessageBox(L"WebView2 controller 실패"); return result2; }
							m_controller = controller;
							m_controller->get_CoreWebView2(&m_webview);
							m_controller->put_IsVisible(TRUE);
							ResizeWebView(); // ← 사이즈 먼저

//							m_webview->NavigateToString(L"<!doctype html><html><body><h1 style='font-family:sans-serif'>hello</h1></body></html>");
							// 네비 완료 로깅
							m_webview->add_NavigationCompleted(
								Callback<ICoreWebView2NavigationCompletedEventHandler>(
									[this](ICoreWebView2*, ICoreWebView2NavigationCompletedEventArgs* args)->HRESULT {
										BOOL ok = FALSE; args->get_IsSuccess(&ok);
										COREWEBVIEW2_WEB_ERROR_STATUS st; args->get_WebErrorStatus(&st);
										if (!ok) {
											CString msg; msg.Format(L"Navigation 실패 (status=%d)", (int)st);
											AfxMessageBox(msg);
											// 개발 편의: DevTools 열기
											if (m_webview) m_webview->OpenDevToolsWindow();
										}
										return S_OK;
									}).Get(), nullptr);

							LoadChartHtml(); // ← 여기서 호출
							return S_OK;
						}).Get());
				return S_OK;
			}).Get());
}


// ====== 크기 조정 ======
void CEeumMFCView::ResizeWebView()
{
	if (!m_controller) return;
	CRect rc; GetClientRect(&rc);
	RECT bounds{ 0,0, rc.Width(), rc.Height() };
	m_controller->put_Bounds(bounds);
}

// ====== HTML 로드 ======
void CEeumMFCView::LoadChartHtml()
{
	wchar_t modulePath[MAX_PATH]{};
	GetModuleFileNameW(nullptr, modulePath, MAX_PATH);

	// exe → 상위(프로젝트 루트) → dashboard.html
	wchar_t exeDir[MAX_PATH]{}; wcscpy_s(exeDir, modulePath);
	PathRemoveFileSpecW(exeDir); // ...\x64\Debug
	PathRemoveFileSpecW(exeDir);
	PathRemoveFileSpecW(exeDir); // ...\ProjectRoot

	wchar_t htmlPath[MAX_PATH]{};
	PathCombineW(htmlPath, exeDir, L"dashboard.html");

	if (!PathFileExistsW(htmlPath)) {
		AfxMessageBox(L"프로젝트 루트에 dashboard.html 이 없습니다.");
		return;
	}
	CString uri = PathToFileUri(htmlPath);
	m_webview->Navigate(uri);
}

// ====== C++ → JS 데이터 푸시 ======
void CEeumMFCView::PushMetrics(const Metrics& m)
{
	if (!m_webview) return;

	// JS 측에서 chrome.webview.addEventListener('message')로 수신
	CString js;
	js.Format(
		L"chrome.webview.postMessage({"
		L"tAvg:%g, hAvg:%g, tEwma:%g, hEwma:%g, "
		L"dewPoint:%g, heatIndex:%g, spike:%s, "
		L"absHumidity:%g, wbgt:%g, pmv:%g, ppd:%g, "
		L"ts:Date.now()});",
		m.tAvg, m.hAvg, m.tEwma, m.hEwma,
		m.dewPoint, m.heatIndex, m.spike ? L"true" : L"false",
		m.absHumidity, m.wbgt, m.pmv, m.ppd
	);
	m_webview->ExecuteScript(js, nullptr);
}

// CEeumMFCView 그리기
void CEeumMFCView::OnDraw(CDC* /*pDC*/)
{
	CEeumMFCDoc* pDoc = GetDocument();
	ASSERT_VALID(pDoc);
	if (!pDoc) return;
	// WebView2가 모든 렌더링을 담당하므로 여기서는 그릴 것 없음.
}

// CEeumMFCView 인쇄
BOOL CEeumMFCView::OnPreparePrinting(CPrintInfo* pInfo)
{
	return DoPreparePrinting(pInfo);
}

void CEeumMFCView::OnBeginPrinting(CDC* /*pDC*/, CPrintInfo* /*pInfo*/)
{
	// TODO: 인쇄하기 전에 추가 초기화 작업을 추가합니다.
}

void CEeumMFCView::OnEndPrinting(CDC* /*pDC*/, CPrintInfo* /*pInfo*/)
{
	// TODO: 인쇄 후 정리 작업을 추가합니다.
}

// ====== 메시지 핸들러 ======
int CEeumMFCView::OnCreate(LPCREATESTRUCT lpCreateStruct)
{
	if (CView::OnCreate(lpCreateStruct) == -1) return -1;

	//CreateWebHostWindow();

	m_hWebHost = this->GetSafeHwnd();
	InitWebView(); // 비동기 초기화, 완료되면 HTML 로드됨
	return 0;
}

void CEeumMFCView::OnSize(UINT nType, int cx, int cy)
{
	CView::OnSize(nType, cx, cy);
	ResizeWebView();
}

void CEeumMFCView::OnShowWindow(BOOL bShow, UINT nStatus)
{
	CView::OnShowWindow(bShow, nStatus);
	// 창이 보인 뒤 한 번 더 리사이즈 (초기 0 크기 대비)
	ResizeWebView();
}

void CEeumMFCView::OnTimer(UINT_PTR nIDEvent)
{
	if (nIDEvent == 1) {
		// 데모용 더미 데이터
		static double t = 23.0, h = 55.0;
		t += (rand() % 100 - 50) * 0.01;
		h += (rand() % 100 - 50) * 0.02;

		Metrics m{};
		m.tAvg = t;
		m.hAvg = h;
		m.tEwma = t;   // 예시: 그대로 사용
		m.hEwma = h;
		m.dewPoint = t - ((100 - h) / 5.0);  // 단순 근사
		m.heatIndex = t + 0.5;                // 단순 근사
		m.spike = (fabs(t - 23.0) > 2.0);   // 단순 조건
		m.absHumidity = h * 0.25;               // 단순 근사
		m.wbgt = t - 0.7;                // 단순 근사
		m.pmv = (t - 23.0) / 10.0;          // 단순 근사
		m.ppd = (std::min)(100.0, fabs(m.pmv) * 20); // 단순 근사
		PushMetrics(m);
	}
	CView::OnTimer(nIDEvent);
}

BOOL CEeumMFCView::OnEraseBkgnd(CDC* pDC)
{
	// WebView2가 덮어쓰므로 깜빡임 방지용으로 기본 배경 지우기 생략
	return TRUE;
}

// CEeumMFCView 진단
#ifdef _DEBUG
void CEeumMFCView::AssertValid() const
{
	CView::AssertValid();
}

void CEeumMFCView::Dump(CDumpContext& dc) const
{
	CView::Dump(dc);
}

CEeumMFCDoc* CEeumMFCView::GetDocument() const // 디버그되지 않은 버전은 인라인으로 지정됩니다.
{
	ASSERT(m_pDocument->IsKindOf(RUNTIME_CLASS(CEeumMFCDoc)));
	return (CEeumMFCDoc*)m_pDocument;
}
#endif //_DEBUG


// CEeumMFCView 메시지 처리기
