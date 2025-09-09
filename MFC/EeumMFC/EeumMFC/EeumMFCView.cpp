
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

using Microsoft::WRL::ComPtr;
using Microsoft::WRL::Callback;


#ifdef _DEBUG
#define new DEBUG_NEW
#endif

// ====== 차트 HTML (ECharts, Temp/Hum 2패널) ======
static const wchar_t* kChartHtml = LR"HTML(

)HTML";

// CEeumMFCView

IMPLEMENT_DYNCREATE(CEeumMFCView, CView)

BEGIN_MESSAGE_MAP(CEeumMFCView, CView)
	// 표준 인쇄 명령입니다.
	ON_COMMAND(ID_FILE_PRINT, &CView::OnFilePrint)
	ON_COMMAND(ID_FILE_PRINT_DIRECT, &CView::OnFilePrint)
	ON_COMMAND(ID_FILE_PRINT_PREVIEW, &CView::OnFilePrintPreview)

	ON_WM_CREATE()
	ON_WM_SIZE()
	ON_WM_TIMER()
	ON_WM_ERASEBKGND()
END_MESSAGE_MAP()

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
	if (m_webview) return;

	CreateCoreWebView2EnvironmentWithOptions(
		nullptr, nullptr, nullptr,
		Callback<ICoreWebView2CreateCoreWebView2EnvironmentCompletedHandler>(
			[this](HRESULT result, ICoreWebView2Environment* env)->HRESULT
			{
				if (FAILED(result) || !env) return result;
				m_env = env;

				env->CreateCoreWebView2Controller(
					m_hWebHost,
					Callback<ICoreWebView2CreateCoreWebView2ControllerCompletedHandler>(
						[this](HRESULT result2, ICoreWebView2Controller* controller)->HRESULT
						{
							if (FAILED(result2) || !controller) return result2;
							m_controller = controller;

							m_controller->get_CoreWebView2(&m_webview);
							ResizeWebView();
							LoadChartHtml();

							// JS → C++ 메시지 예시 핸들러 (필요 시 사용)
							if (m_webview) {
								m_webview->add_WebMessageReceived(
									Callback<ICoreWebView2WebMessageReceivedEventHandler>(
										[this](ICoreWebView2* /*sender*/, ICoreWebView2WebMessageReceivedEventArgs* args)->HRESULT
										{
											LPWSTR json = nullptr;
											args->TryGetWebMessageAsString(&json);
											if (json) {
												// TODO: json 사용 (필요시)
												::CoTaskMemFree(json);
											}
											return S_OK;
										}).Get(),
									nullptr);
							}

							// 데모: 1초마다 더미 데이터 푸시
							SetTimer(1, 1000, nullptr);
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

    // exe 경로 → 프로젝트 루트로 이동
    std::filesystem::path exeDir = std::filesystem::path(modulePath).parent_path();
    std::filesystem::path rootDir = exeDir.parent_path();  // 한 단계 위
    auto htmlPath = rootDir / L"dashboard.html";

    if (!std::filesystem::exists(htmlPath)) {
        AfxMessageBox(L"dashboard.html not found!");
        return;
    }

    CString url;
    url.Format(L"file:///%s", htmlPath.wstring().c_str());
    url.Replace(L"\\", L"/"); // 파일 URL 표준화
    m_webview->Navigate(url);
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

	CreateWebHostWindow();
	InitWebView(); // 비동기 초기화, 완료되면 HTML 로드됨
	return 0;
}

void CEeumMFCView::OnSize(UINT nType, int cx, int cy)
{
	CView::OnSize(nType, cx, cy);
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
