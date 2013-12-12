/*
 * Copyright (C) 2013 Synthetos LLC. All Rights reserved.
 * see license.txt for terms.
 */

package tgfx.tinyg;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.log4j.Logger;
/**
 * The <code>TinygDriverFactory</code> class implement a simple factory that only creates on TinygDriver object
 * @author pfarrell
 * Created on Dec 8, 2013 7:53:01 PM
 */
public class TinygDriverFactory {
    /** logger instance */
    private static final Logger aLog = Logger.getLogger(TinygDriverFactory.class);
    private static TinygDriver theDriver;
    private static ReadWriteLock lock = new ReentrantReadWriteLock();
   /**
    * finds the one instance of a TinygDriver and returns it. Creates it if needed.
    * @return the one instance of a TinygDriver
    */ 
    public static TinygDriver getTinygDriver() {
        lock.readLock().lock();
        TinygDriver rval = null;
        try {
            if (theDriver == null) {
                rval = new AbstractTinygDriver();
                theDriver = rval;
                aLog.info("creating a TinygDriver instance");
            }
        } finally {
            lock.readLock().unlock();
        }
        return rval;
    }
}
