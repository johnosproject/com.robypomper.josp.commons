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

public interface HeartBeatConfigs {

    // Class constants

    int TIMEOUT_MS = 30 * 1000;
    int TIMEOUT_HB_MS = TIMEOUT_MS - calculateMinDiff(TIMEOUT_MS);
    boolean ENABLE_HB_RES = true;


    // Getter/setters

    int getTimeout();

    void setTimeout(int ms);

    int getHBTimeout();

    void setHBTimeout(int ms);

    boolean isHBResponseEnabled();

    void enableHBResponse(boolean enabled);

    static int calculateMinDiff(int timeoutMs) {
        return timeoutMs / 10;
    }


    // Listener

    void addListener(HeartBeatListener listener);

    void removeListener(HeartBeatListener listener);

}
