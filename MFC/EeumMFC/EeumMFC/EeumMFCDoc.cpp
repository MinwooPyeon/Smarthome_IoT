
// EeumMFCDoc.cpp: CEeumMFCDoc 클래스의 구현
//

#include "pch.h"
#include "framework.h"
// SHARED_HANDLERS는 미리 보기, 축소판 그림 및 검색 필터 처리기를 구현하는 ATL 프로젝트에서 정의할 수 있으며
// 해당 프로젝트와 문서 코드를 공유하도록 해 줍니다.
#ifndef SHARED_HANDLERS
#include "EeumMFC.h"
#endif

#include "MainFrm.h"
#include "EeumMFCDoc.h"

#include <propkey.h>
#include <nlohmann/json.hpp>

using json = nlohmann::json;

static EnvSample ParseEnv(const std::string& s) {
	EnvSample e{};
	try {
		auto j = json::parse(s);
		e.t = j.value("t", j.value("temperature", NAN));
		e.h = j.value("h", j.value("humidity", NAN));
		e.tsMs = j.value("ts", (int64_t)::GetTickCount64());
	}
	catch (...) {}
	return e;
}

static IrEvent ParseIr(const std::string& s)
{
	IrEvent e{};
	// TODO: 필요한 필드만 최소 파싱
	return e;
}

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

// CEeumMFCDoc

IMPLEMENT_DYNCREATE(CEeumMFCDoc, CDocument)

BEGIN_MESSAGE_MAP(CEeumMFCDoc, CDocument)
END_MESSAGE_MAP()


// CEeumMFCDoc 생성/소멸

CEeumMFCDoc::CEeumMFCDoc() noexcept {}

CEeumMFCDoc::~CEeumMFCDoc() {}

void CEeumMFCDoc::SetSelectedHub(const CString& hub)
{
	std::wstring ws(hub);
	std::string s(ws.begin(), ws.end());   // hub/001

	if (s.rfind("hub/", 0) != 0) s = "hub/" + s;



	if (!lastOrderedHub_.empty() && lastOrderedHub_ != s && mqtt_) {
		mqtt_->orderEnv(lastOrderedHub_, false); // 그만 보내!
	}
	selectedHub_ = s;

	if (mqtt_) {
		mqtt_->setTopics({ selectedHub_ + "/env", selectedHub_ + "/log" });

		// 스트리밍 시작
		mqtt_->orderEnv(selectedHub_, true);
		lastOrderedHub_ = selectedHub_;
	}

	// 보기 좋게 버퍼 초기화(선택)
	{
		std::lock_guard<std::mutex> lk(mtx_);
		latestEnv_.clear();
		latestIr_.clear();
		// latestMet_는 필요시 초기화
	}
}

BOOL CEeumMFCDoc::OnNewDocument()
{
	if (!CDocument::OnNewDocument())
		return FALSE;

	ingestor_.setCallback([this](const auto& env, const auto& ir, const Metrics& met) {
		{
			std::lock_guard<std::mutex> lk(mtx_);
			latestEnv_ = env;
			latestIr_ = ir;
			latestMet_ = met;
		}

		// 1) 기존 경로 유지 (원하면)
		if (CWnd* pMain = AfxGetMainWnd()) {
			::PostMessage(pMain->GetSafeHwnd(), WM_APP_DATAREADY, 0, 0);
		}

		// 2) ★ 웹뷰로 직통 전송 (끊김 방지)
		if (viewHwnd_ && ::IsWindow(viewHwnd_)) {
			// heap에 복사해서 넘기고, 뷰에서 delete(unique_ptr) 처리
			auto* pm = new Metrics(met);
			::PostMessage(viewHwnd_, WM_APP_METRICS, 0, reinterpret_cast<LPARAM>(pm));
		}
		});

	ingestor_.start(0.5);

	Config config;
	mqtt_ = std::make_unique<MqttClient>(config);

	mqtt_->onMessage = [this](const std::string& topic, const std::string& payload) {
		if (topic.find("/env") != std::string::npos) {
			auto e = ParseEnv(payload);
			ingestor_.pushEnv(e);
		}
		else if (topic.find("/irsignal") != std::string::npos) {
			if (auto* mf = dynamic_cast<CMainFrame*>(AfxGetMainWnd())) {
				CString* msg = new CString(CA2W(payload.c_str()));
				::PostMessage(mf->GetSafeHwnd(), WM_APP_LOG, 0, (LPARAM)msg);
			}
		}
		};

	return TRUE;
}

