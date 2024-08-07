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

package com.robypomper.comm.behaviours;

import com.robypomper.comm.peer.Peer;

import java.util.concurrent.CountDownLatch;

public class HeartBeatListener_Latch implements HeartBeatListener {


    public CountDownLatch onSend = new CountDownLatch(1);
    public CountDownLatch onSuccess = new CountDownLatch(1);
    public CountDownLatch onFail = new CountDownLatch(1);

    @Override
    public void onSend(Peer peer, HeartBeatConfigs hb) {
        onSend.countDown();
    }

    @Override
    public void onSuccess(Peer peer, HeartBeatConfigs hb) {
        onSuccess.countDown();
    }

    @Override
    public void onFail(Peer peer, HeartBeatConfigs hb) {
        onFail.countDown();
    }
}
