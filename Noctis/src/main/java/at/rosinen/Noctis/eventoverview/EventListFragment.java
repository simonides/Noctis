package at.rosinen.Noctis.eventoverview;

import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import at.rosinen.Noctis.R;
import at.rosinen.Noctis.activity.event.ShowDetailsEvent;
import at.rosinen.Noctis.activity.event.ToastMeEvent;
import at.rosinen.Noctis.base.EventBusFragment;
import at.rosinen.Noctis.location.event.FoundLocationEvent;
import at.rosinen.Noctis.location.event.RequestLocationEvent;
import at.rosinen.Noctis.map.MapEventBus;
import at.rosinen.Noctis.map.event.MarkEventsOnMapEvent;
import at.rosinen.Noctis.noctisevents.event.ImageDownloadAvailableEvent;
import at.rosinen.Noctis.noctisevents.event.NoctisEventsAvailableEvent;
import at.rosinen.Noctis.noctisevents.event.RequestEventsEvent;
import org.androidannotations.annotations.*;

/**
 * Created by Harald on 20.03.2015.
 * Edited by Simon on 31.03.2015
 */
@EFragment(R.layout.event_list_fragment)
public class EventListFragment extends EventBusFragment {

    private static final String TAG = EventListFragment.class.getName();

    private static final int DEFAULT_RADIUS = 100; //TODO check this value -> maybe set in sharedpreffile -> option menu

    @ViewById(R.id.eventListView)
    ListView list;

    @ViewById
    SwipeRefreshLayout eventListRefresher;

    @Bean
    NoctisEventAdapter adapter;

    @ViewById
    RelativeLayout emptyIndicator;

    @Bean
    MapEventBus mapEventBus;

    /**
     * set day with builder
     * EventListFragment_.build()...
     * depending on this value new events are fetched
     * most likely the viewpager index will suffice so this is gonna be used for now
     * else an extra event to the fragmentlist would have to be dispatched.
     */
    @FragmentArg
    public int day;


    @AfterViews
    void bindAdapter() {
        list.setAdapter(adapter);
        eventListRefresher.setOnRefreshListener(new EventRefreshListener());

        list.setEmptyView(emptyIndicator);
        // workaround to show the loading circle
//        eventListRefresher.setProgressViewOffset(false, 0,
//                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));
//        eventListRefresher.setRefreshing(true);

    }

    @ItemClick(R.id.eventListView)
    void itemClicked(int position) {
//        adapter.getItem(position);
        //TODO change call of showdetailevent to supply the event directly else nullpointer can occur because of to late calls to the list
        ShowDetailsEvent detailsEvent = new ShowDetailsEvent(adapter.getNoctisEventList(), position);
        mEventBus.post(detailsEvent);
    }


    /**
     * called by the location service
     *  will be called by the map to i think and also
     *  when a text field to enter a city name
     *
     * issued when a new Location update has to be delivered
     *
     * requests immediatly the events for that location
     * @param foundLocationEvent
     */
    public void onEvent(final FoundLocationEvent foundLocationEvent) {
        mEventBus.post(new RequestEventsEvent(foundLocationEvent.coordinate, DEFAULT_RADIUS, day));
        eventListRefresher.setRefreshing(true);
    }

    /**
     * called from Eventservice
     * check if the events are for that fragment and if so display them
     * @param noctisEventsAvailableEvent
     */
    public void onEventMainThread(final NoctisEventsAvailableEvent noctisEventsAvailableEvent) {
        if (noctisEventsAvailableEvent.requestEventsEvent.day != day) {
            return;
        }
        adapter.getNoctisEventList().clear();
        adapter.getNoctisEventList().addAll(noctisEventsAvailableEvent.eventList);
//        adapter.setNoctisEventList(noctisEventsAvailableEvent.eventList); //TODO check whether the one or the other is better
        adapter.notifyDataSetChanged();
        eventListRefresher.setRefreshing(false);

        mapEventBus.getEventBus().post(new MarkEventsOnMapEvent(adapter.getNoctisEventList()));

    }

    /**
     * called from Imageservice
     *  it basically only has to notify the adapter that the data has changed
     * @param imgDownloadAvailableEvent
     */
    public void onEventMainThread(final ImageDownloadAvailableEvent imgDownloadAvailableEvent){
        if (imgDownloadAvailableEvent.day != day) {
            return;
        }
        Log.i(TAG, "image download recieved, updating adapter");
        adapter.notifyDataSetChanged();
    }


    /**
     * OnRefreshListener for NoctisEventListFragment
     *
     * fires a RequestEventsEvent if there is already a location
     *  if not a ToastMeEvent is fired which says that there is no location available
     */
    private class EventRefreshListener implements SwipeRefreshLayout.OnRefreshListener {
        @Override
        public void onRefresh() {
            FoundLocationEvent foundLocationEvent = mEventBus.getStickyEvent(FoundLocationEvent.class);
            if (foundLocationEvent != null) {
                mEventBus.post(new RequestEventsEvent(foundLocationEvent.coordinate, DEFAULT_RADIUS, day));
            } else {
                mEventBus.post(new RequestLocationEvent());
                mEventBus.post(new ToastMeEvent(getString(R.string.needLocationFirst), Toast.LENGTH_LONG));
                mEventBus.post(new ToastMeEvent(getString(R.string.locationUpdateRequested), Toast.LENGTH_LONG));
            }
        }
    }

    /**
     * set the day after construction!
     * it is needed for the RequestEventEvents
     * @param day
     */
    public void setDay(int day) {
        this.day = day;
    }




}

