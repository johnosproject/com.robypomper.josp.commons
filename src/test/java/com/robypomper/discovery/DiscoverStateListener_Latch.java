/*******************************************************************************
 * The John Operating System Project is the collection of software and configurations
 * to generate IoT EcoSystem, like the John Operating System Platform one.
 * Copyright (C) 2024 Roberto Pompermaier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/

package com.robypomper.discovery;

import com.robypomper.discovery.Discover;
import com.robypomper.discovery.DiscoverStateListener;

import java.util.concurrent.CountDownLatch;

public class DiscoverStateListener_Latch implements DiscoverStateListener {

    public CountDownLatch onStart = new CountDownLatch(1);
    public CountDownLatch onStop = new CountDownLatch(1);
    public CountDownLatch onFail = new CountDownLatch(1);

    @Override
    public void onStart(Discover discover) {
        onStart.countDown();
    }

    @Override
    public void onStop(Discover discover) {
        onStop.countDown();
    }

    @Override
    public void onFail(Discover discover, String failMsg, Throwable exception) {
        onFail.countDown();
    }

}
