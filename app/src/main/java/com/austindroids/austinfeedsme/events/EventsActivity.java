package com.austindroids.austinfeedsme.events;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.austindroids.austinfeedsme.NavigationMenuAdapter;
import com.austindroids.austinfeedsme.R;
import com.austindroids.austinfeedsme.addeditevent.AddEditEventActivity;
import com.austindroids.austinfeedsme.choosemeetup.EventFilterActivity;
import com.austindroids.austinfeedsme.data.Event;
import com.austindroids.austinfeedsme.eventsmap.EventsMapActivity;
import com.austindroids.austinfeedsme.utility.DateUtils;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;


public class EventsActivity extends AppCompatActivity
        implements NavigationMenuAdapter.OnItemClickListener, EventsContract.View {

    public static final int RC_SIGN_IN = 7;

    // Navigation Menu member variables
    private ActionBarDrawerToggle mDrawerToggle;
    private DrawerLayout mDrawerLayout;
    private RecyclerView mDrawerList;
    private FloatingActionButton mAddEventFab;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private String[] mNavigationItems;
	
	private final static String TAG = "EventsActivity";

    private EventsContract.UserActionsListener mActionsListener;

    private EventsAdapter mListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        mActionsListener = new EventsPresenter(this, this);

        mTitle = mDrawerTitle = getTitle();

        mDrawerList = (RecyclerView) findViewById(R.id.left_drawer);
        mDrawerList.setLayoutManager(new LinearLayoutManager(this));

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mNavigationItems = getResources().getStringArray(R.array.navigation_items_array);

        // set a custom shadow that overlays the main content when the drawer opens
//        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // improve performance by indicating the list if fixed size.
        mDrawerList.setHasFixedSize(true);

        // set up the drawer's list view with items and click listener
        mDrawerList.setAdapter(new NavigationMenuAdapter(mNavigationItems, this));
        // enable ActionBar app icon to behave as action to toggle nav drawer

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.setDrawerIndicatorEnabled(true);

        mListAdapter = new EventsAdapter(new ArrayList<Event>(0), mEventItemListener);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.event_list_recycler_view);
        recyclerView.setAdapter(mListAdapter);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAddEventFab = (FloatingActionButton) findViewById(R.id.add_event_fab);
        mAddEventFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EventsActivity.this, AddEditEventActivity.class));
            }
        });

        mActionsListener.loadEvents();

    }

    /**
     * Listener for clicks on events in the RecyclerView.
     */
    EventItemListener mEventItemListener = new EventItemListener() {
        @Override
        public void onEventClick(Event clickedEvent) {
            mActionsListener.openEventDetails(clickedEvent);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_events, menu);
        // Get the SearchView and set the searchable configuration
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final SearchView searchView = (SearchView) menu.findItem(R.id.menu_search).getActionView();
        // Assumes current activity is the searchable activity
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
        // Fix to have searchview expand to fill entire ActionBar on tablets
        searchView.setMaxWidth(Integer.MAX_VALUE);

        final MenuItem searchMenu = menu.findItem(R.id.menu_search);

        MenuItemCompat.setOnActionExpandListener(searchMenu,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        mActionsListener.loadEvents();
                        return true;  // Return true to collapse action view
                    }

                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        // Do something when expanded
                        return true;  // Return true to expand action view
                    }
                });

        // Get the search close button image view
        ImageView closeButton = (ImageView) searchView.findViewById(R.id.search_close_btn);

        // Set on click listener
        closeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                //Find EditText view
                EditText et = (EditText) findViewById(R.id.search_src_text);

                //Clear the text from EditText view
                et.setText("");

                //Clear query
                searchView.setQuery("", false);
                //Collapse the action view
                searchView.onActionViewCollapsed();
                //Collapse the search widget
                searchMenu.collapseActionView();
            }
        });
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.login_menu_item)
                .setVisible(FirebaseAuth.getInstance().getCurrentUser() == null);
        menu.findItem(R.id.logout_menu_item)
                .setVisible(FirebaseAuth.getInstance().getCurrentUser() != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.login_menu_item:

                startActivityForResult(
                        // Get an instance of AuthUI based on the default app
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setProviders(AuthUI.EMAIL_PROVIDER).build(),
                        RC_SIGN_IN);
                return true;

            case R.id.logout_menu_item:

                AuthUI.getInstance(FirebaseApp.getInstance())
                        .signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        // user is now signed out
//                        AlphaAnimation animation1 = new AlphaAnimation(1, 0);
//                        animation1.setDuration(1000);
//                        animation1.setStartOffset(1000);
//                        animation1.setFillAfter(true);
//                        mAddEventFab.startAnimation(animation1);
//                        mAddEventFab.setVisibility(View.GONE);
                    }
                });
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (null != FirebaseAuth.getInstance().getCurrentUser()) {
//            mAddEventFab.setVisibility(View.VISIBLE);
//        } else {
//            mAddEventFab.setVisibility(View.GONE);
//        }
    }

    @Override
    protected void onPause() {
      super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            mActionsListener.searchEvents(query);
        }
    }

    @Override
    public void onClick(View view, int position) {
        Log.i(TAG, "Menu item with position: " + position + " was clicked.");
        switch(getResources().getStringArray(R.array.navigation_items_array)[position]) {
            case "Events List" :
                startActivity(new Intent(EventsActivity.this, EventsActivity.class));
                break;
            case "Events Map" :
                startActivity(new Intent(EventsActivity.this, EventsMapActivity.class));
                break;
            case "Food Filter" :
                startActivity(new Intent(EventsActivity.this, EventFilterActivity.class));
                break;
            default:
                startActivity(new Intent(EventsActivity.this, EventsActivity.class));
                break;
        }

        mDrawerLayout.closeDrawer(mDrawerList);

    }

    @Override
    public void showEvents(List<Event> events) {
        mListAdapter.replaceData(events);
    }

    private static class EventsAdapter extends RecyclerView.Adapter<EventsAdapter.ViewHolder> {

        private List<Event> mEvents;
        private EventItemListener mItemListener;

        public EventsAdapter(List<Event> Events, EventItemListener itemListener) {
            setList(Events);
            mItemListener = itemListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View EventView = inflater.inflate(R.layout.item_event, parent, false);

            return new ViewHolder(EventView, mItemListener);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            Event event = mEvents.get(position);

            viewHolder.eventDate.setText(DateUtils.getLocalDateFromTimestamp(event.getTime()));
            viewHolder.title.setText(event.getName());

            Spanned result;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(event.getDescription(),Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(event.getDescription());
            }
            viewHolder.description.setText(result);
            viewHolder.eventUrl.setText(event.getEvent_url());
        }

        public void replaceData(List<Event> Events) {
            setList(Events);
            notifyDataSetChanged();
        }

        private void setList(List<Event> Events) {
            mEvents = Events;
        }

        @Override
        public int getItemCount() {
            return mEvents.size();
        }

        public Event getItem(int position) {
            return mEvents.get(position);
        }

        public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

            public TextView eventDate;
            public TextView title;
            public TextView description;
            public TextView eventUrl;
            private EventItemListener mItemListener;

            public ViewHolder(View itemView, EventItemListener listener) {
                super(itemView);
                mItemListener = listener;
                eventDate = (TextView) itemView.findViewById(R.id.event_detail_time);
                title = (TextView) itemView.findViewById(R.id.event_detail_title);
                description = (TextView) itemView.findViewById(R.id.event_detail_description);
                eventUrl = (TextView) itemView.findViewById(R.id.event_link);
                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                Event Event = getItem(position);
                mItemListener.onEventClick(Event);

            }
        }
    }

    public interface EventItemListener {

        void onEventClick(Event clickedEvent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Log.d("EventsActivity", "This is the current email: " +
                        FirebaseAuth.getInstance().getCurrentUser().getEmail());
                Log.d("EventsActivity", "This is the current uid: " +
                        FirebaseAuth.getInstance().getCurrentUser().getUid());
                    if (!FirebaseAuth.getInstance().getCurrentUser().isAnonymous()) {
//                        AlphaAnimation animation1 = new AlphaAnimation(0, 1);
//                        animation1.setDuration(1000);
//                        animation1.setStartOffset(1000);
//                        animation1.setFillAfter(true);
//                        mAddEventFab.startAnimation(animation1);
//                        mAddEventFab.setVisibility(View.VISIBLE);
                }
            }
        }

    }
}
