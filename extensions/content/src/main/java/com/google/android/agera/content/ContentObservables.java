/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.agera.content;

import static android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import static com.google.android.agera.Observables.updateDispatcher;
import static com.google.android.agera.Preconditions.checkNotNull;

import com.google.android.agera.ActivationHandler;
import com.google.android.agera.Observable;
import com.google.android.agera.Updatable;
import com.google.android.agera.UpdateDispatcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility methods for obtaining {@link Observable} instances.
 *
 * <p>Any {@link Observable} created by this class have to be created from a {@link Looper} thread
 * or they will throw an {@link IllegalStateException}
 */
public final class ContentObservables {

  /**
   * Returns an {@link Observable} that notifies added {@link Updatable}s that the input
   * {@code actions} have been received through the Android broadcast mechanism.
   *
   * <p>Since {@link ActivationHandler#observableDeactivated(UpdateDispatcher)} is called
   * asynchronously, using an activity context here will cause {@link android.os.StrictMode} to
   * report leaked registration objects. This can be avoided using the application context instead.
   *
   * @param context context used to register the receiver
   */
  @NonNull
  public static Observable broadcastObservable(@NonNull final Context context,
      @NonNull final String... actions) {
    return new BroadcastObservable(context, actions);
  }

  /**
   * Returns an {@link Observable} that notifies added {@link Updatable}s that
   * {@code keys} in {@link SharedPreferences} has been changed.
   */
  @NonNull
  public static Observable sharedPreferencesObservable(@NonNull final SharedPreferences preferences,
      @NonNull final String... keys) {
    return new SharedPreferencesObservable(preferences, keys);
  }

  private static final class BroadcastObservable extends BroadcastReceiver
      implements ActivationHandler, Observable {
    @NonNull
    private final UpdateDispatcher updateDispatcher;
    @NonNull
    private final Context context;
    @NonNull
    private final IntentFilter filter;

    BroadcastObservable(@NonNull final Context applicationContext,
        @NonNull final String... actions) {
      this.context = checkNotNull(applicationContext);
      this.updateDispatcher = updateDispatcher(this);
      this.filter = new IntentFilter();
      for (final String action : actions) {
        this.filter.addAction(action);
      }
    }

    @Override
    public void observableActivated(@NonNull final UpdateDispatcher caller) {
      context.registerReceiver(this, filter);
    }

    @Override
    public void observableDeactivated(@NonNull final UpdateDispatcher caller) {
      context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
      updateDispatcher.update();
    }

    @Override
    public void addUpdatable(@NonNull final Updatable updatable) {
      updateDispatcher.addUpdatable(updatable);
    }

    @Override
    public void removeUpdatable(@NonNull final Updatable updatable) {
      updateDispatcher.removeUpdatable(updatable);
    }
  }

  private static final class SharedPreferencesObservable implements
      OnSharedPreferenceChangeListener, Observable, ActivationHandler {
    @NonNull
    private final UpdateDispatcher updateDispatcher;
    @NonNull
    private final SharedPreferences preferences;
    @NonNull
    private final Set<String> keys;

    SharedPreferencesObservable(@NonNull final SharedPreferences preferences,
        @NonNull final String... keys) {
      this.keys = new HashSet<>(Arrays.asList(keys));
      this.preferences = checkNotNull(preferences);
      this.updateDispatcher = updateDispatcher(this);
    }

    @Override
    public void observableActivated(@NonNull final UpdateDispatcher caller) {
      preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void observableDeactivated(@NonNull final UpdateDispatcher caller) {
      preferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void addUpdatable(@NonNull final Updatable updatable) {
      updateDispatcher.addUpdatable(updatable);
    }

    @Override
    public void removeUpdatable(@NonNull final Updatable updatable) {
      updateDispatcher.removeUpdatable(updatable);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences,
        final String key) {
      if (keys.contains(key)) {
        updateDispatcher.update();
      }
    }
  }

  private ContentObservables() {}
}