void CEeumMFCDoc::OnCloseDocument() {
	if (mqtt_ && !selectedHub_.empty()) {
		mqtt_->orderEnv(selectedHub_, false);
	}

	// Ingestor/Analyzer/Timer 같은 내부 스레드 종료
	ingestor_.stop();   // 내부에서 join!
	
	// MQTT 네트워크 스레드 정리 (소멸자에서 loop_stop+disconnect)
	mqtt_->disconnect();
	mqtt_->loop_stop(true);
	mqtt_.reset();

	CDocument::OnCloseDocument();
}


// CEeumMFCDoc serialization

void CEeumMFCDoc::Serialize(CArchive& ar)
{
	if (ar.IsStoring())
	{
		// TODO: 여기에 저장 코드를 추가합니다.
	}
	else
	{
		// TODO: 여기에 로딩 코드를 추가합니다.
	}
}

#ifdef SHARED_HANDLERS

// 축소판 그림을 지원합니다.
void CEeumMFCDoc::OnDrawThumbnail(CDC& dc, LPRECT lprcBounds)
{
	// 문서의 데이터를 그리려면 이 코드를 수정하십시오.
	dc.FillSolidRect(lprcBounds, RGB(255, 255, 255));

	CString strText = _T("TODO: implement thumbnail drawing here");
	LOGFONT lf;

	CFont* pDefaultGUIFont = CFont::FromHandle((HFONT)GetStockObject(DEFAULT_GUI_FONT));
	pDefaultGUIFont->GetLogFont(&lf);
	lf.lfHeight = 36;

	CFont fontDraw;
	fontDraw.CreateFontIndirect(&lf);

	CFont* pOldFont = dc.SelectObject(&fontDraw);
	dc.DrawText(strText, lprcBounds, DT_CENTER | DT_WORDBREAK);
	dc.SelectObject(pOldFont);
}

// 검색 처리기를 지원합니다.
void CEeumMFCDoc::InitializeSearchContent()
{
	CString strSearchContent;
	// 문서의 데이터에서 검색 콘텐츠를 설정합니다.
	// 콘텐츠 부분은 ";"로 구분되어야 합니다.

	// 예: strSearchContent = _T("point;rectangle;circle;ole object;");
	SetSearchContent(strSearchContent);
}

void CEeumMFCDoc::SetSearchContent(const CString& value)
{
	if (value.IsEmpty())
	{
		RemoveChunk(PKEY_Search_Contents.fmtid, PKEY_Search_Contents.pid);
	}
	else
	{
		CMFCFilterChunkValueImpl* pChunk = nullptr;
		ATLTRY(pChunk = new CMFCFilterChunkValueImpl);
		if (pChunk != nullptr)
		{
			pChunk->SetTextValue(PKEY_Search_Contents, value, CHUNK_TEXT);
			SetChunkValue(pChunk);
		}
	}
}

#endif // SHARED_HANDLERS

// CEeumMFCDoc 진단

#ifdef _DEBUG
void CEeumMFCDoc::AssertValid() const
{
	CDocument::AssertValid();
}

void CEeumMFCDoc::Dump(CDumpContext& dc) const
{
	CDocument::Dump(dc);
}
#endif //_DEBUG


// CEeumMFCDoc 명령
