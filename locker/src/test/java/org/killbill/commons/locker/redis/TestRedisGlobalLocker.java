/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.commons.locker.redis;

import java.io.IOException;
import java.util.UUID;

import org.killbill.commons.locker.GlobalLock;
import org.killbill.commons.locker.GlobalLocker;
import org.killbill.commons.locker.LockFailedException;
import org.killbill.commons.request.Request;
import org.killbill.commons.request.RequestData;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestRedisGlobalLocker {

    private GlobalLocker locker;

    @BeforeMethod(groups = "redis")
    public void beforeMethod() throws Exception {
        locker = new RedisGlobalLocker("redis://127.0.0.1:6379");
        Request.resetPerThreadRequestData();
    }

    @AfterMethod(groups = "redis")
    public void afterMethod() throws Exception {
        Request.resetPerThreadRequestData();
    }

    @Test(groups = "redis")
    public void testSimpleLocking() throws IOException, LockFailedException {
        final String serviceLock = "MY_AWESOME_LOCK";
        final String lockName = UUID.randomUUID().toString();

        final GlobalLock lock = locker.lockWithNumberOfTries(serviceLock, lockName, 3);
        Assert.assertFalse(locker.isFree(serviceLock, lockName));

        boolean gotException = false;
        try {
            locker.lockWithNumberOfTries(serviceLock, lockName, 1);
        } catch (final LockFailedException e) {
            gotException = true;
        }
        Assert.assertTrue(gotException);

        lock.release();
        Assert.assertTrue(locker.isFree(serviceLock, lockName));
    }

    @Test(groups = "redis")
    public void testReentrantLock() throws IOException, LockFailedException {
        final String serviceLock = "MY_SHITTY_LOCK";
        final String lockName = UUID.randomUUID().toString();

        final String requestId = "12345";

        Request.setPerThreadRequestData(new RequestData(requestId));

        final GlobalLock lock = locker.lockWithNumberOfTries(serviceLock, lockName, 3);
        Assert.assertFalse(locker.isFree(serviceLock, lockName));

        // Re-aquire the createLock with the same requestId, should work
        final GlobalLock reentrantLock = locker.lockWithNumberOfTries(serviceLock, lockName, 1);

        lock.release();
        Assert.assertFalse(locker.isFree(serviceLock, lockName));

        reentrantLock.release();
        Assert.assertTrue(locker.isFree(serviceLock, lockName));
    }
}
