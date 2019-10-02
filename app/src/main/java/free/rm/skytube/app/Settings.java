/*
 * SkyTube
 * Copyright (C) 2019 Zsombor Gegesy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package free.rm.skytube.app;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.StringRes;

import free.rm.skytube.R;
import free.rm.skytube.app.enums.Policy;

public class Settings {
    private final SkyTubeApp app;

    Settings(SkyTubeApp app) {
        this.app = app;
    }

    /**
     * Returns a localised string.
     *
     * @param stringResId String resource ID (e.g. R.string.my_string)
     * @return Localised string, from the strings XML file.
     */
    public String getStr(int stringResId) {
        return app.getString(stringResId);
    }

    public boolean isDownloadToSeparateFolders() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean(app.getStr(R.string.pref_key_download_to_separate_directories),false);
    }

    public boolean isDownloadToTemporaryFolder() {
        return PreferenceManager.getDefaultSharedPreferences(app).getBoolean(app.getStr(R.string.pref_key_download_to_temporary_directory),false);
    }

    public Policy getWarningMobilePolicy() {
        String currentValue = getPreference(R.string.pref_key_mobile_network_usage_policy, getStr(R.string.pref_mobile_network_usage_value_ask));
        return Policy.valueOf(currentValue.toUpperCase());
    }

    public void setWarningMobilePolicy(Policy warnPolicy) {
        setPreference(R.string.pref_key_mobile_network_usage_policy, warnPolicy.name().toLowerCase());
    }

    public void setDownloadFolder(String dir) {
        setPreference(R.string.pref_key_video_download_folder, dir);
    }

    public String getDownloadFolder(String defaultValue) {
        return getPreference(R.string.pref_key_video_download_folder, defaultValue);
    }

    private void setPreference(@StringRes int resId, String value) {
        final SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(app).edit();
        editor.putString(getStr(resId), value);
        editor.apply();
    }

    private String getPreference(@StringRes int resId, String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(app).getString(app.getStr(resId), defaultValue);
    }
}
