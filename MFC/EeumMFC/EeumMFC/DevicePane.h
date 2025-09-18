#pragma once
#include <afxdockablepane.h>

class CDevicePane : public CDockablePane
{
    DECLARE_DYNAMIC(CDevicePane)
public:
    CDevicePane() = default;

    CTreeCtrl m_tree;
    void AdjustLayout();

protected:
    afx_msg void OnTvnSelChanged(NMHDR* pNMHDR, LRESULT* pResult);
    afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct);
    afx_msg void OnSize(UINT nType, int cx, int cy);
    DECLARE_MESSAGE_MAP()
};
