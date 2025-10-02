#pragma once
#include <afxdockablepane.h>

class CLogPane : public CDockablePane
{
    DECLARE_DYNAMIC(CLogPane)
public:
    CLogPane() = default;

    CListCtrl m_list;
    void AdjustLayout();
    void Append(const CString& level, const CString& msg);

protected:
    afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct);
    afx_msg void OnSize(UINT nType, int cx, int cy);
    DECLARE_MESSAGE_MAP()
};
