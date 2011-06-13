/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.offline.backup;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.offline.OfflineLog;

/**
 * Attribute change callback. Triggered by AttributeManager when interval is changed.
 *
 */
public class BackupIntervalCallback extends AttributeCallback {

    @SuppressWarnings("unchecked")
    @Override
    public void postModify(Map context, String attrName, Entry entry,
            boolean isCreate) {
        try {
            BackupTimer.updateInterval();
        } catch (ServiceException e) {
            OfflineLog.offline.error("Exception while updating backup interval",e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void preModify(Map context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry, boolean isCreate)
            throws ServiceException {
        //do nada
    }

}