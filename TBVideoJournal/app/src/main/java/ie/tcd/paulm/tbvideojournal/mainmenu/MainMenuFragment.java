package ie.tcd.paulm.tbvideojournal.mainmenu;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.TabLayout;

import ie.tcd.paulm.tbvideojournal.MainActivity;
import ie.tcd.paulm.tbvideojournal.R;

public class MainMenuFragment extends Fragment {
    public MainMenuFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main_menu, container, false);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.profile_icon));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.video_icon));
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.diary_icon));
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);


        final ViewPager viewPager = (ViewPager) view.findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter
                (((AppCompatActivity)getActivity()).getSupportFragmentManager(), tabLayout.getTabCount());
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        return view;

    }

    private MainActivity getRoot(){
        return (MainActivity) getActivity();
    }

}
