
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

using Microsoft::WRL::ComPtr;
using Microsoft::WRL::Callback;


#ifdef _DEBUG
#define new DEBUG_NEW
#endif

// ====== 차트 HTML (ECharts, Temp/Hum 2패널) ======
static const wchar_t* kChartHtml = LR"HTML(
<!doctype html>
<html>
<head>
<meta charset="utf-8"/>
<meta http-equiv="X-UA-Compatible" content="IE=edge"/>
<meta name="viewport" content="width=device-width,initial-scale=1"/>
<title>ECharts - Temp/Hum</title>
<style>
 html,body,#root{height:100%; margin:0; padding:0; overflow:hidden; background:#0b0f14;}
 #root{display:flex; flex-direction:column; gap:8px; padding:8px; box-sizing:border-box;}
 .panel{flex:1; min-height:0;}
 .label{color:#9ab; font-family:Segoe UI, sans-serif; margin-bottom:4px}
</style>
</head>
<body>
<div id="root">
  <div class="label">EEUM · Live Env Chart</div>
  <div id="temp" class="panel"></div>
  <div id="hum" class="panel"></div>
</div>
<script src="https://cdn.jsdelivr.net/npm/echarts@5/dist/echarts.min.js"></script>
<script>
  const tDom = document.getElementById('temp');
  const hDom = document.getElementById('hum');
  const tChart = echarts.init(tDom);
  const hChart = echarts.init(hDom);

  const now = Date.now();
  const xs = Array.from({length:50}, (_,i)=> new Date(now - (49-i)*1000).toLocaleTimeString());
  const tData = Array.from({length:50}, ()=> 23 + (Math.random()-0.5)*1.5);
  const hData = Array.from({length:50}, ()=> 55 + (Math.random()-0.5)*3);

  const makeOpt = (name, unit, data) => ({
    backgroundColor: 'transparent',
    grid:{left:48,right:16,top:18,bottom:28},
    xAxis:{type:'category', data: xs, boundaryGap:false, axisLabel:{show:false}},
    yAxis:{type:'value', name: unit, nameTextStyle:{color:'#9ab'}, axisLine:{lineStyle:{color:'#789'}}, splitLine:{lineStyle:{type:'dashed'}}},
    series:[{name, type:'line', data, smooth:true, showSymbol:false, areaStyle:{}}],
    tooltip:{trigger:'axis'}
  });

  tChart.setOption(makeOpt('Temperature','°C',tData));
  hChart.setOption(makeOpt('Humidity','%',hData));

  new ResizeObserver(()=>{ tChart.resize(); hChart.resize(); }).observe(document.body);

  // C++ → JS 데이터 수신
  if (window.chrome && chrome.webview) {
    chrome.webview.addEventListener('message', (msg)=>{
      const {temp, hum, ts} = msg.data || {};
      const label = new Date(ts || Date.now()).toLocaleTimeString();

      xs.push(label); tData.push(temp); hData.push(hum);
      if (xs.length>300){ xs.shift(); tData.shift(); hData.shift(); }

      tChart.setOption({ xAxis:{data:xs}, series:[{data:tData}] });
      hChart.setOption({ xAxis:{data:xs}, series:[{data:hData}] });
    });
  }
</script>
</body>
</html>
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
	if (!m_webview) return;
	m_webview->NavigateToString(kChartHtml);
}

// ====== C++ → JS 데이터 푸시 ======
void CEeumMFCView::PushData(double temp, double hum)
{
	if (!m_webview) return;

	// JS 측에서 chrome.webview.addEventListener('message')로 수신
	CString js;
	js.Format(L"chrome.webview.postMessage({temp:%g, hum:%g, ts: Date.now()});", temp, hum);
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
		PushData(t, h);
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
