package edu.dartmouth.collectors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;

import edu.dartmouth.data.entities.ScreenEventEntity;
import edu.dartmouth.repositories.ScreenEventRepository;

public class ScreenStateReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ScreenEventRepository repository = new ScreenEventRepository(context);
        ScreenEventEntity entity = new ScreenEventEntity();

        // Compute the start of the day timestamp for consistency
        entity.timestamp = getStartOfDay(System.currentTimeMillis());

        if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
            entity.isScreenOn = true;
        } else if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
            entity.isScreenOn = false;
        }

        repository.insert(entity);
    }

    private long getStartOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        // Reset the time to midnight
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}
