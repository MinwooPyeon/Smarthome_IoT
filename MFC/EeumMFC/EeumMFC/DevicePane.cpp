#include "pch.h"
#include "DevicePane.h"

IMPLEMENT_DYNAMIC(CDevicePane, CDockablePane)

BEGIN_MESSAGE_MAP(CDevicePane, CDockablePane)
    ON_WM_CREATE()
    ON_WM_SIZE()
    ON_NOTIFY(TVN_SELCHANGED, 1, &CDevicePane::OnTvnSelChanged)
END_MESSAGE_MAP()

void CDevicePane::OnTvnSelChanged(NMHDR* pNMHDR, LRESULT* pResult)
{
    *pResult = 0;
    HTREEITEM hSel = m_tree.GetSelectedItem();
    if (!hSel) return;

    CString txt = m_tree.GetItemText(hSel);
    if (auto* wnd = AfxGetMainWnd()) {
        auto* payload = new CString(txt);
        ::PostMessage(wnd->GetSafeHwnd(), WM_APP_SELECT_HUB, 0, (LPARAM)payload);
    }
}

int CDevicePane::OnCreate(LPCREATESTRUCT lpCreateStruct)
{
    if (CDockablePane::OnCreate(lpCreateStruct) == -1) return -1;
    CRect rc(0, 0, 0, 0);
    m_tree.Create(WS_CHILD | WS_VISIBLE | WS_BORDER | TVS_HASBUTTONS | TVS_LINESATROOT | TVS_HASLINES,
        rc, this, 1);

    // »ùÇÃ ³ëµå
    HTREEITEM hRoot = m_tree.InsertItem(L"Devices");
    m_tree.InsertItem(L"hub/001", 0, 0, hRoot);
    m_tree.InsertItem(L"hub/002", 0, 0, hRoot);
    m_tree.Expand(hRoot, TVE_EXPAND);
    return 0;
}

void CDevicePane::OnSize(UINT nType, int cx, int cy)
{
    CDockablePane::OnSize(nType, cx, cy);
    AdjustLayout();
}

void CDevicePane::AdjustLayout()
{
    if (!GetSafeHwnd()) return;
    CRect rc; GetClientRect(&rc);
    m_tree.MoveWindow(rc);
}
