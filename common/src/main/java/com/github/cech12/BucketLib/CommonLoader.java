package com.github.cech12.BucketLib;

import com.github.cech12.BucketLib.api.BucketLib;
import com.github.cech12.BucketLib.platform.Services;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A static class for all loaders which initializes everything which is used by all loaders.
 */
public class CommonLoader {

    /** Logger instance */
    public static final Logger LOG = LogManager.getLogger(BucketLib.MOD_NAME);

    /**
     * Initialize method that should be called by every loader mod in the constructor.
     */
    public static void init() {
        Services.CONFIG.init();
    }

    private CommonLoader() {}

}
