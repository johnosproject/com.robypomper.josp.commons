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

package com.robypomper.josp.states;

/**
 * JSL state representations.
 */
public enum JSLState {

    /**
     * JSL library instance is started and operative.
     */
    RUN,

    /**
     * JSL library instance is starting, when finish the status become
     * {@link #RUN} or {@link #STOP} if error occurs.
     */
    STARTING,

    /**
     * JSL library instance is stopped.
     */
    STOP,

    /**
     * JSL library instance is disconnecting, when finish the status
     * become {@link #STOP}.
     */
    SHOUTING,

    /**
     * JSL library instance is shouting down and startup, when finish the status
     * become {@link #RUN} or {@link #STOP} if error occurs.
     */
    RESTARTING;

}
