package com.grammatek.simaromur.db;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.grammatek.simaromur.App;

import java.util.List;


@Database(
        version = ApplicationDb.LATEST_VERSION,
        entities = {Voice.class, AppData.class},
        exportSchema = true
)
public abstract class ApplicationDb extends RoomDatabase {
    private final static String LOG_TAG = "Simaromur_" + ApplicationDb.class.getSimpleName();
    static final int LATEST_VERSION = 4;
    private static ApplicationDb INSTANCE;

    public abstract AppDataDao appDataDao();
    public abstract VoiceDao voiceDao();

    static public final Migration MIGRATION_1_2 = new Migration(1, 2){ // From version 1 to version 2
        @Override
        public void migrate(SupportSQLiteDatabase database){
            // Remove the table
            database.execSQL("DROP INDEX index_voice_table_name_gender_language_code_type");
            database.execSQL("DROP TABLE voice_table");
            database.execSQL("CREATE TABLE IF NOT EXISTS voice_table (`voiceId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `gender` TEXT NOT NULL, `internal_name` TEXT NOT NULL, `language_code` TEXT NOT NULL, `language_name` TEXT NOT NULL, `variant` TEXT NOT NULL, `type` TEXT, `update_time` TEXT, `download_time` TEXT, `url` TEXT, `download_path` TEXT, `version` TEXT, `md5_sum` TEXT, `local_size` INTEGER NOT NULL)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_voice_table_internal_name_gender_language_code_type` ON voice_table (`internal_name`, `gender`, `language_code`, `type`)");
        }
    };
    static public final Migration MIGRATION_2_3 = new Migration(2, 3){  // v2 => 3
        @Override
        public void migrate(SupportSQLiteDatabase database){
            database.execSQL("ALTER TABLE app_data_table "
                    + " ADD COLUMN privacy_info_dialog_accepted INTEGER NOT NULL DEFAULT(0)");
        }
    };
    static public final Migration MIGRATION_3_4 = new Migration(3, 4){  // v3 => 4
        @Override
        public void migrate(SupportSQLiteDatabase database){
            database.execSQL("ALTER TABLE app_data_table "
                    + " ADD COLUMN crash_lytics_user_consent_accepted INTEGER NOT NULL DEFAULT(0)");
        }
    };
    public static ApplicationDb getDatabase(final Context context) {
        Log.v(LOG_TAG, "getDatabase");
        if (INSTANCE == null) {
            synchronized (ApplicationDb.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ApplicationDb.class, "application_db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            // Wipes and rebuilds instead of migrating if no Migration object.
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .addCallback(sRoomDatabaseCallback)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Override the onOpen method to populate the database.
     */
    private static final RoomDatabase.Callback sRoomDatabaseCallback = new RoomDatabase.Callback(){

        @Override
        public void onOpen (@NonNull SupportSQLiteDatabase db){
            Log.v(LOG_TAG, "onOpen");
            super.onOpen(db);
            new PopulateDbAsync(INSTANCE).execute();
        }
    };

    /**
     * Populate the database with initial voices in the background.
     */
    private static class PopulateDbAsync extends AsyncTask<Void, Void, Void> {

        private final AppDataDao mAppDataDao;
        private final VoiceDao mVoiceDao;

        PopulateDbAsync(ApplicationDb db) {
            mAppDataDao = db.appDataDao();
            mVoiceDao = db.voiceDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {

            AppData appData = mAppDataDao.getAppData();
            if (appData == null) {
                appData = new AppData();
                appData.fliteVoiceListPath = App.getDataPath();
                appData.simVoiceListPath = App.getVoiceDataPath();
                mAppDataDao.insert(appData);
            }

            List<Voice> voices = mVoiceDao.getAnyVoices();
            if (voices == null || voices.isEmpty()) {
                Log.d(LOG_TAG, "PopulateDbAsync: no voices yet, network voices are updated async.");
            }
            return null;
        }
    }
}
