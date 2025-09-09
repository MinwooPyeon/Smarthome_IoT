#pragma once
#include <afxdockablepane.h>

class CLogPane : public CDockablePane
{
public:
    CListCtrl m_list;

    void AdjustLayout()
    {
        if (!GetSafeHwnd()) return;
        CRect rc; GetClientRect(&rc);
        m_list.MoveWindow(rc);
    }

    void Append(const CString& level, const CString& msg)
    {
        int idx = m_list.InsertItem(m_list.GetItemCount(), level);
        m_list.SetItemText(idx, 1, msg);
        // 마지막 행 보이기
        m_list.EnsureVisible(idx, FALSE);
    }

protected:
    afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct)
    {
        if (CDockablePane::OnCreate(lpCreateStruct) == -1) return -1;
        CRect rc(0, 0, 0, 0);
        m_list.Create(WS_CHILD | WS_VISIBLE | WS_BORDER | LVS_REPORT | LVS_SHOWSELALWAYS,
            rc, this, 1);
        m_list.SetExtendedStyle(LVS_EX_FULLROWSELECT | LVS_EX_GRIDLINES);
        m_list.InsertColumn(0, L"Level", LVCFMT_LEFT, 80);
        m_list.InsertColumn(1, L"Message", LVCFMT_LEFT, 600);

        // 예시 로그
        Append(L"INFO", L"Logger initialized");
        return 0;
    }

    afx_msg void OnSize(UINT nType, int cx, int cy)
    {
        CDockablePane::OnSize(nType, cx, cy);
        AdjustLayout();
    }

    DECLARE_MESSAGE_MAP()
};
BEGIN_MESSAGE_MAP(CLogPane, CDockablePane)
    ON_WM_CREATE()
    ON_WM_SIZE()
END_MESSAGE_MAP()
