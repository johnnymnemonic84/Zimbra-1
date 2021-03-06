/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package org.jivesoftware.wildfire.filetransfer;

import org.jivesoftware.util.IMConfig;
import org.jivesoftware.util.Cache;
import org.jivesoftware.wildfire.auth.UnauthorizedException;
import org.jivesoftware.wildfire.container.BasicModule;
import org.jivesoftware.wildfire.interceptor.InterceptorManager;
import org.jivesoftware.wildfire.interceptor.PacketInterceptor;
import org.jivesoftware.wildfire.interceptor.PacketRejectedException;
import org.jivesoftware.wildfire.Session;
import org.jivesoftware.wildfire.filetransfer.proxy.ProxyConnectionManager;
import org.jivesoftware.wildfire.filetransfer.proxy.ProxyTransfer;
import org.dom4j.Element;
import org.xmpp.packet.Packet;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Provides several utility methods for file transfer manager implementaions to utilize.
 *
 * @author Alexander Wenckus
 */
public class DefaultFileTransferManager extends BasicModule implements FileTransferManager {

    private static final String CACHE_NAME = "File Transfer Cache";

    private final Map<String, FileTransfer> fileTransferMap;

    private final List<FileTransferInterceptor> fileTransferInterceptorList
            = new ArrayList<FileTransferInterceptor>();

    /**
     * Default constructor creates the cache.
     */
    public DefaultFileTransferManager() {
        super("File Transfer Manager");
        fileTransferMap = createCache(CACHE_NAME, "fileTransfer", 128 * 1024, 1000 * 60 * 10);
        InterceptorManager.getInstance().addInterceptor(new MetaFileTransferInterceptor());
    }

    private Cache<String, FileTransfer> createCache(String name, String propertiesName, int size,
                                                    long expirationTime)
    {
        size = IMConfig.getCacheSize(propertiesName, size);
        expirationTime = IMConfig.getCacheExpirationTime(propertiesName, (int)expirationTime);
        return new Cache<String, FileTransfer>(name, size, expirationTime);
    }

    /**
     * Returns true if the proxy transfer should be matched to an existing file transfer
     * in the system.
     *
     * @return Returns true if the proxy transfer should be matched to an existing file
     * transfer in the system.
     */
    public boolean isMatchProxyTransfer() {
        return IMConfig.XMPP_PROXY_TRANSFER_REQUIRED.getBoolean();
    }

    protected void cacheFileTransfer(String key, FileTransfer transfer) {
        fileTransferMap.put(key, transfer);
    }

    protected FileTransfer retrieveFileTransfer(String key) {
        return fileTransferMap.get(key);
    }

    protected static Element getChildElement(Element element, String namespace) {
        List elements = element.elements();
        if (elements.isEmpty()) {
            return null;
        }
        for (int i = 0; i < elements.size(); i++) {
            Element childElement = (Element) elements.get(i);
            String childNamespace = childElement.getNamespaceURI();
            if (namespace.equals(childNamespace)) {
                return childElement;
            }
        }

        return null;
    }

    public boolean acceptIncomingFileTransferRequest(FileTransfer transfer)
            throws FileTransferRejectedException
    {
        fireFileTransferIntercept(transfer, false);
        if(transfer != null) {
            String streamID = transfer.getSessionID();
            JID from = new JID(transfer.getInitiator());
            JID to = new JID(transfer.getTarget());
            cacheFileTransfer(ProxyConnectionManager.createDigest(streamID, from, to), transfer);
            return true;
        }
        return false;
    }

    public void registerProxyTransfer(String transferDigest, ProxyTransfer proxyTransfer)
            throws UnauthorizedException
    {
        FileTransfer transfer = retrieveFileTransfer(transferDigest);
        if (isMatchProxyTransfer() && transfer == null) {
            throw new UnauthorizedException("Unable to match proxy transfer with a file transfer");
        }
        else if (transfer == null) {
            return;
        }

        transfer.setProgress(proxyTransfer);
        cacheFileTransfer(transferDigest, transfer);
    }

    private FileTransfer createFileTransfer(JID from,
                                            JID to, Element siElement) {
        String streamID = siElement.attributeValue("id");
        String mimeType = siElement.attributeValue("mime-type");
        String profile = siElement.attributeValue("profile");
        // Check profile, the only type we deal with currently is file transfer
        FileTransfer transfer = null;
        if (NAMESPACE_SI_FILETRANSFER.equals(profile)) {
            Element fileTransferElement = getChildElement(siElement, NAMESPACE_SI_FILETRANSFER);
            // Not valid form, reject
            if (fileTransferElement == null) {
                return null;
            }
            String fileName = fileTransferElement.attributeValue("name");
            long size = Long.parseLong(fileTransferElement.attributeValue("size"));

            transfer = new FileTransfer(from.toString(), to.toString(),
                    streamID, fileName, size, mimeType);
        }
        return transfer;
    }

    public void addFileTransferInterceptor(FileTransferInterceptor interceptor) {
        fileTransferInterceptorList.add(interceptor);
    }

    public void removeFileTransferInterceptor(FileTransferInterceptor interceptor) {
        fileTransferInterceptorList.remove(interceptor);
    }

    public void fireFileTransferIntercept(FileTransferProgress transfer, boolean isReady)
            throws FileTransferRejectedException
    {
        fireFileTransferIntercept(fileTransferMap.get(transfer.getSessionID()), isReady);
    }

    private void fireFileTransferIntercept(FileTransfer transfer, boolean isReady)
            throws FileTransferRejectedException
    {
        for(FileTransferInterceptor interceptor : fileTransferInterceptorList) {
            interceptor.interceptFileTransfer(transfer, isReady);
        }
    }

    /**
     * Interceptor to grab and validate file transfer meta information.
     */
    private class MetaFileTransferInterceptor implements PacketInterceptor {
        public void interceptPacket(Packet packet, Session session, boolean incoming,
                                    boolean processed)
                throws PacketRejectedException
        {
            // We only want packets recieved by the server
            if (!processed && incoming && packet instanceof IQ) {
                IQ iq = (IQ) packet;
                Element childElement = iq.getChildElement();
                if(childElement == null) {
                    return;
                }

                String namespace = childElement.getNamespaceURI();
                if (NAMESPACE_SI.equals(namespace)) {
                    // If this is a set, check the feature offer
                    if (iq.getType().equals(IQ.Type.set)) {
                        JID from = iq.getFrom();
                        JID to = iq.getTo();

                        FileTransfer transfer =
                                createFileTransfer(from, to, childElement);

                        try {
                            if (transfer == null || !acceptIncomingFileTransferRequest(transfer)) {
                                throw new PacketRejectedException();
                            }
                        }
                        catch (FileTransferRejectedException e) {
                            throw new PacketRejectedException(e);
                        }
                    }
                }
            }
        }
    }
}
