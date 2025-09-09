#pragma once
#include <afxdockablepane.h>
#include <afxdialogex.h>
#include <afxpropertygridctrl.h>

class CFactorPane : public CDockablePane
{
public:
    CMFCPropertyGridCtrl m_grid;

    void AdjustLayout()
    {
        if (!GetSafeHwnd()) return;
        CRect rc; GetClientRect(&rc);
        m_grid.MoveWindow(rc);
    }

    // 값 갱신용 헬퍼
    void SetDouble(const CString& name, double v)
    {
        for (int i = 0; i < m_grid.GetPropertyCount(); ++i)
        {
            CMFCPropertyGridProperty* p = m_grid.GetProperty(i);
            if (p && p->GetName() == name) {
                CString s; s.Format(L"%.3f", v);
                p->SetValue(_variant_t(s));
                break;
            }
        }
    }

protected:
    afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct)
    {
        if (CDockablePane::OnCreate(lpCreateStruct) == -1) return -1;
        CRect rc(0, 0, 0, 0);
        m_grid.Create(WS_CHILD | WS_VISIBLE | WS_BORDER, rc, this, 1);
        m_grid.EnableHeaderCtrl(TRUE);
        m_grid.EnableDescriptionArea();
        m_grid.SetVSDotNetLook(TRUE);
        m_grid.MarkModifiedProperties(TRUE);

        // 예시 항목들
        m_grid.AddProperty(new CMFCPropertyGridProperty(L"Alpha T (EWMA)", (_variant_t)L"0.20", L"Temperature EWMA factor"));
        m_grid.AddProperty(new CMFCPropertyGridProperty(L"Alpha H (EWMA)", (_variant_t)L"0.20", L"Humidity EWMA factor"));
        m_grid.AddProperty(new CMFCPropertyGridProperty(L"Met (met)", (_variant_t)L"1.20", L"Metabolic rate"));
        m_grid.AddProperty(new CMFCPropertyGridProperty(L"Clo (clo)", (_variant_t)L"0.50", L"Clothing insulation"));
        m_grid.AddProperty(new CMFCPropertyGridProperty(L"Air Vel (m/s)", (_variant_t)L"0.10", L"Air speed"));
        return 0;
    }

    afx_msg void OnSize(UINT nType, int cx, int cy)
    {
        CDockablePane::OnSize(nType, cx, cy);
        AdjustLayout();
    }

    DECLARE_MESSAGE_MAP()
};
BEGIN_MESSAGE_MAP(CFactorPane, CDockablePane)
    ON_WM_CREATE()
    ON_WM_SIZE()
END_MESSAGE_MAP()
