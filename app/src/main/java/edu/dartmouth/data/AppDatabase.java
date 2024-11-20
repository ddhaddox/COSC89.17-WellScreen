package edu.dartmouth.data;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import edu.dartmouth.data.dao.DailyAppUsageDao;
import edu.dartmouth.data.dao.DailyCategoryUsageDao;
import edu.dartmouth.data.dao.DailyScreenTimeDao;
import edu.dartmouth.data.dao.MPHQ9Dao;
import edu.dartmouth.data.dao.ScreenEventDao;
import edu.dartmouth.data.entities.DailyAppUsageEntity;
import edu.dartmouth.data.entities.DailyCategoryUsageEntity;
import edu.dartmouth.data.entities.DailyScreenTimeEntity;
import edu.dartmouth.data.entities.MPHQ9Entity;
import edu.dartmouth.data.entities.ScreenEventEntity;
import edu.dartmouth.KeyManager;
import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SupportFactory;

@Database(
        entities = {
                ScreenEventEntity.class,
                MPHQ9Entity.class,
                DailyAppUsageEntity.class,
                DailyCategoryUsageEntity.class,
                DailyScreenTimeEntity.class
        },
        version = 6,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    public abstract ScreenEventDao screenEventDao();

    public abstract MPHQ9Dao mphq9Dao();

    public abstract DailyAppUsageDao dailyAppUsageDao();

    public abstract DailyCategoryUsageDao dailyCategoryUsageDao();

    public abstract DailyScreenTimeDao dailyScreenTimeDao();

    private static volatile AppDatabase INSTANCE;
    private static final String TAG = "AppDatabase";

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    try {
                        // Initialize SQLCipher library
                        SQLiteDatabase.loadLibs(context);

                        // Generate or retrieve the encryption key
                        KeyManager.generateKey(context);
                        byte[] passphrase = KeyManager.getKey(context);
                        if (passphrase == null) {
                            throw new IllegalStateException("Failed to retrieve encryption key.");
                        }

                        SupportFactory factory = new SupportFactory(passphrase);

                        INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                        AppDatabase.class, "app_data.db")
                                .openHelperFactory(factory)
                                .fallbackToDestructiveMigration()
                                .build();

                        Log.d(TAG, "Encrypted AppDatabase instance created.");
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Error initializing database: ", e);
                        throw new RuntimeException("Error initializing database", e);
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error initializing database: ", e);
                        throw new RuntimeException("Unexpected error initializing database", e);
                    }
                }
            }
        }
        return INSTANCE;
    }
}