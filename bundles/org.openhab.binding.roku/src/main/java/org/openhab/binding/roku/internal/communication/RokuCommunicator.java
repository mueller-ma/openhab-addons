/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.roku.internal.communication;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.binding.roku.internal.RokuHttpException;
import org.openhab.binding.roku.internal.dto.ActiveApp;
import org.openhab.binding.roku.internal.dto.Apps;
import org.openhab.binding.roku.internal.dto.Apps.App;
import org.openhab.binding.roku.internal.dto.DeviceInfo;
import org.openhab.binding.roku.internal.dto.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Methods for accessing the HTTP interface of the Roku
 *
 * @author Michael Lobstein - Initial contribution
 */
@NonNullByDefault
public class RokuCommunicator {
    private final Logger logger = LoggerFactory.getLogger(RokuCommunicator.class);
    private final HttpClient httpClient;

    private final String urlKeyPress;
    private final String urlLaunchApp;
    private final String urlQryDevice;
    private final String urlQryActiveApp;
    private final String urlQryApps;
    private final String urlQryPlayer;

    public RokuCommunicator(HttpClient httpClient, String host, int port) {
        this.httpClient = httpClient;

        final String baseUrl = "http://" + host + ":" + port;
        urlKeyPress = baseUrl + "/keypress/";
        urlLaunchApp = baseUrl + "/launch/";
        urlQryDevice = baseUrl + "/query/device-info";
        urlQryActiveApp = baseUrl + "/query/active-app";
        urlQryApps = baseUrl + "/query/apps";
        urlQryPlayer = baseUrl + "/query/media-player";
    }

    /**
     * Send a keypress command to the Roku
     *
     * @param key The key code to send
     *
     */
    public void keyPress(String key) throws RokuHttpException {
        postCommand(urlKeyPress + key);
    }

    /**
     * Send a launch app command to the Roku
     *
     * @param appId The appId of the app to launch
     *
     */
    public void launchApp(String appId) throws RokuHttpException {
        postCommand(urlLaunchApp + appId);
    }

    /**
     * Send a command to get device-info from the Roku and return a DeviceInfo object
     *
     * @return A DeviceInfo object populated with information about the connected Roku
     * @throws RokuHttpException
     */
    public DeviceInfo getDeviceInfo() throws RokuHttpException {
        try {
            JAXBContext ctx = JAXBUtils.JAXBCONTEXT_DEVICE_INFO;
            if (ctx != null) {
                Unmarshaller unmarshaller = ctx.createUnmarshaller();
                if (unmarshaller != null) {
                    DeviceInfo device = (DeviceInfo) unmarshaller.unmarshal(new StringReader(getCommand(urlQryDevice)));
                    if (device != null) {
                        return device;
                    }
                }
            }
            throw new RokuHttpException("No DeviceInfo model in response");
        } catch (JAXBException e) {
            throw new RokuHttpException("Exception creating DeviceInfo Unmarshaller: " + e.getLocalizedMessage());
        }
    }

    /**
     * Send a command to get active-app from the Roku and return an ActiveApp object
     *
     * @return An ActiveApp object populated with information about the current running app on the Roku
     * @throws RokuHttpException
     */
    public ActiveApp getActiveApp() throws RokuHttpException {
        try {
            JAXBContext ctx = JAXBUtils.JAXBCONTEXT_ACTIVE_APP;
            if (ctx != null) {
                Unmarshaller unmarshaller = ctx.createUnmarshaller();
                if (unmarshaller != null) {
                    ActiveApp activeApp = (ActiveApp) unmarshaller
                            .unmarshal(new StringReader(getCommand(urlQryActiveApp)));
                    if (activeApp != null) {
                        return activeApp;
                    }
                }
            }
            throw new RokuHttpException("No ActiveApp model in response");
        } catch (JAXBException e) {
            throw new RokuHttpException("Exception creating ActiveApp Unmarshaller: " + e.getLocalizedMessage());
        }
    }

    /**
     * Send a command to get the installed app list from the Roku and return a List of App objects
     *
     * @return A List of App objects for all apps currently installed on the Roku
     * @throws RokuHttpException
     */
    public List<App> getAppList() throws RokuHttpException {
        try {
            JAXBContext ctx = JAXBUtils.JAXBCONTEXT_APPS;
            if (ctx != null) {
                Unmarshaller unmarshaller = ctx.createUnmarshaller();
                if (unmarshaller != null) {
                    Apps appList = (Apps) unmarshaller.unmarshal(new StringReader(getCommand(urlQryApps)));
                    if (appList != null) {
                        return appList.getApp();
                    }
                }
            }
            throw new RokuHttpException("No AppList model in response");
        } catch (JAXBException e) {
            throw new RokuHttpException("Exception creating AppList Unmarshaller: " + e.getLocalizedMessage());
        }
    }

    /**
     * Send a command to get media-player from the Roku and return a Player object
     *
     * @return A Player object populated with information about the current stream playing on the Roku
     * @throws RokuHttpException
     */
    public Player getPlayerInfo() throws RokuHttpException {
        try {
            JAXBContext ctx = JAXBUtils.JAXBCONTEXT_PLAYER;
            if (ctx != null) {
                Unmarshaller unmarshaller = ctx.createUnmarshaller();
                if (unmarshaller != null) {
                    Player playerInfo = (Player) unmarshaller.unmarshal(new StringReader(getCommand(urlQryPlayer)));
                    if (playerInfo != null) {
                        return playerInfo;
                    }
                }
            }
            throw new RokuHttpException("No Player info model in response");
        } catch (JAXBException e) {
            throw new RokuHttpException("Exception creating Player info Unmarshaller: " + e.getLocalizedMessage());
        }
    }

    /**
     * Sends a GET command to the Roku
     *
     * @param url The url to send with the command embedded in the URI
     * @return The response content of the http request
     */
    private String getCommand(String url) {
        try {
            return httpClient.GET(url).getContentAsString();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.debug("Error executing player GET command, URL: {}, {} ", url, e.getMessage());
            return "";
        }
    }

    /**
     * Sends a POST command to the Roku
     *
     * @param url The url to send with the command embedded in the URI
     * @throws RokuHttpException
     */
    private void postCommand(String url) throws RokuHttpException {
        try {
            httpClient.POST(url).method(HttpMethod.POST).send();
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            throw new RokuHttpException("Error executing player POST command, URL: " + url + e.getMessage());
        }
    }
}
