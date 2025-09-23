
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

#include <wrl.h>          // Microsoft::WRL::ComPtr
#include <WebView2.h>
#include <string>
#include <sstream>
#include <algorithm>      // std::replace
#include <cmath>          // std::isfinite

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
	ON_WM_SIZE()
	ON_MESSAGE(WM_APP_INIT_WEBVIEW, &CEeumMFCView::OnInitWebViewMsg)
END_MESSAGE_MAP()

static void TraceW(const wchar_t* s) { ::OutputDebugStringW(s); ::OutputDebugStringW(L"\r\n"); }
static std::wstring ToW(const std::string& s) { return std::wstring(s.begin(), s.end()); }
static double jnum(double v) { return std::isfinite(v) ? v : 0.0; } // NaN/Inf 방어
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
static std::wstring BuildDashboardFileUri(std::wstring* outPath = nullptr)
{
	wchar_t buf[MAX_PATH] = { 0 };
	GetModuleFileNameW(nullptr, buf, MAX_PATH);
	std::wstring exePath(buf);

	// 디렉터리 추출
	size_t pos = exePath.find_last_of(L"\\/");
	std::wstring dir = (pos == std::wstring::npos) ? L"." : exePath.substr(0, pos);

	// dashboard.html 붙이기
	std::wstring full = dir + L"\\dashboard.html";
	std::wstring html = dir + L"\\dashboard.html";
	if (!PathFileExistsW(html.c_str())) {
		// ② exe\..\dashboard.html
		std::wstring html2 = dir + L"\\..\\dashboard.html";
		if (PathFileExistsW(html2.c_str())) html = html2;
	}
	if (outPath) *outPath = html;

	std::wstring uri = L"file:///" + html;
	std::replace(uri.begin(), uri.end(), L'\\', L'/');
	return uri;
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
void CEeumMFCView::InitWebView()
{
	RECT rc{}; GetClientRect(&rc);

	HRESULT hr = CreateCoreWebView2EnvironmentWithOptions(
		nullptr, nullptr, nullptr,
		Microsoft::WRL::Callback<ICoreWebView2CreateCoreWebView2EnvironmentCompletedHandler>(
			[this, rc](HRESULT hrEnv, ICoreWebView2Environment* envRaw) -> HRESULT {
				if (FAILED(hrEnv) || !envRaw) return hrEnv;

				m_env = envRaw;
				return m_env->CreateCoreWebView2Controller(
					m_hWnd,
					Microsoft::WRL::Callback<ICoreWebView2CreateCoreWebView2ControllerCompletedHandler>(
						[this, rc](HRESULT hrCtrl, ICoreWebView2Controller* ctrlRaw) -> HRESULT {
							if (FAILED(hrCtrl) || !ctrlRaw) return hrCtrl;

							m_ctrl = ctrlRaw;
							m_ctrl->put_Bounds(rc);
							m_ctrl->put_IsVisible(true);

							HRESULT hrWV = m_ctrl->get_CoreWebView2(m_webview.GetAddressOf());
							if (FAILED(hrWV) || !m_webview) return hrWV;

							ComPtr<ICoreWebView2Settings> settings;
							if (SUCCEEDED(m_webview->get_Settings(settings.GetAddressOf())) && settings) {
								settings->put_AreDefaultScriptDialogsEnabled(TRUE);
								settings->put_IsScriptEnabled(TRUE);
								settings->put_IsWebMessageEnabled(TRUE);
								settings->put_AreDevToolsEnabled(TRUE);
							}

							// ready 수신 → 마지막 값 1회 재전송
							m_webview->add_WebMessageReceived(
								Microsoft::WRL::Callback<ICoreWebView2WebMessageReceivedEventHandler>(
									[this](ICoreWebView2*, ICoreWebView2WebMessageReceivedEventArgs* args)->HRESULT {
										LPWSTR json = nullptr;
										if (SUCCEEDED(args->get_WebMessageAsJson(&json)) && json) {
											if (wcsstr(json, L"\"type\":\"ready\"")) {
												m_pageReady_ = true;
												PushMetrics(m_last_);
											}
											CoTaskMemFree(json);
										}
										return S_OK;
									}
								).Get(), nullptr);

							m_webview->add_WebMessageReceived(
								Microsoft::WRL::Callback<ICoreWebView2WebMessageReceivedEventHandler>(
									[this](ICoreWebView2*, ICoreWebView2WebMessageReceivedEventArgs* args)->HRESULT {
										LPWSTR json = nullptr;
										if (SUCCEEDED(args->get_WebMessageAsJson(&json)) && json) {
											if (wcsstr(json, L"\"type\":\"ready\"")) {
												m_pageReady_ = true;
												PushMetrics(m_last_); // 준비되면 마지막 값 1회 재전송
											}
											CoTaskMemFree(json);
										}
										return S_OK;
									}
								).Get(), nullptr);

							// ★ filesystem 없이 URI 생성
							std::wstring diskPath;
							std::wstring uri = BuildDashboardFileUri(&diskPath);
							if (PathFileExistsW(diskPath.c_str())) {
								std::wstring dbg = L"[WebView] Navigate to " + uri + L"\r\n";
								::OutputDebugStringW(dbg.c_str());
								m_webview->Navigate(uri.c_str());
							}
							
							return S_OK;
						}
					).Get()
				);
			}
		).Get()
	);

	if (FAILED(hr)) {
		AfxMessageBox(L"Failed to create WebView2 environment");
	}
}
// ====== 크기 조정 ======
void CEeumMFCView::ResizeWebView()
{
	if (!m_ctrl) return;
	CRect rc; GetClientRect(&rc);
	RECT bounds{ 0,0, rc.Width(), rc.Height() };
	m_ctrl->put_Bounds(bounds);
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
	static unsigned long long seq = 0;
	m_last_ = m;
	if (!m_webview || !m_pageReady_) return;

	std::ostringstream oss;
	oss << "{"
		<< "\"type\":\"metrics\","
		<< "\"seq\":" << (++seq) << ","            // ★ 추가
		<< "\"tAvg\":" << jnum(m.tAvg) << ","
		<< "\"hAvg\":" << jnum(m.hAvg) << ","
		<< "\"tEwma\":" << jnum(m.tEwma) << ","
		<< "\"hEwma\":" << jnum(m.hEwma) << ","
		<< "\"dewPoint\":" << jnum(m.dewPoint) << ","
		<< "\"heatIndex\":" << jnum(m.heatIndex) << ","
		<< "\"spike\":" << (m.spike ? "true" : "false") << ","
		<< "\"absHumidity\":" << jnum(m.absHumidity) << ","
		<< "\"wbgt\":" << jnum(m.wbgt) << ","
		<< "\"pmv\":" << jnum(m.pmv) << ","
		<< "\"ppd\":" << jnum(m.ppd) << ","
		<< "\"ts\":" << static_cast<long long>(::GetTickCount64())
		<< "}";

	std::wstring wjson(oss.str().begin(), oss.str().end());
	m_webview->PostWebMessageAsJson(wjson.c_str());
}

int CEeumMFCView::OnCreate(LPCREATESTRUCT cs)
{
	if (CView::OnCreate(cs) == -1) return -1;
	// HWND가 생겼을 때 “지연” 초기화 예약
	if (!m_webviewInitPosted_) {
		m_webviewInitPosted_ = true;
		PostMessage(WM_APP_INIT_WEBVIEW, 0, 0);
		TraceW(L"[View] Post INIT_WEBVIEW from OnCreate");
	}
	return 0;
}
void CEeumMFCView::OnInitialUpdate()
{
	CView::OnInitialUpdate();
	// 일부 템플릿에선 OnCreate보다 여기 타이밍이 확실한 경우가 있어, 한 번 더 보강
	if (!m_webviewInitPosted_) {
		m_webviewInitPosted_ = true;
		PostMessage(WM_APP_INIT_WEBVIEW, 0, 0);
		TraceW(L"[View] Post INIT_WEBVIEW from OnInitialUpdate");
	}
}
LRESULT CEeumMFCView::OnInitWebViewMsg(WPARAM, LPARAM)
{
	if (m_webviewInitRunning_) {
		TraceW(L"[View] InitWebView already running; skip");
		return 0;
	}
	m_webviewInitRunning_ = true;

	TraceW(L"[View] InitWebView() start");
	InitWebView();                     // ← 여기서 실제 WebView 생성/네비
	TraceW(L"[View] InitWebView() done");

	m_webviewInitRunning_ = false;
	return 0;
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

void CEeumMFCView::OnSize(UINT nType, int cx, int cy)
{
	CView::OnSize(nType, cx, cy);
	if (m_ctrl) {
		RECT r{ 0,0,cx,cy };
		m_ctrl->put_Bounds(r);
		m_ctrl->put_IsVisible(TRUE);
	}

	// 만약 아직 초기화 예약을 못 했고, 이제 HWND가 생겼다면 여기서도 예약
	if (!m_webviewInitPosted_ && ::IsWindow(m_hWnd)) {
		m_webviewInitPosted_ = true;
		PostMessage(WM_APP_INIT_WEBVIEW, 0, 0);
		TraceW(L"[View] Post INIT_WEBVIEW from OnSize");
	}
}


void CEeumMFCView::OnUpdate(CView*, LPARAM, CObject*) {
	auto* doc = GetDocument();
	if (!doc || !m_webview) return;

	Metrics m{};
	{
		std::lock_guard<std::mutex> lk(doc->mtx_);
		m = doc->latestMet_;
	}

	PushMetrics(m);
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
