
// EeumMFCDoc.cpp: CEeumMFCDoc 클래스의 구현
//

#include "pch.h"
#include "framework.h"
// SHARED_HANDLERS는 미리 보기, 축소판 그림 및 검색 필터 처리기를 구현하는 ATL 프로젝트에서 정의할 수 있으며
// 해당 프로젝트와 문서 코드를 공유하도록 해 줍니다.
#ifndef SHARED_HANDLERS
#include "EeumMFC.h"
#endif

#include "EeumMFCDoc.h"

#include <propkey.h>

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

// CEeumMFCDoc

IMPLEMENT_DYNCREATE(CEeumMFCDoc, CDocument)

BEGIN_MESSAGE_MAP(CEeumMFCDoc, CDocument)
END_MESSAGE_MAP()


// CEeumMFCDoc 생성/소멸

CEeumMFCDoc::CEeumMFCDoc() noexcept{}

CEeumMFCDoc::~CEeumMFCDoc(){}

BOOL CEeumMFCDoc::OnNewDocument()
{
	if (!CDocument::OnNewDocument())
		return FALSE;

	m_ing.setCallback([this](const auto& env, const auto& ir, const Metrics& met) {
		m_lastEnvBatch = env;
		m_lastIrBatch = ir;
		m_lastMetrics = met;

		CWnd* pMain = AfxGetMainWnd();
		if (pMain && ::IsWindow(pMain->GetSafeHwnd()))
			::PostMessage(pMain->GetSafeHwnd(), WM_APP_DATAREADY, 0, 0);
		});

	m_ing.start(20.0);
	return TRUE;
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

	CFont* pDefaultGUIFont = CFont::FromHandle((HFONT) GetStockObject(DEFAULT_GUI_FONT));
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
		CMFCFilterChunkValueImpl *pChunk = nullptr;
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
