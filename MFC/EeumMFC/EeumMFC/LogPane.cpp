#include "pch.h"
#include "LogPane.h"

IMPLEMENT_DYNAMIC(CLogPane, CDockablePane)

BEGIN_MESSAGE_MAP(CLogPane, CDockablePane)
    ON_WM_CREATE()
    ON_WM_SIZE()
END_MESSAGE_MAP()

int CLogPane::OnCreate(LPCREATESTRUCT lpCreateStruct)
{
    if (CDockablePane::OnCreate(lpCreateStruct) == -1) return -1;
    CRect rc(0, 0, 0, 0);
    m_list.Create(WS_CHILD | WS_VISIBLE | WS_BORDER | LVS_REPORT | LVS_SHOWSELALWAYS,
        rc, this, 1);
    m_list.SetExtendedStyle(LVS_EX_FULLROWSELECT | LVS_EX_GRIDLINES);
    m_list.InsertColumn(0, L"Level", LVCFMT_LEFT, 80);
    m_list.InsertColumn(1, L"Message", LVCFMT_LEFT, 600);
    Append(L"INFO", L"Logger initialized");
    return 0;
}

void CLogPane::OnSize(UINT nType, int cx, int cy)
{
    CDockablePane::OnSize(nType, int(cx), int(cy));
    AdjustLayout();
}

void CLogPane::AdjustLayout()
{
    if (!GetSafeHwnd()) return;
    CRect rc; GetClientRect(&rc);
    m_list.MoveWindow(rc);
}

void CLogPane::Append(const CString& level, const CString& msg)
{
    int idx = m_list.InsertItem(m_list.GetItemCount(), level);
    m_list.SetItemText(idx, 1, msg);
    m_list.EnsureVisible(idx, FALSE);
}
