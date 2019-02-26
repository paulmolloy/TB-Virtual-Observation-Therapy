package ie.tcd.paulm.tbvideojournal.mainmenu;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int NumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.NumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                TabProfileFragment tab1 = new TabProfileFragment();
                return tab1;
            case 1:
                TabVideoFragment tab2 = new TabVideoFragment();
                return tab2;
            case 2:
                TabDiaryFragment tab3 = new TabDiaryFragment();
                return tab3;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return NumOfTabs;
    }
}
