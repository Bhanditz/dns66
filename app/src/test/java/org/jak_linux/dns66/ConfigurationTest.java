package org.jak_linux.dns66;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.NonNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Created by jak on 07/04/17.
 */
public class ConfigurationTest {

    private Configuration.Item newItemForLocation(String location) {
        Configuration.Item item = new Configuration.Item();
        item.location = location;
        return item;
    }

    @Test
    public void testIsDownloadable() {
        try {
            newItemForLocation(null).isDownloadable();
            fail("Was null");
        } catch (NullPointerException e) {
            // OK
        }

        assertTrue("http:// URI downloadable", newItemForLocation("http://example.com").isDownloadable());
        assertTrue("https:// URI downloadable", newItemForLocation("https://example.com").isDownloadable());
        assertFalse("file:// URI downloadable", newItemForLocation("file://example.com").isDownloadable());
        assertFalse("file:// URI downloadable", newItemForLocation("file:/example.com").isDownloadable());
        assertFalse("https domain not downloadable", newItemForLocation("https.example.com").isDownloadable());
        assertFalse("http domain not downloadable", newItemForLocation("http.example.com").isDownloadable());
    }

    @Test
    public void testResolve() throws Exception {
        Configuration.Whitelist wl = new Configuration.Whitelist() {
            @Override
            Intent newBrowserIntent() {
                return mock(Intent.class);
            }
        };

        List<ResolveInfo> resolveInfoList = new ArrayList<>();
        List<ApplicationInfo> applicationInfoList = new ArrayList<>();

        // Web browsers
        resolveInfoList.add(newResolveInfo("system-browser", 0));
        applicationInfoList.add(newApplicationInfo("system-browser", ApplicationInfo.FLAG_SYSTEM));
        resolveInfoList.add(newResolveInfo("data-browser", 0));
        applicationInfoList.add(newApplicationInfo("data-browser", 0));

        // Not a browser
        applicationInfoList.add(newApplicationInfo("system-app", ApplicationInfo.FLAG_SYSTEM));
        applicationInfoList.add(newApplicationInfo("data-app", 0));

        // This app
        applicationInfoList.add(newApplicationInfo(BuildConfig.APPLICATION_ID, 0));

        PackageManager pm = mock(PackageManager.class);
        //noinspection WrongConstant
        when(pm.queryIntentActivities(any(Intent.class), anyInt())).thenReturn(resolveInfoList);
        //noinspection WrongConstant
        when(pm.getInstalledApplications(anyInt())).thenReturn(applicationInfoList);

        Set<String> onVpn = new HashSet<>();
        Set<String> notOnVpn = new HashSet<>();

        wl.defaultMode = Configuration.Whitelist.DEFAULT_MODE_NOT_ON_VPN;
        wl.resolve(pm, onVpn, notOnVpn);

        assertTrue(onVpn.contains(BuildConfig.APPLICATION_ID));
        assertTrue(notOnVpn.contains("system-app"));
        assertTrue(notOnVpn.contains("data-app"));
        assertTrue(notOnVpn.contains("system-browser"));
        assertTrue(notOnVpn.contains("data-browser"));

        // Default allow on vpn
        onVpn.clear();
        notOnVpn.clear();
        wl.defaultMode = Configuration.Whitelist.DEFAULT_MODE_ON_VPN;
        wl.resolve(pm, onVpn, notOnVpn);

        assertTrue(onVpn.contains(BuildConfig.APPLICATION_ID));
        assertTrue(onVpn.contains("system-app"));
        assertTrue(onVpn.contains("data-app"));
        assertTrue(onVpn.contains("system-browser"));
        assertTrue(onVpn.contains("data-browser"));

        // Default intelligent on vpn
        onVpn.clear();
        notOnVpn.clear();
        wl.defaultMode = Configuration.Whitelist.DEFAULT_MODE_INTELLIGENT;
        wl.resolve(pm, onVpn, notOnVpn);

        assertTrue(onVpn.contains(BuildConfig.APPLICATION_ID));
        assertTrue(notOnVpn.contains("system-app"));
        assertTrue(onVpn.contains("data-app"));
        assertTrue(onVpn.contains("system-browser"));
        assertTrue(onVpn.contains("data-browser"));

        // Default intelligent on vpn
        onVpn.clear();
        notOnVpn.clear();
        wl.items.clear();
        wl.itemsOnVpn.clear();
        wl.items.add(BuildConfig.APPLICATION_ID);
        wl.items.add("system-browser");
        wl.defaultMode = Configuration.Whitelist.DEFAULT_MODE_INTELLIGENT;
        wl.resolve(pm, onVpn, notOnVpn);
        assertTrue(onVpn.contains(BuildConfig.APPLICATION_ID));
        assertTrue(notOnVpn.contains("system-browser"));

        // Check that blacklisting works
        onVpn.clear();
        notOnVpn.clear();
        wl.items.clear();
        wl.itemsOnVpn.clear();
        wl.itemsOnVpn.add("data-app");
        wl.defaultMode = Configuration.Whitelist.DEFAULT_MODE_NOT_ON_VPN;
        wl.resolve(pm, onVpn, notOnVpn);
        assertTrue(onVpn.contains("data-app"));
    }

    @NonNull
    private ResolveInfo newResolveInfo(String name, int flags) {
        ResolveInfo resolveInfo = mock(ResolveInfo.class);
        ActivityInfo activityInfo = mock(ActivityInfo.class);
        activityInfo.packageName = name;
        resolveInfo.activityInfo = activityInfo;
        resolveInfo.activityInfo.flags = flags;
        return resolveInfo;
    }

    @NonNull
    private ApplicationInfo newApplicationInfo(String name, int flags) {
        ApplicationInfo applicationInfo = mock(ApplicationInfo.class);
        applicationInfo.packageName = name;
        applicationInfo.flags = flags;
        return applicationInfo;
    }

}