// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.AppManager.backup;

import android.system.ErrnoException;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import io.github.muntashirakon.AppManager.db.AppsDb;
import io.github.muntashirakon.AppManager.db.dao.BackupDao;
import io.github.muntashirakon.AppManager.db.entity.Backup;
import io.github.muntashirakon.AppManager.logcat.helper.SaveLogHelper;
import io.github.muntashirakon.io.Path;
import io.github.muntashirakon.io.UidGidPair;

public final class BackupUtils {
    @NonNull
    private static List<Path> getBackupPaths() {
        Path backupPath = BackupFiles.getBackupDirectory();
        List<Path> backupPaths;
        Path[] files = backupPath.listFiles(Path::isDirectory);
        backupPaths = new ArrayList<>(files.length);
        for (Path path : files) {
            if (SaveLogHelper.SAVED_LOGS_DIR.equals(path.getName())) {
                continue;
            }
            if (BackupFiles.APK_SAVING_DIRECTORY.equals(path.getName())) {
                continue;
            }
            if (BackupFiles.TEMPORARY_DIRECTORY.equals(path.getName())) {
                continue;
            }
            backupPaths.add(path);
        }
        // We don't need to check further at this stage.
        // It's the caller's job to check the contents if needed.
        return backupPaths;
    }

    @WorkerThread
    @NonNull
    public static HashMap<String, Backup> storeAllAndGetLatestBackupMetadata() {
        BackupDao backupDao = AppsDb.getInstance().backupDao();
        HashMap<String, Backup> backupMetadata = new HashMap<>();
        HashMap<String, List<MetadataManager.Metadata>> allBackupMetadata = getAllMetadata();
        List<Backup> backups = new ArrayList<>();
        for (List<MetadataManager.Metadata> metadataList : allBackupMetadata.values()) {
            if (metadataList.size() == 0) continue;
            Backup latestBackup = null;
            Backup backup;
            for (MetadataManager.Metadata metadata : metadataList) {
                backup = Backup.fromBackupMetadata(metadata);
                backups.add(backup);
                if (latestBackup == null || backup.backupTime > latestBackup.backupTime) {
                    latestBackup = backup;
                }
            }
            backupMetadata.put(latestBackup.packageName, latestBackup);
        }
        backupDao.deleteAll();
        backupDao.insert(backups);
        return backupMetadata;
    }

    @WorkerThread
    @Nullable
    @Deprecated
    public static Backup storeAllAndGetLatestBackupMetadata(String packageName) throws IOException {
        MetadataManager.Metadata[] allBackupMetadata = MetadataManager.getMetadata(packageName);
        List<Backup> backups = new ArrayList<>();
        Backup latestBackup = null;
        Backup backup;
        for (MetadataManager.Metadata metadata : allBackupMetadata) {
            backup = Backup.fromBackupMetadata(metadata);
            backups.add(backup);
            if (latestBackup == null || backup.backupTime > latestBackup.backupTime) {
                latestBackup = backup;
            }
        }
        AppsDb.getInstance().backupDao().insert(backups);
        return latestBackup;
    }

    @WorkerThread
    @NonNull
    public static HashMap<String, Backup> getAllLatestBackupMetadataFromDb() {
        HashMap<String, Backup> backupMetadata = new HashMap<>();
        List<Backup> backups = AppsDb.getInstance().backupDao().getAll();
        for (Backup backup : backups) {
            Backup latestBackup = backupMetadata.get(backup.packageName);
            if (latestBackup == null || backup.backupTime > latestBackup.backupTime) {
                backupMetadata.put(backup.packageName, backup);
            }
        }
        return backupMetadata;
    }

    /**
     * Retrieves all metadata for all packages
     */
    @WorkerThread
    @NonNull
    public static HashMap<String, List<MetadataManager.Metadata>> getAllMetadata() {
        HashMap<String, List<MetadataManager.Metadata>> backupMetadata = new HashMap<>();
        List<Path> backupFolderNames = getBackupPaths();
        for (Path backupFolderName : backupFolderNames) {
            MetadataManager.Metadata[] metadataList = MetadataManager.getMetadata(backupFolderName);
            for (MetadataManager.Metadata metadata : metadataList) {
                if (!backupMetadata.containsKey(metadata.packageName)) {
                    backupMetadata.put(metadata.packageName, new ArrayList<>());
                }
                //noinspection ConstantConditions
                backupMetadata.get(metadata.packageName).add(metadata);
            }
        }
        return backupMetadata;
    }

    @NonNull
    static Pair<Integer, Integer> getUidAndGid(Path filepath, int uid) {
        try {
            UidGidPair uidGidPair = Objects.requireNonNull(filepath.getFile()).getUidGid();
            return new Pair<>(uidGidPair.uid, uidGidPair.gid);
        } catch (ErrnoException e) {
            // Fallback to kernel user ID
            return new Pair<>(uid, uid);
        }
    }

    @Nullable
    public static String getShortBackupName(@NonNull String backupFileName) {
        if (TextUtils.isDigitsOnly(backupFileName)) {
            // It's already a user handle
            return null;
        } else {
            int firstUnderscore = backupFileName.indexOf('_');
            if (firstUnderscore != -1) {
                // Found an underscore
                String userHandle = backupFileName.substring(0, firstUnderscore);
                if (TextUtils.isDigitsOnly(userHandle)) {
                    // The new backup system
                    return backupFileName.substring(firstUnderscore + 1);
                }
            }
            // Could be the old naming style
            throw new IllegalArgumentException("Invalid backup name " + backupFileName);
        }
    }

    static int getUserHandleFromBackupName(@NonNull String backupFileName) {
        if (TextUtils.isDigitsOnly(backupFileName)) return Integer.parseInt(backupFileName);
        else {
            int firstUnderscore = backupFileName.indexOf('_');
            if (firstUnderscore != -1) {
                // Found an underscore
                String userHandle = backupFileName.substring(0, firstUnderscore);
                if (TextUtils.isDigitsOnly(userHandle)) {
                    // The new backup system
                    return Integer.parseInt(userHandle);
                }
            }
            throw new IllegalArgumentException("Invalid backup name");
        }
    }

    @NonNull
    static String[] getExcludeDirs(boolean includeCache, @Nullable String[] others) {
        // Lib dirs has to be ignored by default
        List<String> excludeDirs = new ArrayList<>(Arrays.asList(BackupManager.LIB_DIR));
        if (includeCache) {
            excludeDirs.addAll(Arrays.asList(BackupManager.CACHE_DIRS));
        }
        if (others != null) {
            excludeDirs.addAll(Arrays.asList(others));
        }
        return excludeDirs.toArray(new String[0]);
    }
}
