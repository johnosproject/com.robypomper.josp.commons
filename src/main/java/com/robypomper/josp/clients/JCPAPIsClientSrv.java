/*******************************************************************************
 * The John Operating System Project is the collection of software and configurations
 * to generate IoT EcoSystem, like the John Operating System Platform one.
 * Copyright (C) 2021 Roberto Pompermaier
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

package com.robypomper.josp.clients;

import com.robypomper.josp.consts.JOSPConstants;
import com.robypomper.josp.states.StateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SuppressWarnings("unused")
public abstract class JCPAPIsClientSrv extends DefaultJCPClient2 implements JCPClient2.ConnectionListener {

    // Internal vars

    private static final Logger log = LoggerFactory.getLogger(JCPAPIsClientSrv.class);
    public static final String JCP_NAME = "JCP APIs";


    // Constructor

    public JCPAPIsClientSrv(boolean useSSL, String client, String secret, String urlAPIs, String urlAuth, String callBack, String authCodeRefreshToken, int connectionRetrySeconds) throws AuthenticationException, StateException {
        super(client, secret, urlAPIs, useSSL, urlAuth, "openid offline_access", callBack, "jcp", authCodeRefreshToken, connectionRetrySeconds, JCP_NAME);
        addConnectionListener(this);

        connect();
    }


    // Headers default values setters

    public void setServiceId(String srvId) {
        if (srvId != null && !srvId.isEmpty())
            addDefaultHeader(JOSPConstants.API_HEADER_SRV_ID, srvId);
        else
            removeDefaultHeader(JOSPConstants.API_HEADER_SRV_ID);
    }

    public void setUserId(String usrId) {
        if (usrId != null && !usrId.isEmpty())
            addDefaultHeader(JOSPConstants.API_HEADER_USR_ID, usrId);
        else
            removeDefaultHeader(JOSPConstants.API_HEADER_USR_ID);
    }

    public void setLoginCodeAndReconnect(String loginCode) throws StateException, AuthenticationException {
        disconnect();
        setLoginCode(loginCode);
        connect();
    }


    // Self-Connection observer

    @Override
    public void onConnected(JCPClient2 jcpClient) {
        log.info(String.format("%s connected", JCP_NAME));
        storeTokens();
    }

    @Override
    public void onConnectionFailed(JCPClient2 jcpClient, Throwable t) {
        if (t instanceof JCPNotReachableException)
            if (isConnecting())
                log.trace(String.format("Can't connect to JCP APIs at '%s', retry later", jcpClient.getAPIsUrl()));
            else
                log.debug(String.format("Can't connect to JCP APIs at '%s', retry later", jcpClient.getAPIsUrl()));
        else
            log.debug(String.format("Error on JCP APIs connection attempt at '%s'", jcpClient.getAPIsUrl()), t);
    }

    @Override
    public void onAuthenticationFailed(JCPClient2 jcpClient, Throwable t) {
        log.debug(String.format("Error on %s connection authentication because %s", getApiName(), t.getMessage()));
    }

    @Override
    public void onDisconnected(JCPClient2 jcpClient) {
        log.debug(String.format("%s disconnected", JCP_NAME));
    }

    protected abstract void storeTokens();

}
