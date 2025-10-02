#pragma once
#include <afxdockablepane.h>
#include <afxpropertygridctrl.h>

class CFactorPane : public CDockablePane
{
    DECLARE_DYNAMIC(CFactorPane)
public:
    CFactorPane() = default;

    CMFCPropertyGridCtrl m_grid;
    void AdjustLayout();
    void SetDouble(const CString& name, double v);

protected:
    afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct);
    afx_msg void OnSize(UINT nType, int cx, int cy);
    DECLARE_MESSAGE_MAP()
};
