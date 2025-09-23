
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
	ON_WM_SHOWWINDOW()
	ON_WM_SIZE()
	ON_WM_TIMER()
	ON_WM_ERASEBKGND()
END_MESSAGE_MAP()

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
static std::wstring BuildDashboardFileUri()
{
	wchar_t buf[MAX_PATH] = { 0 };
	GetModuleFileNameW(nullptr, buf, MAX_PATH);
	std::wstring exePath(buf);

	// 디렉터리 추출
	size_t pos = exePath.find_last_of(L"\\/");
	std::wstring dir = (pos == std::wstring::npos) ? L"." : exePath.substr(0, pos);

	// dashboard.html 붙이기
	std::wstring full = dir + L"\\dashboard.html";

	// file:/// URI 만들기 (역슬래시를 슬래시로)
	std::wstring uri = L"file:///" + full;
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

							m_webview->add_NavigationCompleted(
								Microsoft::WRL::Callback<ICoreWebView2NavigationCompletedEventHandler>(
									[](ICoreWebView2*, ICoreWebView2NavigationCompletedEventArgs* e)->HRESULT {
										BOOL ok = FALSE; e->get_IsSuccess(&ok);
										if (!ok) ::OutputDebugStringW(L"[WebView] Navigation failed\r\n");
										return S_OK;
									}
								).Get(), nullptr);

							// ★ filesystem 없이 URI 생성
							std::wstring uri = BuildDashboardFileUri();
							m_webview->Navigate(uri.c_str());
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
	m_last_ = m;
	if (!m_webview || !m_pageReady_) return;

	std::ostringstream oss;
	oss << "{"
		<< "\"type\":\"metrics\","
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

	std::string json = oss.str();
	std::wstring wjson(json.begin(), json.end());
	m_webview->PostWebMessageAsJson(wjson.c_str());
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

	//TODO : DUMMY TEST용 코드
	//SetTimer(1, 1000, NULL); // 1초마다 더미 Metrics 발생

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
